# Stage 教学日志

> 本文件既是**变更记录**，也是**教学文档**。  
> 每个 Stage 从零开始多推进一步：讲清楚「为什么做」「学什么」「怎么验」「改了什么」。  
> 建议按 Stage 顺序阅读，并亲手跑一遍「动手实验」。

---

## 如何使用本文档

| 阅读方式 | 适合谁 |
|----------|--------|
| 只看「本课小结」 | 快速回顾 |
| 按「核心概念 → 动手实验」顺序 | 第一次学习 |
| 查「变更记录」表格 | 代码 Review / 上线对账 |

**配套材料：**

- 总体规划：[README.md](./README.md)
- 源代码仓库：https://github.com/code-corey/api-gateway

---

## 课程路线图

```
Stage 0  工程骨架          ← 会搭 Maven 项目、会看健康检查
   ↓
Stage 1  最小网关          ← 会配路由、理解请求转发
   ↓
Stage 2  硬编码认证        ← 会写 GlobalFilter（待学）
   ↓
  ...
   ↓
Stage 12 生产级网关        ← 插件热部署 + 集群（待学）
```

### 进度索引

| Stage | 工程名 | 你将要掌握 | 提交 | 状态 |
|-------|--------|------------|------|------|
| 0 | `api-gateway-bootstrap` | Maven 多模块 + Spring Boot 启动 | `9fc87b3` | ✅ |
| 1 | `minimal-gateway` | Spring Cloud Gateway 静态路由 | `f49634c` | ✅ |
| 2 | `gateway-with-hardcoded-auth` | GlobalFilter 硬编码认证 | `9a550e8` | ✅ |
| 3 | `gateway-with-static-jwt` | RS256 本地公钥 JWT | `863a027` | ✅ |
| 4 | `gateway-with-jwks` | JWKS 远端拉取 + kid 匹配 | `7ae0e87` | ✅ |
| 5 | `gateway-plugin-api` | AuthPlugin SPI 接口契约 | `16adc84` | ✅ |
| 6 | `gateway-jwt-plugin-jar` | JWT 独立 JAR + 静态加载 | `5b57fd3` | ✅ |
| 7 | `gateway-plugin-manager-cold` | PluginClassLoader + 冷加载 Manager | 待提交 | ✅ |
| 8 ~ 12 | … | 见 README | — | ⏳ |

---

# Stage 0 — 工程骨架

**工程名：** `api-gateway-bootstrap`  
**提交：** `9fc87b3` · 2026-07-06  
**一句话：** 先让项目能编译、能启动、能报健康状态——什么都不做，但地基要稳。

---

## 学习目标

学完本 Stage，你应该能够：

1. 说出 Maven 父 POM 与子模块的关系
2. 创建一个最小 Spring Boot 应用并打包成 JAR
3. 使用 Actuator 的 `/actuator/health` 判断服务是否存活
4. 理解「先骨架、后业务」的分阶段开发思路

---

## 为什么要做这个 Stage

很多项目一上来就写网关路由、JWT、插件——结果环境问题、依赖冲突、启动失败，很难判断是**基础设施**错了还是**业务逻辑**错了。

Stage 0 故意只做三件事：

- 项目结构对
- 依赖版本对
- 能启动、能探活

这样 Stage 1 开始加网关时，如果出问题，你可以确定：**不是骨架的问题**。

---

## 核心概念

### 1. Maven 多模块

```
api-gateway/          ← 父工程（packaging=pom），管版本、管模块
└── gateway-core/     ← 子模块，真正可运行的应用
```

- **父 POM**：统一 Java 版本、Spring Boot 版本，避免子模块各写各的
- **子模块**：以后还会加 `gateway-api`、`plugin-jwt`、`mock-backend` 等

### 2. Spring Boot 启动器（Starter）

| 依赖 | 作用 |
|------|------|
| `spring-boot-starter-web` | 内嵌 Tomcat，提供 HTTP 能力（Stage 0 使用；Stage 1 会换掉） |
| `spring-boot-starter-actuator` | 暴露运维端点，如健康检查 |
| `spring-boot-starter-test` | 测试支持，仅 test 范围 |

### 3. Actuator 健康检查

生产环境里，K8s、负载均衡器需要知道你的进程是否还活着。  
`/actuator/health` 返回 `{"status":"UP"}` 就表示：**应用上下文已启动，核心组件正常**。

---

## 实现步骤（我们做了什么）

| 步骤 | 动作 |
|------|------|
| 1 | 创建父工程 `pom.xml`，`groupId=com.codecore`，Java 17 |
| 2 | 创建子模块 `gateway-core` |
| 3 | 写入口类 `GatewayApplication`（一个 `@SpringBootApplication` 即可） |
| 4 | 配置 `application.yml`：端口 8080，开放 health/info 端点 |
| 5 | 写 `GatewayApplicationTests`：验证 Spring 容器能加载 |
| 6 | 添加 `.gitignore`，更新 README 分阶段计划 |

---

## 代码解读

### 入口类

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- `@SpringBootApplication` = 配置类 + 组件扫描 + 自动配置
- 没有 Controller、没有路由——**故意的**，Stage 0 只验证「能活」

### 配置文件

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- `server.port`：监听端口
- `management.endpoints.web.exposure.include`：哪些 Actuator 端点对 HTTP 可见

---

## 动手实验

```bash
cd E:\MyGithub\api-gateway

# 1. 编译 + 测试
mvn clean package

# 2. 启动
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 3. 健康检查（浏览器或 curl）
curl http://localhost:8080/actuator/health
```

**期望输出：**

```json
{"status":"UP"}
```

### 思考题

1. 如果把 `server.port` 改成 9090，健康检查的 URL 要改成什么？
2. `GatewayApplicationTests` 没有写任何 `assert`，为什么也能发现配置错误？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | BUILD SUCCESS |
| 2 | 应用启动 | 日志出现 `Started GatewayApplication` |
| 3 | 健康检查 | `/actuator/health` → `UP` |

---

## 变更记录

| 类型 | 路径 |
|------|------|
| 新增 | `pom.xml`、`.gitignore`、`gateway-core/` 全部源码 |
| 修改 | `README.md`（分阶段计划） |

**Stage 0 时项目结构：**

```
api-gateway/
├── pom.xml
├── .gitignore
├── README.md
└── gateway-core/
    ├── pom.xml
    └── src/main/.../GatewayApplication.java
    └── src/main/resources/application.yml
    └── src/test/.../GatewayApplicationTests.java
```

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| 分阶段开发 | 先保证能启动，再加业务 |
| 父 POM | 管版本、管模块，自己不打包运行 |
| Actuator | 健康检查是运维的「心跳」 |
| Stage 0 不做 | 路由、认证、插件——都是后面的课 |

**下一课预告（Stage 1）：** 把「空壳应用」变成真正能**转发请求**的网关。

---

# Stage 1 — 最小网关

**工程名：** `minimal-gateway`  
**提交：** `f49634c` · 2026-07-06  
**一句话：** 客户端只访问网关，网关把请求转到下游——这是 API 网关最本质的能力。

---

## 学习目标

学完本 Stage，你应该能够：

1. 解释 API 网关在整个微服务架构中的位置
2. 配置 Spring Cloud Gateway 的一条静态路由
3. 理解 `Predicate`（断言）和 `Filter`（过滤器）的作用
4. 读懂 `StripPrefix` 对 URL 路径的影响
5. 用 `mock-backend` 在本地完成端到端转发验证

---

## 为什么要做这个 Stage

真实环境里，客户端不应该直连每个微服务：

```
❌ 客户端 → 用户服务:8081
           → 订单服务:8082
           → 商品服务:8083   （地址多、难管理、无法统一鉴权）

✅ 客户端 → API 网关:8080 → 各微服务   （统一入口）
```

Stage 1 用**一条最简单的路由**把这个模型跑通，后面加的认证、限流、插件，都是挂在这条链路上的。

---

## 核心概念

### 1. Spring Cloud Gateway 是什么

基于 **Spring WebFlux + Netty** 的响应式网关，核心三件事：

| 概念 | 含义 | 本 Stage 示例 |
|------|------|---------------|
| **Route（路由）** | 一条转发规则 | `api-route` |
| **Predicate（断言）** | 匹配条件，满足才走路由 | `Path=/api/**` |
| **Filter（过滤器）** | 匹配后对请求/响应做处理 | `StripPrefix=1` |

### 2. 为什么不能同时用 `starter-web` 和 Gateway

| | `starter-web` | `spring-cloud-starter-gateway` |
|--|---------------|-------------------------------|
| 模型 | Servlet（Tomcat） | Reactive（Netty） |
| 场景 | 普通 REST 应用 | API 网关 |

两者冲突，所以 Stage 1 **移除了 `starter-web`**，换成 Gateway。

> `mock-backend` 仍用 `starter-web`——它是普通下游服务，不是网关。

### 3. StripPrefix 图解

```
客户端请求:  GET /api/hello
                │
    Gateway 匹配 /api/**
                │
    StripPrefix=1  → 去掉第 1 段路径 "api"
                │
转发到下游:  GET /hello   →  http://localhost:8081/hello
```

如果不加 `StripPrefix`，下游收到的是 `/api/hello`，而 `HelloController` 只映射了 `/hello`，会 404。

### 4. 请求全链路

```
┌──────────┐     GET /api/hello      ┌─────────────────┐
│  客户端   │ ──────────────────────▶ │ Gateway :8080   │
└──────────┘                         │  api-route      │
                                     └────────┬────────┘
                                              │ GET /hello
                                              ▼
                                     ┌─────────────────┐
                                     │ mock-backend    │
                                     │ :8081           │
                                     └────────┬────────┘
                                              │
                                              ▼
                              {"message":"hello from mock backend"}
```

---

## 实现步骤（我们做了什么）

| 步骤 | 动作 |
|------|------|
| 1 | 父 POM 引入 Spring Cloud BOM `2023.0.4` |
| 2 | `gateway-core` 依赖改为 `spring-cloud-starter-gateway` |
| 3 | `application.yml` 增加路由：`/api/**` → `localhost:8081` |
| 4 | 新建 `mock-backend` 模块，端口 8081，提供 `GET /hello` |
| 5 | 联调验证转发；更新 README 与 CHANGELOG |

---

## 代码解读

### 路由配置（核心）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: api-route              # 路由唯一标识，方便日志和排查
          uri: http://localhost:8081 # 下游地址
          predicates:
            - Path=/api/**           # 只转发 /api 开头的请求
          filters:
            - StripPrefix=1          # 去掉路径第一段 /api
```

### 模拟下游

```java
@RestController
public class HelloController {
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "hello from mock backend");
    }
}
```

这是一个**最简下游**：没有数据库、没有业务逻辑，专门用来证明「网关确实把请求转过去了」。

---

## 动手实验

### 实验 1：端到端转发

```bash
# 终端 1 — 下游（必须先启动）
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar

# 终端 2 — 网关
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 终端 3 — 验证
curl http://localhost:8081/hello          # 直连下游
curl http://localhost:8080/api/hello      # 经网关转发
```

两次应返回相同 JSON：

```json
{"message":"hello from mock backend"}
```

### 实验 2：理解 StripPrefix

尝试访问 `http://localhost:8080/api/hello`，观察网关日志中的转发 URL。

再**临时注释掉** `StripPrefix=1`，重启网关，再次请求——应该会 404。  
想想为什么？（提示：下游映射的是 `/hello` 不是 `/api/hello`）

### 实验 3：路由不匹配

访问 `http://localhost:8080/other/hello`——没有匹配的路由，网关会返回什么？

### 思考题

1. 如果下游改成 `GET /api/hello`，`StripPrefix` 还要不要？怎么配？
2. 生产环境 `uri` 能写死 `localhost` 吗？Stage 11 会怎么解决？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | 两个模块都编译成功 |
| 2 | 下游启动 | 8081 可访问 `/hello` |
| 3 | 网关启动 | 8080 健康检查 UP |
| 4 | 转发 | `8080/api/hello` 返回下游 JSON |
| 5 | 网关日志 | 能看到路由 `api-route` 被匹配 |

---

## 变更记录

### 新增文件

| 路径 | 说明 |
|------|------|
| `mock-backend/pom.xml` | 模拟下游模块 |
| `mock-backend/.../MockBackendApplication.java` | 下游入口 |
| `mock-backend/.../HelloController.java` | `/hello` 接口 |
| `mock-backend/.../application.yml` | 端口 8081 |

### 修改文件

| 路径 | 说明 |
|------|------|
| `pom.xml` | +`mock-backend` 模块，+Spring Cloud BOM |
| `gateway-core/pom.xml` | web → gateway |
| `gateway-core/.../application.yml` | +路由配置 |
| `README.md` | Stage 1 标记完成 |

### Stage 0 → Stage 1 对比

| 维度 | Stage 0 | Stage 1 |
|------|---------|---------|
| 模块 | 1 | 2（+mock-backend） |
| Web 栈 | Servlet | Reactive（Gateway） |
| 业务能力 | 仅探活 | 请求转发 |
| Spring Cloud | 无 | 2023.0.4 |

---

## 常见问题

**Q：网关启动报错 `Port 8080 was already in use`**  
A：上次进程没关。Windows 下 `Get-Process java | Stop-Process -Force`，或换端口。

**Q：`mvn clean` 失败，删不掉 jar**  
A：jar 正在被运行。先停掉 Java 进程再编译。

**Q：网关 404，但下游直连正常**  
A：检查 `StripPrefix` 与下游 Controller 路径是否对齐。

**Q：Gateway 和 Zuul 有什么区别？**  
A：Zuul 1.x 基于 Servlet，已偏旧；Spring Cloud Gateway 基于 WebFlux，是 Spring 官方当前推荐的网关方案。

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| 网关本质 | 统一入口 + 按规则转发 |
| 路由三要素 | Route + Predicate + Filter |
| StripPrefix | 网关对外路径 ≠ 下游内部路径时常用 |
| mock-backend | 教学用假下游，让你不依赖外部服务也能验 |
| Stage 1 不做 | 认证、JWT、插件——Stage 2 开始 |

**下一课预告（Stage 2）：** 在转发之前加一道关——没有 Token 的请求直接 401。

---

# Stage 2 — 硬编码认证

**工程名：** `gateway-with-hardcoded-auth`  
**提交：** `9a550e8` · 2026-07-06  
**一句话：** 在路由转发之前拦截请求——没带 Token 的，一律 401。

---

## 学习目标

学完本 Stage，你应该能够：

1. 理解 Spring Cloud Gateway 的 **GlobalFilter** 与过滤器链执行顺序
2. 实现一个检查 `Authorization: Bearer <token>` 的认证过滤器
3. 配置路径白名单（如 `/actuator/**` 免认证）
4. 用 `WebTestClient` 编写网关认证相关的集成测试

---

## 为什么要做这个 Stage

Stage 1 的网关谁都能访问——这在生产里不可接受。  
但直接上 JWT、插件热部署太复杂，所以 Stage 2 先用**固定 Token** 把「认证挂在 Filter 链上」这件事练熟：

```
请求 → 认证 Filter → 路由转发 → 下游
         ↑
      本 Stage 只加这一层
```

后面 Stage 3 把「固定 Token」换成 JWT，Stage 5 以后再把认证逻辑抽成插件——**Filter 的位置不变**。

---

## 核心概念

### 1. GlobalFilter vs GatewayFilter

| 类型 | 作用范围 | 本 Stage |
|------|----------|----------|
| **GlobalFilter** | 对所有路由生效 | ✅ 使用 |
| **GatewayFilter** | 仅绑定某条路由 | 未使用 |

认证通常用 GlobalFilter——所有业务 API 都要验，而不是只保护某一条路。

### 2. 过滤器链与 Order

```
请求进入
  → GlobalFilter (order 越小越先执行)
  → 路由匹配
  → 路由级 Filter
  → 转发下游
```

本实现 `getOrder() = HIGHEST_PRECEDENCE`，保证**先认证、后路由**。

### 3. Bearer Token 格式

```
Authorization: Bearer test-token-123
               ────── ──────────────
               固定前缀   Token 值（本 Stage 写死在配置里）
```

### 4. 请求全链路（Stage 2）

```
客户端  GET /api/hello
  │     Authorization: Bearer test-token-123
  ▼
HardcodedAuthGlobalFilter
  │  白名单？→ 跳过
  │  Token 合法？→ 否：401 JSON
  ▼ 是
路由 api-route → mock-backend
```

---

## 实现步骤（我们做了什么）

| 步骤 | 动作 |
|------|------|
| 1 | 新增 `GatewayAuthProperties`，绑定 `gateway.auth.*` 配置 |
| 2 | 实现 `HardcodedAuthGlobalFilter`（GlobalFilter + Ordered） |
| 3 | `application.yml` 配置 token、白名单路径 |
| 4 | 新增 `GatewayAuthWebTests` 集成测试 |
| 5 | 更新 README / 本教学日志 |

---

## 代码解读

### 配置项

```yaml
gateway:
  auth:
    enabled: true
    token: test-token-123
    exclude-paths:
      - /actuator/**
```

| 配置 | 含义 |
|------|------|
| `enabled` | 总开关，方便本地调试时关闭认证 |
| `token` | 允许的 Bearer Token 值（教学用硬编码） |
| `exclude-paths` | Ant 风格路径白名单 |

### 过滤器核心逻辑

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!authProperties.isEnabled()) {
        return chain.filter(exchange);
    }
    if (isExcluded(path)) {
        return chain.filter(exchange);
    }
    if (!isValidToken(authorization)) {
        return unauthorized(exchange);  // 401 + JSON
    }
    return chain.filter(exchange);
}
```

### 401 响应体

```json
{"code":401,"message":"未认证，请提供有效的 Bearer Token"}
```

统一 JSON 格式，方便前端和 API 调用方处理。

---

## 动手实验

```bash
# 先启动下游和网关（同 Stage 1）
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 实验 1：无 Token → 401
curl -i http://localhost:8080/api/hello

# 实验 2：错误 Token → 401
curl -i -H "Authorization: Bearer wrong" http://localhost:8080/api/hello

# 实验 3：正确 Token → 200 + 下游 JSON
curl -H "Authorization: Bearer test-token-123" http://localhost:8080/api/hello

# 实验 4：健康检查免认证
curl http://localhost:8080/actuator/health
```

### 思考题

1. 如果把 `gateway.auth.enabled` 设为 `false`，行为会怎样？
2. 为什么 Actuator 必须放白名单？不放会怎样？
3. 硬编码 Token 在生产环境有什么问题？（提示：Stage 3 JWT）

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | 全部测试通过 |
| 2 | 无 Token 访问 `/api/hello` | HTTP 401 |
| 3 | 错误 Token | HTTP 401 |
| 4 | 正确 Token | 返回下游 JSON |
| 5 | `/actuator/health` 无 Token | HTTP 200 |

---

## 变更记录

### 新增文件

| 路径 | 说明 |
|------|------|
| `gateway-core/.../auth/GatewayAuthProperties.java` | 认证配置绑定 |
| `gateway-core/.../auth/GatewayAuthConfiguration.java` | 启用配置属性 |
| `gateway-core/.../auth/HardcodedAuthGlobalFilter.java` | 认证全局过滤器 |
| `gateway-core/.../auth/GatewayAuthWebTests.java` | 认证集成测试 |

### 修改文件

| 路径 | 说明 |
|------|------|
| `gateway-core/.../application.yml` | +`gateway.auth` 配置块 |
| `gateway-core/.../GatewayApplication.java` | 更新 Stage 注释 |
| `README.md` | Stage 2 标记完成 |

### Stage 1 → Stage 2 对比

| 维度 | Stage 1 | Stage 2 |
|------|---------|---------|
| 认证 | 无 | Bearer Token 硬编码 |
| 过滤器 | 仅路由内置 Filter | +GlobalFilter |
| `/api/hello` 无 Token | 200（能转发） | 401 |
| 测试 | 上下文加载 | +WebTestClient 认证测试 |

---

## 常见问题

**Q：带了正确 Token 却返回 502/500**  
A：认证已通过，是下游 `mock-backend` 没启动。401 才是认证问题。

**Q：Token 配置了但还是 401**  
A：检查 Header 格式必须是 `Bearer ` + 空格 + token，大小写敏感。

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| GlobalFilter | 全局生效，适合做认证 |
| 执行顺序 | 认证要在路由转发之前 |
| 白名单 | 健康检查等路径不能误拦 |
| 硬编码 Token | 仅教学用，生产要用 JWT / 插件 |
| Stage 2 不做 | JWT 解析、插件化——Stage 3 开始 |

**下一课预告（Stage 3）：** 把固定 Token 换成真正的 JWT 验签。

---

# Stage 3 — JWT 本地公钥验签

**工程名：** `gateway-with-static-jwt`  
**提交：** `863a027` · 2026-07-06  
**一句话：** 用 RSA 公钥验证 JWT 签名，并校验 iss、aud、exp——这才是真实认证的样子。

---

## 学习目标

学完本 Stage，你应该能够：

1. 理解 JWT 结构：Header.Payload.Signature
2. 使用 `nimbus-jose-jwt` 做 RS256 验签
3. 从本地 PEM 文件加载公钥
4. 校验 `iss`、`aud`、`exp` 标准 Claims
5. 区分「认证失败 401」与「下游不可用 502」

---

## 为什么要做这个 Stage

Stage 2 的固定 Token 无法表达用户身份、过期时间，也无法防止伪造。  
JWT 是业界标准：Auth 服务用**私钥签名**，网关用**公钥验签**，无需共享密钥明文。

Stage 3 故意只用**本地公钥文件**（还不拉 JWKS），先把验签逻辑练熟；Stage 4 再改成从远端拉公钥。

---

## 核心概念

### 1. JWT 三段式

```
eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ...}.SflKxwRJ...
──────── Header ──────── ── Payload ── ─ Signature ─
```

| 段 | 内容 |
|----|------|
| Header | 算法（如 RS256）、类型 JWT |
| Payload | iss、aud、sub、exp 等 Claims |
| Signature | 用私钥对前两段签名，网关用公钥验证 |

### 2. RS256 验签流程

```
1. 解析 JWT 字符串
2. 用 RSA 公钥验证 Signature
3. 检查 iss 是否匹配配置
4. 检查 aud 是否包含 api-gateway
5. 检查 exp 是否未过期（含 clock skew）
```

### 3. 密钥分工

| 文件 | 位置 | 用途 |
|------|------|------|
| `dev-public.pem` | `src/main/resources/jwt/` | 网关验签（可提交 Git） |
| `dev-private.pem` | `src/test/resources/jwt/` | 仅测试/本地签发 Token |

**私钥绝不打进生产网关 JAR。**

### 4. 请求全链路（Stage 3）

```
Authorization: Bearer <JWT>
        │
        ▼
JwtAuthGlobalFilter
        │
        ▼
LocalJwtValidator（公钥验签 + Claims）
        │ 失败 → 401
        ▼ 通过
路由转发 → mock-backend
```

---

## 实现步骤（我们做了什么）

| 步骤 | 动作 |
|------|------|
| 1 | 引入 `nimbus-jose-jwt` |
| 2 | 生成 dev RSA 密钥对（公钥 main、私钥 test） |
| 3 | `PublicKeyLoader` 启动时加载 PEM 公钥 |
| 4 | `LocalJwtValidator` 验签 + iss/aud/exp |
| 5 | `JwtAuthGlobalFilter` 替换 Stage 2 硬编码过滤器 |
| 6 | `JwtTestTokenHelper` / `JwtDevTokenPrinter` 测试签发 |
| 7 | 集成测试覆盖过期、错误 iss、合法 JWT |

---

## 代码解读

### 配置

```yaml
gateway:
  auth:
    jwt:
      issuer: https://auth.example.com
      audience: api-gateway
      public-key-location: classpath:jwt/dev-public.pem
      clock-skew-seconds: 30
```

### 验签核心

```java
SignedJWT signedJwt = SignedJWT.parse(rawJwt);
JWSVerifier verifier = new RSASSAVerifier(publicKeyLoader.getPublicKey());
if (!signedJwt.verify(verifier)) {
    return failure("JWT 签名无效");
}
// 再校验 iss / aud / exp ...
```

---

## 动手实验

```bash
# 1. 编译
mvn clean package

# 2. 生成测试 JWT（在 IDE 运行 JwtDevTokenPrinter.main，或记下测试输出的 token）

# 3. 启动下游与网关
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 4. 无 Token → 401
curl -i http://localhost:8080/api/hello

# 5. 合法 JWT → 200（将 <JWT> 替换为 JwtDevTokenPrinter 输出）
curl -H "Authorization: Bearer <JWT>" http://localhost:8080/api/hello
```

### 思考题

1. 为什么伪造的 JWT（改 Payload 不重签）会验签失败？
2. 公钥泄露了，攻击者能伪造 Token 吗？（提示：需要私钥才能签名）
3. Stage 4 为什么需要 JWKS？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | 全部测试通过 |
| 2 | 无 Token | 401 |
| 3 | 过期 JWT | 401，message 含「已过期」 |
| 4 | 错误 iss | 401 |
| 5 | 合法 JWT + 下游已启动 | 200 + 下游 JSON |

---

## 变更记录

### 新增

| 路径 | 说明 |
|------|------|
| `auth/GatewayJwtProperties.java` | JWT 配置 |
| `auth/PublicKeyLoader.java` | PEM 公钥加载 |
| `auth/LocalJwtValidator.java` | 验签器 |
| `auth/JwtAuthGlobalFilter.java` | JWT 全局过滤器 |
| `auth/JwtValidationResult.java` | 校验结果 |
| `auth/AuthResponseWriter.java` | 401 响应工具 |
| `resources/jwt/dev-public.pem` | 开发公钥 |
| `test/resources/jwt/dev-private.pem` | 测试私钥 |
| `test/.../JwtTestTokenHelper.java` | 测试签发 |
| `test/.../JwtDevTokenPrinter.java` | 本地打印 JWT |
| `test/.../LocalJwtValidatorTests.java` | 验签单元测试 |

### 删除

| 路径 | 说明 |
|------|------|
| `auth/HardcodedAuthGlobalFilter.java` | Stage 2 硬编码过滤器 |

### Stage 2 → Stage 3 对比

| 维度 | Stage 2 | Stage 3 |
|------|---------|---------|
| 认证方式 | 固定字符串 | RS256 JWT |
| 配置项 | `token` | `jwt.issuer/audience/public-key` |
| 过滤器 | HardcodedAuthGlobalFilter | JwtAuthGlobalFilter |
| 可表达过期 | 否 | 是（exp） |

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| JWT | Header.Payload.Signature，公钥验签 |
| RS256 | 私钥签、公钥验，适合网关场景 |
| Claims | iss/aud/exp 必须校验 |
| 私钥位置 | 只在 Auth 服务与测试资源，不在网关 |
| Stage 3 不做 | JWKS 远端拉钥——Stage 4 |

**下一课预告（Stage 4）：** 公钥不再放文件，从 Auth 服务 JWKS 端点动态获取。

---

# Stage 4 — JWKS 远端拉取公钥

**工程名：** `gateway-with-jwks`  
**提交：** `7ae0e87` · 2026-07-09（多钥轮换：`5643fb6`）  
**一句话：** 公钥从 Auth 服务的 JWKS 端点动态拉取，支持 kid 匹配与定时刷新。

---

## 学习目标

1. 理解 JWKS 是什么，以及为何生产环境不用本地 PEM
2. 实现 JWKS 拉取、缓存与按 `kid` 选钥
3. 理解密钥轮换（Key Rotation）与定时刷新
4. 搭建 `mock-jwks-server` 模拟 Auth 服务

---

## 核心变更

| 组件 | 作用 |
|------|------|
| `JwksKeyProvider` | WebClient 拉取 JWKS，按 kid 返回公钥 |
| `JwksRefreshScheduler` | 定时刷新 JWKS 缓存 |
| `mock-jwks-server` | :8082 提供 `/.well-known/jwks.json` |
| 移除 `PublicKeyLoader` | 不再使用本地 PEM 验签 |

**配置：**

```yaml
gateway:
  auth:
    jwt:
      jwks-uri: http://localhost:8082/.well-known/jwks.json
      jwks-refresh-interval-seconds: 300
```

---

## 动手实验

### 实验 1：三进程联调（基础 JWKS 验签）

```bash
cd E:\MyGithub\api-gateway
mvn clean package

# 终端 1 — Mock Auth（JWKS）
java -jar mock-jwks-server/target/mock-jwks-server-0.0.1-SNAPSHOT.jar

# 终端 2 — 模拟下游
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar

# 终端 3 — 网关
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 终端 4 — 生成 JWT（IDE 运行 JwtDevTokenPrinter.main，或使用 mock-jwks issue-token）
curl -i http://localhost:8080/api/hello
curl -i -H "Authorization: Bearer <JWT>" http://localhost:8080/api/hello
curl http://localhost:8080/actuator/health
```

**期望结果：**

| 请求 | 期望 |
|------|------|
| 无 Token | HTTP 401 |
| 合法 JWT | HTTP 200 + 下游 JSON |
| `/actuator/health` 无 Token | HTTP 200 |

### 实验 2：密钥轮换（多钥并存，贴近生产）

```bash
# 1. 用 JwtDevTokenPrinter 或 issue-token 拿到 kid=dev-key-1 的 JWT（OLD_TOKEN）
curl -X POST http://localhost:8082/admin/issue-token

# 2. 查看 JWKS 中的 kid 列表
curl http://localhost:8082/admin/keys

# 3. 轮换：追加新钥，旧钥 dev-key-1 仍保留
curl -X POST http://localhost:8082/admin/rotate-key

# 4. 旧 Token 仍应通过（kid=dev-key-1 的公钥仍在 JWKS 中）
curl http://localhost:8080/api/hello -H "Authorization: Bearer <OLD_TOKEN>"

# 5. 用新钥签发 Token
curl -X POST http://localhost:8082/admin/issue-token

# 6. 重叠窗口结束：撤销旧钥
curl -X DELETE http://localhost:8082/admin/keys/dev-key-1
# → 此后 OLD_TOKEN 将因 JWKS 中无 dev-key-1 而验签失败
```

> **要点：** 网关按 JWT Header 的 `kid` 在 JWKS **集合**中选公钥，不是使用「最新一把钥」。轮换时 Auth 应 **追加** 新公钥，旧 Token 在 `exp` 内仍可验签。

### 思考题

1. 为什么 kid miss 时会触发 refresh？高并发下有什么风险？
2. 如果 Auth 服务短暂不可用，网关应 fail-open 还是 fail-close？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | BUILD SUCCESS |
| 2 | mock-jwks-server :8082 | `/admin/keys` 可访问 |
| 3 | 合法 JWT 经网关 | 200（下游已启动） |
| 4 | 轮换后旧 Token | 重叠窗口内仍 200 |
| 5 | 撤销旧 kid 后 | 旧 Token 401 |

---

## 变更记录

| 类型 | 路径 |
|------|------|
| 新增 | `mock-jwks-server/`、`JwksKeyProvider`、`JwksRefreshScheduler` |
| 修改 | `LocalJwtValidator`（JWKS 模式）、`application.yml` |
| 删除 | `PublicKeyLoader` |
| 改进 | `5643fb6` mock-jwks 多钥并存轮换模型 |

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| JWKS | Auth 公布「有效公钥集合」，轮换时可多钥并存 |
| kid | 按 Token Header 的 kid 选公钥，不是取「最新钥」 |
| 轮换 | 追加新钥 + 保留旧钥；旧 JWT 在 exp 内仍可验 |
| 撤销 | 重叠窗口后再从 JWKS 移除旧 kid |
| Stage 4 不做 | 插件化——Stage 5 开始 |

**下一课预告（Stage 5）：** 把认证逻辑抽成 `AuthPlugin` 接口。

---

# Stage 5 — 插件 SPI 接口层

**工程名：** `gateway-plugin-api`  
**提交：** `16adc84` · 2026-07-09  
**一句话：** 认证逻辑从 Filter 里抽出来，变成可插拔的 `AuthPlugin` 契约。

---

## 学习目标

1. 理解 SPI（Service Provider Interface）在插件化架构中的作用
2. 定义 `AuthPlugin` 及请求/响应模型
3. 网关 Filter 只负责调用插件，不再硬编码 JWT 逻辑
4. 为 Stage 6 独立 JAR 插件打包做准备

---

## 核心变更

### 新模块 `gateway-api`（纯契约，无 Spring 依赖）

| 类型 | 说明 |
|------|------|
| `AuthPlugin` | 插件 SPI：`init` / `authenticate` / `destroy` / `metadata` |
| `AuthRequest` | 路径、方法、IP、traceId、headers |
| `AuthResult` | 状态 + 消息 + 转发 Header |
| `PluginContext` | 配置与日志访问 |
| `PluginMetadata` | 名称、版本、作者 |

### gateway-core 重构

| 组件 | 作用 |
|------|------|
| `JwtAuthPlugin` | 内置 JWT 实现（暂留 core 内，Stage 6 外移） |
| `AuthPluginManager` | 持有当前插件，调用 `init()` |
| `AuthPluginGlobalFilter` | 替换 `JwtAuthGlobalFilter`，走 SPI |
| `SpringPluginContext` | `PluginContext` 的 Spring 实现 |

### 调用链（Stage 5）

```
请求 → AuthPluginGlobalFilter
         → AuthPluginManager.getCurrentPlugin()
         → JwtAuthPlugin.authenticate(AuthRequest)
         → LocalJwtValidator（JWKS 验签）
         → AuthResult → 401 或继续转发
```

---

## 动手实验

### 实验 1：验证 SPI 调用链与认证行为不变

```bash
cd E:\MyGithub\api-gateway
mvn clean package

# 终端 1~3：与 Stage 4 相同，启动 mock-jwks、mock-backend、gateway-core
java -jar mock-jwks-server/target/mock-jwks-server-0.0.1-SNAPSHOT.jar
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar

# 观察启动日志：应出现「JWT 认证插件已初始化: jwt-auth v1.0.0」
# 获取 JWT
curl -X POST http://localhost:8082/admin/issue-token

# 无 Token → 401
curl -i http://localhost:8080/api/hello

# 合法 JWT → 200
curl -i -H "Authorization: Bearer <JWT>" http://localhost:8080/api/hello
```

### 实验 2：对比 Stage 4 与 Stage 5 的代码结构

打开以下文件，理解「Filter 变薄、插件变厚」：

| 文件 | 职责 |
|------|------|
| `gateway-api/.../AuthPlugin.java` | SPI 契约（无 Spring） |
| `AuthPluginGlobalFilter.java` | 构造 AuthRequest、调插件 |
| `JwtAuthPlugin.java` | JWT 验签实现（Stage 5 仍在 core 内） |
| `AuthPluginManager.java` | 持有当前插件 |

### 思考题

1. 为什么 `gateway-api` 不能依赖 Spring？
2. Stage 6 为什么要把 `JwtAuthPlugin` 移出 core？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | 全部测试通过 |
| 2 | 启动日志 | 出现插件初始化日志 |
| 3 | 无 Token | HTTP 401 |
| 4 | 合法 JWT | HTTP 200 |
| 5 | `AuthPluginManagerTests` | 通过 |

---

## 变更记录

| 类型 | 路径 |
|------|------|
| 新增 | `gateway-api/` 模块、`JwtAuthPlugin`、`AuthPluginManager`、`AuthPluginGlobalFilter` |
| 删除 | `JwtAuthGlobalFilter` |
| 修改 | README、本教学日志 |

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| SPI | 网关与插件之间的「合同」 |
| gateway-api | 独立 artifact，插件只依赖它 |
| Filter 变薄 | 只构造 AuthRequest、调插件、处理结果 |
| Stage 5 不做 | 独立 JAR、热部署——Stage 6/8 |

**下一课预告（Stage 6）：** 把 `JwtAuthPlugin` 打成独立 JAR，启动时从 `plugins/` 加载。

---

# Stage 6 — JWT 插件独立 JAR

**工程名：** `gateway-jwt-plugin-jar`  
**提交：** `5b57fd3` · 2026-07-10  
**一句话：** JWT 认证从 gateway-core 剥离，打成独立 JAR，启动时通过 SPI 静态加载。

---

## 学习目标

1. 理解「插件 = 独立 artifact + SPI 注册」的打包模型
2. 使用 Java `ServiceLoader` 发现 `AuthPlugin` 实现
3. 使用 `URLClassLoader` 隔离插件与网关主程序
4. 理解 Maven 构建时如何把插件 JAR 复制到 `plugins/` 目录

---

## 为什么要做这个 Stage

Stage 5 的 `JwtAuthPlugin` 仍编译在 `gateway-core` 里——改认证逻辑仍要重新打网关 fat JAR。  
Stage 6 把 JWT 验签（含 JWKS 拉取）整体迁入 `plugin-jwt` 模块，网关只负责**加载和调用**，为 Stage 7~8 的热部署铺路。

```
Stage 5:  gateway-core.jar 内含 JwtAuthPlugin
Stage 6:  gateway-core.jar + plugins/plugin-jwt-*.jar
Stage 8:  运行中替换 plugins/ 下的 JAR，无需重启
```

---

## 核心变更

### 新模块 `plugin-jwt`（仅依赖 gateway-api + nimbus-jose-jwt）

| 类 | 作用 |
|----|------|
| `JwtAuthPlugin` | SPI 实现，插件入口 |
| `PluginJwtValidator` | RS256 验签 + iss/aud/exp |
| `PluginJwksKeyProvider` | JDK HttpClient 拉取 JWKS |
| `META-INF/services/...AuthPlugin` | Java SPI 注册文件 |

### gateway-core 重构

| 组件 | 作用 |
|------|------|
| `StaticPluginLoader` | 扫描 `plugins/`，URLClassLoader + ServiceLoader |
| `GatewayPluginProperties` | `gateway.plugins.directory` 配置 |
| `AuthPluginManager` | 启动时加载插件 JAR 并 init |
| 移除 | core 内 `JwtAuthPlugin`、`JwksKeyProvider`、`LocalJwtValidator`、`JwksRefreshScheduler` |

### 调用链（Stage 6）

```
启动 → StaticPluginLoader 加载 plugins/plugin-jwt-*.jar
     → ServiceLoader 发现 JwtAuthPlugin
     → plugin.init(SpringPluginContext)
请求 → AuthPluginGlobalFilter → plugin.authenticate()
```

**配置：**

```yaml
gateway:
  plugins:
    directory: plugins
  auth:
    jwt:
      jwks-uri: http://localhost:8082/.well-known/jwks.json
```

---

## 深入理解：`ServiceLoader` 与 Java SPI

Stage 6 的核心一行代码是：

```java
URLClassLoader pluginClassLoader = createPluginClassLoader(pluginJar);
ServiceLoader<AuthPlugin> serviceLoader = ServiceLoader.load(AuthPlugin.class, pluginClassLoader);
AuthPlugin plugin = serviceLoader.findFirst()
        .orElseThrow(() -> new IllegalStateException("插件 JAR 中未找到 AuthPlugin SPI 实现"));
```

下面按「是什么 → 怎么发现 → 怎么实例化 → 和 ClassLoader 的关系 → 本项目的完整链路」讲清楚。

### 1. SPI 是什么

**SPI（Service Provider Interface，服务提供者接口）** 是 JDK 内置的一种**插件发现机制**：

| 角色 | 在本项目里是谁 | 职责 |
|------|----------------|------|
| **SPI 接口** | `gateway-api` 里的 `AuthPlugin` | 定义「网关需要插件提供什么能力」 |
| **Provider（实现方）** | `plugin-jwt` 里的 `JwtAuthPlugin` | 实现接口，打包进独立 JAR |
| **Consumer（调用方）** | `gateway-core` 里的 `StaticPluginLoader` | 运行时找到实现类并调用 |

和 Spring `@Autowired` 的区别：

| | Spring 注入 | Java SPI |
|--|-------------|----------|
| 发现时机 | 启动扫描 classpath 上的 `@Component` | 读取 JAR 内 `META-INF/services/` 文件 |
| 实现类位置 | 通常和主程序打在同一 JAR | **可以在独立 JAR**，运行时再加进来 |
| 是否依赖 Spring | 是 | **否**（纯 JDK） |

Stage 6 选 SPI，是因为 Stage 8 要在**不重启**的情况下换 JAR——实现类不能写死在 gateway-core 的 classpath 里。

### 2. 注册文件：`META-INF/services/<接口全限定名>`

插件 JAR 里必须有一个文本文件，路径**固定**：

```
plugin-jwt-0.0.1-SNAPSHOT.jar
└── META-INF/
    └── services/
        └── com.codecore.gateway.plugin.AuthPlugin   ← 文件名 = 接口全限定名
```

文件内容：**一行一个实现类的全限定名**（本项目只有一行）：

```
com.codecore.gateway.plugin.jwt.JwtAuthPlugin
```

规则要点：

| 规则 | 说明 |
|------|------|
| 文件名 | 必须是接口的**完整类名**，不能写错一个字母 |
| 文件内容 | 实现类的**完整类名**；多个实现就写多行 |
| 注释 | 不支持 `#` 注释，写了会被当成类名解析失败 |
| 打包 | 必须在 `src/main/resources/META-INF/services/` 下，Maven 才会打进 JAR |

可以用下面命令亲自验证：

```bash
jar xf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar META-INF/services/com.codecore.gateway.plugin.AuthPlugin
type META-INF\services\com.codecore.gateway.plugin.AuthPlugin
```

### 3. `ServiceLoader.load` 两个参数分别做什么

```java
ServiceLoader.load(AuthPlugin.class, pluginClassLoader);
//              ──────── SPI 接口 ────────  ───── 用哪个 ClassLoader ─────
```

#### 参数 1：`AuthPlugin.class`

告诉 `ServiceLoader`：

1. 去 ClassLoader 可见的 classpath 里找 `META-INF/services/com.codecore.gateway.plugin.AuthPlugin`
2. 读出里面的实现类名（如 `com.codecore.gateway.plugin.jwt.JwtAuthPlugin`）
3. 返回的类型是 `ServiceLoader<AuthPlugin>`，实例化出来的对象可当作 `AuthPlugin` 使用

#### 参数 2：`pluginClassLoader`（关键）

**必须传入插件专用的 ClassLoader**，不能省略。

若写成：

```java
ServiceLoader.load(AuthPlugin.class);  // 使用当前线程的 Context ClassLoader
```

则 `ServiceLoader` 会在 **gateway-core 的 classpath** 里找注册文件——而 `JwtAuthPlugin` 在 `plugin-jwt` 独立 JAR 里，**找不到**，启动直接失败。

传入 `pluginClassLoader` 的效果：

```
ServiceLoader 读注册文件  → 从 plugin-jwt.jar 里读 META-INF/services/...
ServiceLoader 加载实现类  → 用 pluginClassLoader 加载 JwtAuthPlugin.class
ServiceLoader 实例化      → new JwtAuthPlugin()（通过无参构造器）
```

### 4. `ServiceLoader` 内部加载流程（逐步）

结合 `StaticPluginLoader` 源码，启动时实际发生：

```
① findJwtPluginJar()
   扫描 plugins/ 目录 → 找到 plugin-jwt-0.0.1-SNAPSHOT.jar

② createPluginClassLoader(pluginJar)
   new URLClassLoader([plugin-jwt.jar 的 URL], parent = AuthPlugin 的 ClassLoader)

③ ServiceLoader.load(AuthPlugin.class, pluginClassLoader)
   3a. 在 plugin-jwt.jar 内打开
       META-INF/services/com.codecore.gateway.plugin.AuthPlugin
   3b. 读到一行：com.codecore.gateway.plugin.jwt.JwtAuthPlugin
   3c. 调用 pluginClassLoader.loadClass("...JwtAuthPlugin")
   3d. 缓存 Provider 信息（此时通常还未 new 实例）

④ serviceLoader.findFirst()
   4a. 取第一个 Provider
   4b. 调用 provider.get() → 反射执行 new JwtAuthPlugin()
   4c. 返回 AuthPlugin 实例

⑤ plugin.init(pluginContext)
   网关把配置、日志能力注入插件
```

用图表示：

```
┌─────────────────────────────────────────────────────────────────┐
│ gateway-core（Consumer）                                         │
│                                                                 │
│  StaticPluginLoader.loadAndInit()                               │
│       │                                                         │
│       ▼                                                         │
│  URLClassLoader ──────────────► plugin-jwt.jar                  │
│  parent = gateway-api CL              │                         │
│                                       ├── META-INF/services/    │
│                                       │   └── AuthPlugin 注册文件│
│                                       ├── JwtAuthPlugin.class   │
│                                       ├── PluginJwtValidator    │
│                                       └── nimbus-jose-jwt...    │
│       │                                                         │
│       ▼                                                         │
│  ServiceLoader.load(AuthPlugin.class, pluginClassLoader)        │
│       │  读注册文件 → loadClass → new JwtAuthPlugin()           │
│       ▼                                                         │
│  AuthPlugin plugin  ──init()──►  SpringPluginContext            │
└─────────────────────────────────────────────────────────────────┘
         ▲
         │ 共享接口类型（同一份 class 对象）
         │
┌────────┴────────┐
│  gateway-api    │  ← AuthPlugin、AuthRequest、AuthResult 接口
│  （SPI 契约）    │     parent ClassLoader 指向这里
└─────────────────┘
```

### 5. 为什么要配 `URLClassLoader`，且 parent 是 `gateway-api`

```java
ClassLoader parent = AuthPlugin.class.getClassLoader();  // gateway-api 的 ClassLoader
return new URLClassLoader(new URL[]{jarUrl}, parent);
```

**ClassLoader 双亲委派**（简化版）：

```
pluginClassLoader 加载某个类
  → 先问 parent（gateway-api CL）能不能加载
  → parent 能加载（如 AuthPlugin 接口）→ 直接用 parent 的版本
  → parent 不能加载（如 JwtAuthPlugin 实现）→ 自己从 plugin-jwt.jar 加载
```

这样设计的原因：

| 设计 | 原因 |
|------|------|
| parent = `gateway-api` | 接口 `AuthPlugin` 在 parent 里只有**一份**；网关和插件用的是**同一个** `AuthPlugin.class`，`instanceof`、方法调用才正常 |
| parent ≠ `gateway-core` | 插件不应看见 core 内部类（Filter、Manager 等），减少耦合和类冲突 |
| 独立 `URLClassLoader` | 插件实现类、nimbus-jose-jwt 等从插件 JAR 加载；换插件 JAR = 换 ClassLoader（Stage 8 热部署的基础） |

若 parent 设错（例如 `null` 或 core 的 AppClassLoader），可能出现：

- `ClassCastException`：两个 ClassLoader 各加载了一份 `AuthPlugin`，同名但不是同一个 Class
- 插件看不见 SPI 接口，或网关 cast 插件实例失败

### 6. `findFirst()` 与懒加载

```java
AuthPlugin plugin = serviceLoader.findFirst().orElseThrow(...);
```

| API | 行为 |
|-----|------|
| `ServiceLoader.load(...)` | 只**解析**注册文件，列出 Provider，一般不立刻 new 对象 |
| `.iterator()` / `.forEach()` | 遍历时对每个 Provider 调用 `get()`，**此时才实例化** |
| `.findFirst()` | 取第一个 Provider 并 `get()`，只实例化**一个**实现 |

本项目 Stage 6 约定：**一个 plugins 目录里只有一个 JWT 插件 JAR、一个实现类**，所以 `findFirst()` 足够。  
Stage 7 若支持多插件，会改用遍历 + 按 `metadata().name()` 选择。

实现类要求：

- 必须有一个** public 无参构造器**（默认就有，除非你自己删掉）
- 不要写成 Spring `@Component`——实例由 `ServiceLoader` 创建，不是 Spring 容器

### 7. 和 Stage 5 的对比（帮助记忆）

| 步骤 | Stage 5 | Stage 6 |
|------|---------|---------|
| 谁创建 `JwtAuthPlugin` | Spring 容器（`@Component`） | `ServiceLoader`（反射 `new`） |
| 谁找到实现类 | `@ComponentScan` | `META-INF/services` + `ServiceLoader` |
| 实现类在哪 | gateway-core 模块内 | plugin-jwt 独立 JAR |
| 怎么换实现 | 改代码、重打 core | 换 plugins 下的 JAR（Stage 8 无需重启） |

### 8. 常见错误排查

| 现象 | 可能原因 |
|------|----------|
| `插件 JAR 中未找到 AuthPlugin SPI 实现` | JAR 里没有 `META-INF/services/com.codecore.gateway.plugin.AuthPlugin`，或文件名拼错 |
| `ServiceConfigurationError` | 注册文件里的类名写错，或实现类没有 public 无参构造器 |
| `ClassCastException: JwtAuthPlugin cannot be cast to AuthPlugin` | 没用插件 ClassLoader，或 parent ClassLoader 配错，接口被加载了两份 |
| `NoClassDefFoundError: nimbus...` | plugin-jwt 没把依赖打进 JAR，且运行时 classpath 也没有 nimbus（Stage 6 插件 JAR 需自带依赖或后续用 fat jar） |

> **说明：** 当前 `plugin-jwt` 是普通 JAR，nimbus-jose-jwt 在插件 JAR 内；gateway-core 的 `URLClassLoader` 只加载 plugin-jwt 这一个 URL。若 nimbus 不在 plugin-jwt.jar 里，需要在 Stage 7 考虑 **shaded/fat plugin jar** 或扩展 ClassLoader 的 URL 列表。

### 9. 小结（背这几句就够）

| 要点 | 一句话 |
|------|--------|
| SPI 注册 | `META-INF/services/接口全名` 文件里写实现类全名 |
| `load(接口, ClassLoader)` | 第二个参数决定**从哪个 JAR**读注册文件、**用谁**加载实现类 |
| `URLClassLoader` | 把 plugin-jwt.jar 挂进 JVM；parent 指向 gateway-api 保证接口只有一份 |
| `findFirst()` | 实例化第一个 Provider，得到 `AuthPlugin` 供网关调用 |
| 为何不用 Spring 扫描 | 实现类在运行时 JAR 里，不在 core 的编译 classpath 上 |

---

## 动手实验

### 实验 1：确认插件 JAR 已生成

```bash
cd E:\MyGithub\api-gateway
mvn clean package

# 应存在插件 JAR 与网关旁的 plugins 目录
dir gateway-core\target\plugins
# → plugin-jwt-0.0.1-SNAPSHOT.jar
```

### 实验 2：三进程联调（认证行为与 Stage 5 一致）

```bash
# 终端 1 — Mock Auth
java -jar mock-jwks-server/target/mock-jwks-server-0.0.1-SNAPSHOT.jar

# 终端 2 — 模拟下游
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar

# 终端 3 — 网关（mvn package 后可直接 java -jar，会自动找到 target/plugins）
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar
```

**观察启动日志：**

```
从目录 .../gateway-core/target/plugins 加载认证插件: plugin-jwt-0.0.1-SNAPSHOT.jar
JWT 认证插件已初始化: jwt-auth v1.0.0
```

```bash
# 获取 JWT
curl -X POST http://localhost:8082/admin/issue-token

# 无 Token → 401
curl -i http://localhost:8080/api/hello

# 合法 JWT → 200
curl -i -H "Authorization: Bearer <JWT>" http://localhost:8080/api/hello
```

### 实验 3：验证插件与 core 解耦

```bash
# 查看 plugin-jwt JAR 内容（不含 gateway-core 类）
jar tf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar | findstr JwtAuthPlugin
jar tf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar | findstr META-INF/services
```

### 实验 4：亲手走一遍 SPI 发现过程

```bash
# 1. 解出 SPI 注册文件，确认内容与接口名一致
cd E:\MyGithub\api-gateway
jar xf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar META-INF/services/com.codecore.gateway.plugin.AuthPlugin
type META-INF\services\com.codecore.gateway.plugin.AuthPlugin
# 期望输出：com.codecore.gateway.plugin.jwt.JwtAuthPlugin

# 2. 启动网关，在日志里定位两行（对应 ServiceLoader 成功）
#    - StaticPluginLoader: 从目录 ... 加载认证插件: plugin-jwt-....jar
#    - SpringPluginContext: JWT 认证插件已初始化: jwt-auth v1.0.0
# 第二行来自 plugin.init()，说明 findFirst().get() 已成功 new 出 JwtAuthPlugin

# 3.（可选）故意改坏注册文件再打包，观察 ServiceConfigurationError / 找不到实现
```

### 思考题

1. 为什么插件 ClassLoader 的 parent 是 `gateway-api` 而不是 `gateway-core`？
2. Stage 7 的 PluginManager 会比 StaticPluginLoader 多做什么？
3. 若 `META-INF/services/com.codecore.gateway.plugin.AuthPlugin` 里写两行实现类，`findFirst()` 会加载哪一个？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | BUILD SUCCESS，含 plugin-jwt 测试 |
| 2 | `gateway-core/target/plugins/` | 存在 `plugin-jwt-*.jar` |
| 3 | 网关启动日志 | 加载 plugin-jwt + 插件 init 日志 |
| 4 | 合法 JWT 经网关 | 200（与 Stage 5 行为一致） |
| 5 | `plugin-jwt` 不依赖 `gateway-core` | pom 仅依赖 gateway-api |

---

## 变更记录

| 类型 | 路径 |
|------|------|
| 新增 | `plugin-jwt/` 模块、`StaticPluginLoader`、`GatewayPluginProperties` |
| 迁移 | JWT 验签逻辑从 core → plugin-jwt |
| 删除 | core 内 `JwtAuthPlugin`、`JwksKeyProvider`、`LocalJwtValidator` 等 |
| 修改 | `AuthPluginManager`、`gateway-core/pom.xml`（copy 插件 JAR）、`application.yml` |

### Stage 5 → Stage 6 对比

| 维度 | Stage 5 | Stage 6 |
|------|---------|---------|
| JwtAuthPlugin 位置 | gateway-core | plugin-jwt JAR |
| 加载方式 | Spring `@Component` 注入 | URLClassLoader + SPI |
| JWKS 拉取 | core 内 WebClient | 插件内 JDK HttpClient |
| 改认证逻辑 | 需重打 gateway-core | 只需重打 plugin-jwt |

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| 独立 JAR | 插件是 artifact，不是 core 里的一个类 |
| SPI 注册 | `META-INF/services/接口全名` → 实现类全名 |
| ServiceLoader | `load(接口, 插件ClassLoader)` 读注册文件并实例化 Provider |
| ClassLoader | parent=gateway-api，保证 SPI 接口只有一份 |
| plugins/ | Maven 构建时复制，运行时加载 |
| Stage 6 不做 | 热部署、Admin API——Stage 7~9 |

**下一课预告（Stage 7）：** PluginManager 统一管理插件生命周期与冷加载。

---

# Stage 7 — 冷加载插件管理器

**工程名：** `gateway-plugin-manager-cold`  
**提交：** 待提交 · 2026-07-15  
**一句话：** 用 `PluginClassLoader` + `AuthPluginManager` 统一扫描、发现、激活、销毁插件——仍需重启，但生命周期已完整。

---

## 学习目标

1. 理解冷加载（Cold Load）与热部署（Hot Deploy）的区别
2. 实现专用 `PluginClassLoader` 并正确设置 parent
3. 用 Manager 统一：扫描 → SPI 发现 → 按名激活 → destroy/close
4. 将 `plugin-jwt` 打成 shaded JAR，避免独立 ClassLoader 缺依赖

---

## 为什么要做这个 Stage

Stage 6 的 `StaticPluginLoader` 写死找 `plugin-jwt*.jar`，职责散乱，关闭时也不释放 ClassLoader。  
Stage 7 把加载逻辑收拢到 Manager，并抽出 `PluginClassLoader`，为 Stage 8「运行中换 JAR」铺平道路：

```
Stage 6:  硬编码找 plugin-jwt → ServiceLoader → init（无 destroy/close）
Stage 7:  扫描全部 JAR → PluginClassLoader → 按 active 激活 → PreDestroy 清理
Stage 8:  运行中再走一遍 load → 原子切换 current → destroy 旧插件
```

**冷加载**：进程启动时加载一次，改 JAR 需重启。  
**热部署**：运行中替换，不重启（下一 Stage）。

---

## 核心变更

| 组件 | 作用 |
|------|------|
| `PluginClassLoader` | 每 JAR 一个 URLClassLoader，parent=gateway-api，可 close |
| `LoadedPlugin` | 绑定 plugin + ClassLoader + JAR 路径 |
| `AuthPluginManager` | 扫描 / 发现 / 按 `active` 激活 / `@PreDestroy` 清理 |
| 删除 `StaticPluginLoader` | 职责并入 Manager |
| `plugin-jwt` shade | nimbus 打进插件 JAR，独立 ClassLoader 可运行 |

**配置：**

```yaml
gateway:
  plugins:
    directory: plugins
    active: jwt-auth   # 与 PluginMetadata.name() 一致
```

### 启动流程

```
AuthPluginManager 构造
  → resolvePluginsDirectory()
  → list *.jar
  → 每个 JAR: new PluginClassLoader → ServiceLoader 发现 AuthPlugin
  → 按 gateway.plugins.active 选中
  → 关闭未选中 JAR 的 ClassLoader
  → selected.init(context)
  → Filter 经 getCurrentPlugin() 调用
```

### 关闭流程

```
@PreDestroy destroy()
  → plugin.destroy()
  → classLoader.close()   # 释放 Windows 上对 JAR 的文件锁
```

---

## 动手实验

### 实验 1：确认冷加载日志

```bash
cd E:\MyGithub\api-gateway
mvn clean package

java -jar mock-jwks-server/target/mock-jwks-server-0.0.1-SNAPSHOT.jar
java -jar mock-backend/target/mock-backend-0.0.1-SNAPSHOT.jar
java -jar gateway-core/target/gateway-core-0.0.1-SNAPSHOT.jar
```

**期望启动日志类似：**

```
扫描插件目录 .../target/plugins，共 1 个 JAR
发现插件: jwt-auth v1.0.0 ← plugin-jwt-0.0.1-SNAPSHOT.jar
认证插件已激活: jwt-auth v1.0.0（JAR: plugin-jwt-0.0.1-SNAPSHOT.jar）
```

```bash
curl -X POST http://localhost:8082/admin/issue-token
curl -i -H "Authorization: Bearer <JWT>" http://localhost:8080/api/hello
```

### 实验 2：错误的 active 名称

临时把 `application.yml` 中 `gateway.plugins.active` 改成 `not-exist`，重启网关：

```
未找到名为 'not-exist' 的插件，已发现: jwt-auth
```

改回 `jwt-auth`。

### 实验 3：观察 shaded JAR

```bash
# shaded 后 JAR 应包含 nimbus 包
jar tf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar | findstr nimbus
jar tf plugin-jwt/target/plugin-jwt-0.0.1-SNAPSHOT.jar | findstr META-INF/services
```

### 思考题

1. 为什么关闭时必须 `classLoader.close()`？（提示：Windows 文件锁、Stage 8 覆盖 JAR）
2. 若 `plugins/` 里有两个不同 name 的插件 JAR，怎样切换激活的那个？
3. Stage 8 热替换时，为什么要「先 init 新插件，再切换引用，最后 destroy 旧插件」？

---

## 验收清单

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | `mvn clean package` | BUILD SUCCESS |
| 2 | 启动日志 | 扫描 + 发现 + 激活 jwt-auth |
| 3 | 合法 JWT | 200（行为与 Stage 6 一致） |
| 4 | `listDiscoveredMetadata()` | 含 jwt-auth |
| 5 | 进程退出 | 日志可见 ClassLoader 已关闭（可选观察） |

---

## 变更记录

| 类型 | 路径 |
|------|------|
| 新增 | `PluginClassLoader`、`LoadedPlugin`、`PluginClassLoaderTests` |
| 重写 | `AuthPluginManager`（冷加载全生命周期） |
| 删除 | `StaticPluginLoader` |
| 修改 | `GatewayPluginProperties`（+active）、`plugin-jwt` shade、`application.yml` |

### Stage 6 → Stage 7 对比

| 维度 | Stage 6 | Stage 7 |
|------|---------|---------|
| 加载器 | StaticPluginLoader（硬编码前缀） | AuthPluginManager 扫描全部 JAR |
| ClassLoader | 裸 URLClassLoader | PluginClassLoader（可 close） |
| 激活方式 | 固定第一个 SPI | `gateway.plugins.active` 按名 |
| 生命周期 | 仅 init | init + @PreDestroy destroy/close |
| 插件 JAR | 普通 JAR | shaded（含 nimbus） |

---

## 本课小结

| 要点 | 记住这句话 |
|------|------------|
| 冷加载 | 启动时加载；改插件需重启 |
| PluginClassLoader | 一 JAR 一 Loader，parent=api，可关闭 |
| Manager | 扫描、发现、激活、销毁的唯一入口 |
| active | 按 metadata.name 选择激活哪个插件 |
| Stage 7 不做 | 运行中热替换——Stage 8 |

**下一课预告（Stage 8）：** 不重启替换插件 JAR（热部署核心）。

---

# 附录

## Git 提交记录

| 顺序 | 提交 | 说明 | Stage |
|------|------|------|-------|
| 1 | `8b082f5` | 需求与架构文档 | 文档 |
| 2 | `9fc87b3` | Maven 骨架 | 0 |
| 3 | `f49634c` | 静态路由 + mock 下游 | 1 |
| 4 | `9a550e8` | 硬编码 Bearer 认证 | 2 |
| 5 | `863a027` | JWT 本地公钥验签 | 3 |
| 6 | `ae9fe90` | RSA 密钥对生成工具 | 工具 |
| 7 | `7ae0e87` | JWKS 远端拉取 + mock-jwks-server | 4 |
| 8 | `16adc84` | AuthPlugin SPI 接口层 | 5 |
| 9 | `5643fb6` | mock-jwks 多钥并存轮换 | 4 改进 |
| 10 | `5b57fd3` | plugin-jwt 独立 JAR + 静态加载 | 6 |
| 11 | 待提交 | PluginClassLoader + 冷加载 Manager | 7 |

## 后续课程预告

| Stage | 课题 | 你将学到 |
|-------|------|----------|
| 8 ~ 10 | 热部署 | 不重启换 JAR、Admin API、回滚 |
| 11 ~ 12 | 生产化 | 集群同步、监控、安全 |

每完成一个 Stage，在本文档追加对应教学章节。
