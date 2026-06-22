# CLAUDE.md — im-gateway

## 模块职责

Spring Cloud Gateway 网关，端口 **8080**，所有 REST 请求的入口。负责全局 Token 鉴权和路由转发。**不含** CORS 配置（依赖外部 Nginx）。

## 路由表

| 路径前缀 | 目标服务 |
|----------|----------|
| `/user/**` | `lb://im-auth` |
| `/ai-message/**`, `/conversation/**`, `/message/**`,`/redPacket/**` | `lb://im-chat` |
| `/apply/**`, `/friend/**` | `lb://im-social` |
| `/favorites/**` | `lb://im-content` |
| `/moments/**` | `lb://im-moments` |
| `/wallet/**` | `lb://im-pay` |

## 关键类（仅 4 个文件）

- [GatewayApplication.java](src/main/java/com/zzzlew/GatewayApplication.java) — `@SpringBootApplication` 入口
- [AuthGlobalFilter.java](src/main/java/com/zzzlew/filters/AuthGlobalFilter.java) — 核心，`GlobalFilter`，`Order=0`
- [ReactiveRedisConfig.java](src/main/java/com/zzzlew/config/ReactiveRedisConfig.java) — 响应式 Redis `ReactiveRedisTemplate` Bean（String 序列化）
- [AuthProperties.java](src/main/java/com/zzzlew/properties/AuthProperties.java) — 读取 `auth.excludePaths` / `auth.refreshPaths` 配置

## 鉴权流程（AuthGlobalFilter）

```
请求进入 → isExclude(path) → 放行（登录/注册/验证码/Swagger/上传校验/favicon）
       → 提取 Authorization 头
       → isRefreshPath(path)?
           → 是：验证长期Token（fresh-secret-key）→ 查Redis → 刷新TTL → 放行
           → 否：验证短期Token（access-secret-key）→ 查Redis → 刷新TTL → 放行
       → user-info 头透传用户 JSON 给下游
```

- **排除路径**：登录、注册、验证码、Swagger/Knife4j、文件上传分块校验、`/error/**`
- **刷新路径**（长期 Token）：`/user/refreshToken/**`、`/message/verifyUploadToken/**`、`/message/merge`、`/message/init/list/**`、`/conversation/init/list`、`/friend/init/list`
- 401 = Token 无效/过期/Redis 中不存在
- 用户信息以 JSON 存入 `user-info` 请求头传给下游

## 依赖

- `common-model`（UserBaseDTO）
- `common-redis`（JwtProperties、JwtUtil、RedisConstant）
- `spring-boot-starter-data-redis-reactive`（ReactiveRedisTemplate + Lettuce 连接池）
- `spring-cloud-starter-gateway`、`nacos-discovery`、`loadbalancer`

## 配置注意

- `application.yml` 是**唯一**配置文件（没有 dev/docker profile 区分）
- Redis 指向 `39.106.183.13:6379`（公网），密码 `123456`
- JWT 密钥和 `auth.excludePaths` / `auth.refreshPaths` 均在 yml 中配置
- Gateway 使用**响应式编程**（WebFlux），不能用阻塞式 JDBC/MyBatis
