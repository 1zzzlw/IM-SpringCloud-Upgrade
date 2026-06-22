# CLAUDE.md — im-api

## 模块职责

**Feign API 库（JAR）**，不是可运行服务。定义所有跨服务 REST 调用的 `@FeignClient` 接口 + 共享的 `DefaultFeignConfig`（日志 + 用户上下文传播）。

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
