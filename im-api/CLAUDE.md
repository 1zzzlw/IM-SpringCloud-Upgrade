# CLAUDE.md — im-api

## 模块职责

**Feign API 库（JAR）**，不是可运行服务。定义所有跨服务 REST 调用的 `@FeignClient` 接口 + 共享的 `DefaultFeignConfig`（日志 + 用户上下文传播）。

## 技术标签

- Feign 声明式 HTTP 客户端接口库（接口即契约，JAR 包模式解耦服务间调用）
- 用户上下文跨服务传播（RequestInterceptor 从 ThreadLocal 读用户 → JSON 序列化 → 注入 user-info HTTP Header）
- 显式配置引用模式（@FeignClient(configuration=…) 按需引用，避免全局 @Configuration 组件扫描污染）
- 接口聚合层设计（4 个 FeignClient 覆盖 Auth / Chat / Social / Pay 四服务的对外 API）
- 零 Spring Boot 依赖（无启动类、无配置文件，纯接口 + 配置的轻量 JAR）

> 我设计了一个基于 Feign 声明式 HTTP 客户端的微服务接口聚合库（JAR 包模式），通过 RequestInterceptor 实现用户上下文的跨服务自动传播（从 ThreadLocal 读取 → JSON 序列化 → 注入 HTTP Header），通过显式配置引用而非 @Configuration 自动扫描避免全局组件污染，本质上是一个**解耦合、可传播用户上下文、零 Spring Boot 依赖的轻量级 RPC 接口聚合层**。

## Feign 客户端

### AuthClient → `im-auth`
```java
POST /user/list/ids → Result<List<UserAuth>>
```
批量查询用户信息（供其他服务调用）

### ChatClient → `im-chat`
```java
POST /conversation/create    → 创建会话（单聊/群聊）
POST /conversation/inviteFriend  → 邀请好友入群
POST /conversation/internal/updateGroupAvatar  → 内部更新群头像
GET  /conversation/query     → 查询会话详情
```

### PayClient → `im-pay`
```java
POST /wallet/deduct → Result<Void>
```
扣除钱包余额（红包场景）

### SocialClient → `im-social`
```java
GET  /friend/init/list?isInit=  → 获取好友列表
POST /apply/groupApply          → 发送群申请
```

## DefaultFeignConfig

[DefaultFeignConfig.java](src/main/java/com/zzzlew/config/DefaultFeignConfig.java) 提供两个 Bean：

1. **`feignLogLevel`**：`Logger.Level.FULL`（全量请求/响应日志）
2. **`userInfoRequestInterceptor`**：从 `UserHolder.getUser()` 获取当前用户 → JSON 序列化 → 设置 `user-info` 请求头

**注意**：类上没有 `@Configuration` 注解，通过 `@FeignClient(configuration = ...)` 显式引用，避免被全局组件扫描。

## 用户上下文传播链路

```
Gateway → user-info 头 → 下游服务拦截器 → UserHolder(ThreadLocal)
                                                ↓
                              Feign RequestInterceptor → user-info 头 → 再下游
```

## 依赖的类型（来自 common-model / common-base）

| 类型 | 来源 | 用途 |
|------|------|------|
| `Result<T>` | common-model | 统一响应包装 |
| `UserAuth` | common-model | 用户认证实体 |
| `ConversationVO` | common-model | 会话详情 VO |
| `FriendRelationVO` | common-model | 好友关系 VO |
| `GroupMemberDTO` | common-model | 群成员 DTO |
| `GroupApplyDTO` | common-model | 群申请 DTO |
| `DeductDTO` | common-model | 钱包扣款 DTO |
| `UserBaseDTO` | common-base | 当前用户信息 |
| `UserHolder` | common-base | ThreadLocal 工具 |

## 依赖

- `common-model` + `common-base`（共享类型）
- `spring-cloud-starter-openfeign` + `spring-cloud-starter-loadbalancer`
- `hutool-all`（JSONUtil）
- **无** Spring Boot 入口类，无配置文件
