# API Gateway — 支持认证插件热部署的微服务网关

> 生产级微服务 API 网关，支持在不重启服务的情况下上传并激活 JWT 认证插件 JAR，实现认证逻辑的在线热替换。

---

## 目录

- [项目背景](#项目背景)
- [产品定位](#产品定位)
- [核心需求](#核心需求)
- [技术选型](#技术选型)
- [总体架构](#总体架构)
- [模块划分](#模块划分)
- [插件 SPI 契约](#插件-spi-契约)
- [JWT 认证插件](#jwt-认证插件)
- [插件热部署引擎](#插件热部署引擎)
- [集群一致热部署](#集群一致热部署)
- [管理面 API](#管理面-api)
- [数据模型](#数据模型)
- [网关生产能力](#网关生产能力)
- [可观测与告警](#可观测与告警)
- [安全基线](#安全基线)
- [部署架构](#部署架构)
- [测试策略](#测试策略)
- [分阶段交付计划](#分阶段交付计划)
- [风险与应对](#风险与应对)
- [仓库状态](#仓库状态)

---

## 项目背景

在微服务架构中，API 网关承担统一入口、路由转发、认证鉴权、限流熔断等职责。认证逻辑（如 JWT 校验）往往随业务演进频繁变更：算法升级、Claims 规则调整、JWKS 轮换策略变化等。

传统做法是将认证逻辑硬编码在网关主程序中，每次变更都需要**重新构建、发布、重启**，带来：

- 发布窗口长，影响在线服务
- 多环境（开发/预发/生产）同步成本高
- 认证逻辑与网关核心耦合，难以独立迭代

本项目目标是构建一套**可插拔认证引擎**的 API 网关：认证能力以独立 JAR 插件形式交付，支持**热部署**——上传新插件 JAR 后无需重启网关即可生效，并具备生产级所需的版本管理、回滚、集群同步与可观测能力。

---

## 产品定位

| 维度 | 定义 |
|------|------|
| **产品形态** | 统一 API 网关 + 可插拔认证引擎 |
| **首期能力** | JWT 校验（RS256/ES256）、路由转发、插件热部署、集群一致生效 |
| **构建工具** | Maven 多模块 |
| **部署目标** | Docker + Kubernetes，支持水平扩展、滚动升级、可观测 |
| **非首期范围** | WAF、全链路灰度、多租户计费（架构预留扩展点） |

---

## 核心需求

### 功能需求

1. **微服务网关**
   - 作为统一 API 入口，将请求路由转发至下游微服务
   - 支持动态路由配置（路径、Host、Header 等谓词）
   - 支持限流、熔断、超时与重试

2. **JWT 认证**
   - 支持 RS256、ES256 等非对称算法
   - 通过 JWKS 端点拉取公钥，支持密钥轮换（`kid` 匹配）
   - 校验标准 Claims：`exp`、`nbf`、`iss`、`aud`、`sub`
   - 将指定 Claims 转发为下游 Header（如 `X-User-Id`、`X-Roles`）
   - 支持路径白名单（如 `/health`、`/metrics` 跳过认证）

3. **认证插件热部署**
   - 通过管理 API 上传认证插件 JAR
   - **不重启网关**即可完成加载、激活与生效
   - 支持插件版本管理、回滚与卸载
   - 热替换过程中 in-flight 请求不中断

4. **集群一致生效**
   - 多实例部署时，一次上传全集群同步激活
   - 各节点上报加载状态，版本不一致时告警

### 非功能需求

| 类别 | 要求 |
|------|------|
| **可用性** | 网关核心与插件解耦；插件加载失败时保持旧版本运行 |
| **性能** | 热替换对 P99 延迟影响 < 10%（压测目标） |
| **安全** | 插件签名校验、管理面鉴权、审计日志、默认 fail-close |
| **可观测** | 指标、链路追踪、结构化日志、健康检查 |
| **可运维** | 回滚、Runbook、Helm 部署、优雅停机 |

---

## 技术选型

| 类别 | 选型 | 说明 |
|------|------|------|
| 语言 | Java 17 LTS | 长期支持版本 |
| 构建 | Maven 多模块 | 依赖版本统一管理 |
| 网关框架 | Spring Boot 3.2+ / Spring Cloud Gateway | 成熟 Filter 链，生态完善 |
| 注册/配置 | Nacos | 路由与运行时配置 |
| 插件隔离 | 独立 `PluginClassLoader` | 每插件独立类加载器，避免冲突 |
| JWT 库 | nimbus-jose-jwt | 支持 JWKS、RS256/ES256 |
| 插件存储 | MinIO（开发）/ OSS（生产） | 多实例共享 JAR 文件 |
| 集群同步 | Redis Pub/Sub | 单点上传，全集群 reload |
| 元数据存储 | MySQL / PostgreSQL | 插件版本、部署事件、节点状态 |
| 可观测 | Micrometer + Prometheus + OpenTelemetry | 指标、链路、日志 |
| 部署 | Docker + Helm Chart | 标准化上线 |

---

## 总体架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端 / 业务应用                            │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API Gateway 集群（N 实例）                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │  Instance 1  │  │  Instance 2  │  │  Instance N  │                   │
│  │              │  │              │  │              │                   │
│  │ GlobalFilter │  │ GlobalFilter │  │ GlobalFilter │                   │
│  │ AuthFilter   │  │ AuthFilter   │  │ AuthFilter   │                   │
│  │ PluginMgr    │  │ PluginMgr    │  │ PluginMgr    │                   │
│  │ JWT Plugin   │  │ JWT Plugin   │  │ JWT Plugin   │                   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                   │
└─────────┼─────────────────┼─────────────────┼───────────────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            ▼
                   ┌─────────────────┐
                   │   下游微服务      │
                   └─────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                              控制面（Control Plane）                      │
│                                                                         │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐ │
│  │  Admin API  │──▶│   Registry  │──▶│   Storage   │   │ Redis Sync  │ │
│  │  插件管理    │   │  MySQL/PG   │   │ MinIO/OSS   │   │  Pub/Sub    │ │
│  └─────────────┘   └─────────────┘   └─────────────┘   └──────┬──────┘ │
│                                                                │        │
│                                    广播 reload 事件 ────────────┘        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 数据面请求路径

```
Client
  → Gateway Global Filter（链路追踪、日志）
  → Auth Plugin Filter（调用当前 JWT 插件）
  → Route Filter（路由匹配）
  → 下游微服务
```

### 控制面插件变更路径

```
运维上传 JAR
  → 校验（格式、接口、签名、版本兼容）
  → 写入 Storage（对象存储）
  → 写入 Registry（元数据、配置）
  → Redis 广播 reload 事件
  → 各节点 PluginManager 热加载
  → 原子切换当前插件引用
  → 旧插件 destroy + 释放 ClassLoader
  → 节点上报 ACTIVE / FAILED
```

---

## 模块划分

```
api-gateway/
├── pom.xml                          # 父 POM，依赖版本统一管理
├── gateway-api/                     # 插件 SPI 契约（对外发布 artifact）
│   └── AuthPlugin, PluginContext, PluginMetadata, AuthResult
├── gateway-common/                  # 公共模型、异常、工具类
├── gateway-core/                    # 网关主服务（Spring Boot 可执行 JAR）
│   ├── filter/                      # AuthPluginFilter, TraceFilter, RateLimitFilter
│   ├── plugin/                      # PluginManager, PluginClassLoader, Validator
│   ├── routing/                     # 动态路由加载
│   ├── admin/                       # 插件管理 REST API
│   ├── sync/                        # 集群插件同步监听
│   └── config/                      # 安全配置、CORS、限流
├── plugin-jwt/                      # 官方 JWT 认证插件（独立打包 JAR）
│   └── JwtAuthPlugin
├── deploy/
│   ├── docker/                      # Dockerfile, docker-compose.yml
│   └── helm/                        # Helm Chart
└── docs/
    ├── plugin-dev-guide.md          # 插件开发指南
    ├── ops-runbook.md               # 运维手册
    └── architecture.md              # 架构详细说明
```

**模块原则：**

- `gateway-api` 作为独立 artifact 发布，插件仅依赖此包，与 core 解耦
- 认证插件（如 `plugin-jwt`）**不**打入 `gateway-core`，运行时从 Storage 动态加载
- 控制面 API 与数据面流量隔离（独立端口或 K8s Service）

---

## 插件 SPI 契约

插件通过 Java 接口与网关交互，网关通过 SPI（`META-INF/services`）或约定入口类发现插件实现。

```java
public interface AuthPlugin {

    /** 插件初始化，读取 JWT 配置（JWKS URL、issuer、audience 等） */
    void init(PluginContext context);

    /** 认证逻辑，必须无阻塞或可控超时 */
    AuthResult authenticate(AuthRequest request);

    /** 热卸载前调用，释放连接池、定时任务等资源 */
    void destroy();

    /** 元信息：name、version、author、minGatewayVersion */
    PluginMetadata metadata();

    /** 健康检查：JWKS 可达性、配置有效性 */
    HealthStatus healthCheck();
}
```

**契约约束：**

| 类型 | 字段 / 行为 |
|------|-------------|
| `AuthRequest` | headers、path、method、clientIp、traceId |
| `AuthResult` | 通过 / 拒绝 / 匿名 / 内部错误 |
| `PluginContext` | 提供 Logger、Config、Metrics，禁止访问 Gateway 内部类 |
| 插件 JAR | 不得携带 `gateway-core` 依赖；仅依赖 `gateway-api` |

---

## JWT 认证插件

### 校验能力

| 能力 | 说明 |
|------|------|
| 算法 | RS256、ES256（生产环境禁用 HS256 裸密钥） |
| JWKS | 从 URL 拉取并缓存，支持 key rotation（`kid` 匹配） |
| 标准 Claims | `exp`、`nbf`、`iss`、`aud`、`sub` |
| 自定义 Claims | 可配置白名单，转发为下游 Header |
| 路径规则 | 支持 exclude paths（`/health`、`/metrics` 等） |
| 失败策略 | 401 + 统一错误码；JWKS 不可用时默认 fail-close |

### 插件配置示例

```json
{
  "issuer": "https://auth.example.com",
  "audiences": ["api-gateway"],
  "jwksUri": "https://auth.example.com/.well-known/jwks.json",
  "jwksRefreshIntervalSec": 300,
  "headerName": "Authorization",
  "tokenPrefix": "Bearer ",
  "forwardClaims": ["sub", "roles", "tenant_id"],
  "clockSkewSec": 30,
  "excludePaths": ["/health", "/metrics", "/actuator/**"]
}
```

配置随插件版本存储在 Registry 中，热部署时可同时更新 JAR 与配置。

---

## 插件热部署引擎

### 生命周期状态机

```
UPLOADED → VALIDATED → LOADED → ACTIVE → DEPRECATING → UNLOADED
                              ↘ FAILED
                              ↘ ROLLED_BACK
```

| 阶段 | 动作 |
|------|------|
| **UPLOADED** | 接收 JAR，写入 Storage，记录 checksum |
| **VALIDATED** | 校验 manifest、接口实现、签名、网关版本兼容 |
| **LOADED** | 新建 ClassLoader 加载 JAR，调用 `init()` |
| **ACTIVE** | CAS 原子切换当前插件指针，新请求走新插件 |
| **DEPRECATING** | 旧插件 `destroy()`，等待 in-flight 请求结束（Grace Period 默认 30s） |
| **UNLOADED** | 释放 ClassLoader 引用，等待 GC |

### 并发与一致性

- `PluginManager` 使用 **ReadWriteLock**：读多写少，认证路径无阻塞
- 切换采用 **volatile + 双缓冲** 或 `AtomicReference<AuthPluginHolder>`
- Filter 执行：`holder = pluginManager.getCurrent()` → `holder.authenticate(request)`
- 旧插件在 Grace Period 内保留引用，防止 mid-request 被卸载

### 生产级校验

- JAR 大小限制（默认 10MB）
- SHA-256 checksum 校验
- 可选 GPG / 内部 CA 签名校验
- 禁止插件 JAR 携带 gateway-core 依赖（依赖冲突检测）
- 启动时校验 `minGatewayVersion` 兼容性

### 回滚

- Registry 保留最近 N 个版本（默认 5）
- `POST /admin/plugins/{name}/rollback?version=x.y.z`
- 回滚走同一套 LOAD → ACTIVE 流程，无需重启

---

## 集群一致热部署

单实例热部署无法满足生产多副本场景。本方案通过 **共享存储 + 事件广播** 实现一次发布、全集群生效。

```
Admin API 接收上传
    │
    ├─▶ JAR 写入共享 Storage（MinIO / OSS / PVC）
    ├─▶ 元数据写入 Registry（MySQL）
    └─▶ 发布事件到 Redis Channel: gateway:plugin:reload
              │
              ├─▶ Gateway Instance 1 → PluginManager.reload()
              ├─▶ Gateway Instance 2 → PluginManager.reload()
              └─▶ Gateway Instance N → PluginManager.reload()
                        │
                        └─▶ 各节点上报 ACTIVE / FAILED
```

**一致性策略：**

- 加载失败节点自动保持旧插件运行，并触发告警
- Phase 2 可选：Canary 单节点试加载，成功后再全量广播
- 通过 `plugin_node_status` 表监控各节点版本一致性

---

## 管理面 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/admin/plugins/upload` | `POST` multipart | 上传 JAR + metadata + config |
| `/admin/plugins` | `GET` | 插件列表（name、version、status、activeNodeCount） |
| `/admin/plugins/{name}` | `GET` | 插件详情与部署历史 |
| `/admin/plugins/{name}/activate` | `POST` | 激活指定版本 |
| `/admin/plugins/{name}/rollback` | `POST` | 回滚至历史版本 |
| `/admin/plugins/{name}/deactivate` | `POST` | 卸载插件 |
| `/admin/plugins/{name}/health` | `GET` | 当前插件健康状态 |

**管理面安全：**

- Admin API 独立端口或 K8s ClusterIP，不对外暴露
- OAuth2 Client Credentials 或 mTLS 鉴权
- RBAC：上传、激活、回滚权限分离
- 全操作审计日志（操作人、时间、checksum、结果）

---

## 数据模型

### `plugin_artifact` — 插件制品

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR | 插件名，如 `jwt-auth` |
| version | VARCHAR | semver，如 `1.2.0` |
| storage_path | VARCHAR | 对象存储路径 |
| checksum | VARCHAR | SHA-256 |
| config_json | TEXT | JWT 等运行时配置 |
| status | ENUM | UPLOADED / ACTIVE / DEPRECATED |
| created_by | VARCHAR | 上传人 |
| created_at | DATETIME | 创建时间 |

### `plugin_deploy_event` — 部署事件

| 字段 | 类型 | 说明 |
|------|------|------|
| event_id | BIGINT PK | 事件 ID |
| plugin_id | BIGINT FK | 关联插件 |
| action | ENUM | ACTIVATE / ROLLBACK / DEACTIVATE |
| operator | VARCHAR | 操作人 |
| result_summary | VARCHAR | 成功/失败节点数 |
| created_at | DATETIME | 事件时间 |

### `plugin_node_status` — 节点运行状态

| 字段 | 类型 | 说明 |
|------|------|------|
| node_id | VARCHAR PK | 网关节点实例 ID |
| plugin_id | BIGINT FK | 当前插件 |
| runtime_status | ENUM | ACTIVE / FAILED / LOADING |
| last_error | TEXT | 最近错误信息 |
| updated_at | DATETIME | 更新时间 |

---

## 网关生产能力

| 能力 | 实现要点 |
|------|----------|
| 动态路由 | Nacos / DB 加载，支持 path、host、header predicates |
| 限流 | Redis Rate Limiter 或 Gateway RequestRateLimiter |
| 熔断 | Resilience4j Circuit Breaker |
| 超时/重试 | 按路由可配置 |
| 链路追踪 | W3C traceparent 注入 |
| 指标 | QPS、P99、认证失败率、插件切换次数、JWKS 刷新失败 |
| 日志 | JSON 结构化，含 traceId、pluginVersion |
| 健康检查 | `/actuator/health`：JWKS、PluginManager、Redis、Registry |
| 优雅停机 | 摘流 → 等待 in-flight → destroy plugins |

---

## 可观测与告警

| 指标 | 说明 | 告警阈值（示例） |
|------|------|------------------|
| `gateway_auth_failure_rate` | 认证失败率 | > 5% 持续 5min |
| `gateway_plugin_reload_failure` | 插件加载失败 | 任意失败立即告警 |
| `gateway_jwks_fetch_error` | JWKS 拉取失败 | 连续 3 次 |
| `gateway_plugin_version_drift` | 节点版本不一致 | 任意不一致 |
| `gateway_request_p99_latency` | P99 延迟 | > 500ms |

日志审计范围：所有插件变更操作、认证拒绝（采样）、JWKS 刷新事件。

---

## 安全基线

- 数据面与管理面**网络隔离**（K8s NetworkPolicy）
- JWT 插件默认 **fail-close**（JWKS 不可用时拒绝请求）
- 插件 JAR **签名校验**（内部 CA）
- Secrets（DB、Redis、Admin Token）通过 K8s Secret / Vault 管理
- 禁止插件通过反射访问系统 ClassLoader
- Admin 上传路径防路径穿越、ZIP Slip 检查
- 生产环境禁用 HS256 裸密钥，强制 RS256/ES256

---

## 部署架构

### 开发环境（Docker Compose）

| 组件 | 说明 |
|------|------|
| gateway-core × 1~3 | 网关实例 |
| MySQL 8 | 插件 Registry |
| Redis 7 | 集群同步 |
| MinIO | 插件 JAR 存储 |
| mock-jwks-server | 模拟 Auth 服务 JWKS 端点 |

### 生产环境（Kubernetes）

| 组件 | 说明 |
|------|------|
| gateway Deployment | HPA，≥ 2 副本 |
| 共享 PVC 或 OSS | 插件 JAR 存储 |
| Admin API Service | ClusterIP，仅内网访问 |
| Ingress | 暴露数据面 443 |
| Prometheus + Grafana | 监控面板 |

---

## 测试策略

| 类型 | 覆盖范围 |
|------|----------|
| 单元测试 | PluginManager 加载/卸载/并发切换 |
| 集成测试 | 上传 JWT 插件 → 带 Token 请求 → 验证 200/401 |
| 热替换测试 | v1 → v2 无重启，in-flight 请求不中断 |
| 回滚测试 | v2 故障 → 回滚 v1，服务恢复 |
| 集群测试 | 3 节点同时收到 reload 事件，版本一致 |
| 压测 | 10k QPS 下热替换，P99 波动 < 10% |
| 混沌测试 | JWKS 不可用、Storage 短暂不可读 |

---

## 分阶段交付计划

### Phase 1 — 核心可运行（预计 2~3 周）

- [ ] Maven 多模块骨架 + CI（GitHub Actions: build / test / package）
- [ ] `gateway-api` SPI 接口定义
- [ ] `gateway-core` 基础路由 + AuthPluginFilter
- [ ] PluginManager 热加载 / 卸载 / 原子切换
- [ ] `plugin-jwt` v1.0（RS256 + JWKS + Claims 转发）
- [ ] Admin 上传 / 激活 / 查询 API（单节点）
- [ ] Docker Compose 本地联调环境

### Phase 2 — 集群与运维（预计 1~2 周）

- [ ] Plugin Registry 持久化（MySQL）
- [ ] Redis 集群同步 reload
- [ ] 节点状态上报 + 一键回滚
- [ ] Prometheus 指标 + Grafana 面板
- [ ] Helm Chart + 健康/就绪探针

### Phase 3 — 生产加固（预计 1 周）

- [ ] 插件 GPG / 内部 CA 签名校验
- [ ] Admin RBAC + 审计日志
- [ ] Canary 发布（可选）
- [ ] 压测报告 + 运维 Runbook

---

## 风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| ClassLoader 泄漏 | Metaspace OOM | destroy 钩子 + 弱引用 + Metaspace 监控 |
| 插件恶意代码 | 安全风险 | 签名校验 + 沙箱（禁止 Runtime.exec 等） |
| 集群部分节点加载失败 | 版本不一致 | 告警 + 自动重试 + 一键回滚 |
| JWT 密钥轮换窗口 | 短暂认证失败 | JWKS 缓存 + kid 匹配 + clock skew 容差 |
| 热替换瞬间延迟抖动 | 用户体验 | 双缓冲 + 读锁无阻塞切换 |

---

## 仓库状态

| 项 | 状态 |
|----|------|
| 需求文档 | ✅ 本文档 |
| 架构设计 | ✅ 本文档 |
| 代码实现 | ⏳ 待开发（回复「开始编码」后启动 Phase 1） |
| CI/CD | ⏳ 待开发 |
| 部署配置 | ⏳ 待开发 |

---

## License

待定
