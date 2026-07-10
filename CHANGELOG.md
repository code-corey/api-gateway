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
| 6 | `gateway-jwt-plugin-jar` | JWT 独立 JAR + 静态加载 | 待提交 | ✅ |
| 7 ~ 12 | … | 见 README | — | ⏳ |

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
**提交：** 待提交 · 2026-07-10  
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

### 思考题

1. 为什么插件 ClassLoader 的 parent 是 `gateway-api` 而不是 `gateway-core`？
2. Stage 7 的 PluginManager 会比 StaticPluginLoader 多做什么？

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
| SPI | `META-INF/services` + ServiceLoader 发现实现 |
| ClassLoader | 插件与 core 类隔离，共享 gateway-api 契约 |
| plugins/ | Maven 构建时复制，运行时加载 |
| Stage 6 不做 | 热部署、Admin API——Stage 7~9 |

**下一课预告（Stage 7）：** PluginManager 统一管理插件生命周期与冷加载。

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
| 10 | 待提交 | plugin-jwt 独立 JAR + 静态加载 | 6 |

## 后续课程预告

| Stage | 课题 | 你将学到 |
|-------|------|----------|
| 6 | 独立 JAR 插件 | plugin-jwt 模块打包 |
| 5 ~ 7 | 插件体系 | SPI、ClassLoader、冷加载 |
| 8 ~ 10 | 热部署 | 不重启换 JAR、Admin API、回滚 |
| 11 ~ 12 | 生产化 | 集群同步、监控、安全 |

每完成一个 Stage，在本文档追加对应教学章节。
