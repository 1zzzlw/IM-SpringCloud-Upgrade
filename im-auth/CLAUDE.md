# CLAUDE.md — im-auth

## 模块职责

认证/授权微服务，端口 **8081**，Nacos 注册名 `im-auth`。负责用户注册、登录、Token 生成与刷新、验证码、用户搜索。

## 技术标签

- 双 Token 认证体系（短期 Access Token + 长期 Refresh Token 分层过期）
- Redis Lua 脚本原子化登录（并发安全的旧 Token 清理 + 新 Token 写入）
- Kaptcha 图形验证码生成与 Redis 缓存校验（5min TTL）
- AOP + ConcurrentHashMap + AtomicInteger 进程内接口限流
- ThreadLocal 用户上下文管理（UserHolder 请求级隔离）
- Feign 声明式跨服务调用（登录后加载好友列表 + 注册后创建会话）

> 我设计并实现了一套基于双 Token 机制的认证中心，通过 Redis Lua 脚本实现原子化登录——在高并发下并发安全地清理旧会话、写入新 Token 对，结合 AOP 切面 + ConcurrentHashMap 实现接口级进程内限流，通过 ThreadLocal 管理请求级用户上下文，本质上是一个**具备并发安全性和基础防护能力的统一认证微服务**。

## 端点一览（`/user`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/user/login` | 账号密码登录，返回双 Token + 好友列表 |
| `GET` | `/user/verifyCode` | 生成 Kaptcha 图形验证码（存 Redis，5min TTL） |
| `GET` | `/user/pendingLogin` | 扫码/待确认登录，校验 refreshToken → 重新签发 |
| `POST` | `/user/register` | 注册（含头像上传 MinIO），调用 Feign 创建聊天会话 |
| `POST` | `/user/phoneCode` | 生成 6 位手机验证码（明文返回，仅开发用） |
| `POST` | `/user/list/ids` | 批量查询用户（供其他微服务 Feign 调用） |
| `GET` | `/user/search` | 按手机号/账号搜索用户，附带好友关系状态 |
| `POST` | `/user/refreshToken/{userId}` | 刷新 Token（从 UserHolder 获取当前用户） |

## 核心架构

### 双 Token 认证

- **Access Token**: `access-secret-key` 签名，1 小时过期，存 Redis Hash `login:user:Info:accessToken:{token}`
- **Refresh Token**: `fresh-secret-key` 签名，365 天过期，存 Redis Hash `login:user:Info:refreshToken:{token}`
- 每个用户在 Redis 有一个 SET `login:user:tokenList:{userId}` 记录所有活跃 Token
- 登录时通过 [login_token_atomic_operation.lua](src/main/resources/login_token_atomic_operation.lua) **原子**清理旧 Token 并写入新 Token

### Lua 脚本原子操作

```
KEYS[1]=login:user:tokenList:{userId} (SET)
KEYS[2]=login:user:Info:accessToken:{token} (Hash)
KEYS[3]=login:user:Info:refreshToken:{token} (Hash)

流程：
1. 读取旧 Token SET → 逐个删除旧 access/refresh Token 的 Hash
2. 删除旧 SET → 添加新 Token 到 SET + 设置 TTL
3. HSET 用户信息到新 access/refresh Token Hash + 设置 TTL
```

### 限流

- `@UrlLimit(LimitKeyType.IP)` 注解在登录接口上，默认 30 次/分钟
- `UrlLimitAspect` 使用 `ConcurrentHashMap<String, AtomicInteger>` 进程内限流
- **注意**：是单实例限流，多 Pod 不共享

## 关键类

- [UserServiceImpl.java](src/main/java/com/zzzlew/server/impl/UserServiceImpl.java) — 核心业务逻辑，`generateAndStoreWithUpdateToken()` 是 Token 生成入口
- [UserController.java](src/main/java/com/zzzlew/controller/UserController.java) — 所有 REST 端点
- [KaptchaConfig.java](src/main/java/com/zzzlew/config/KaptchaConfig.java) — 验证码生成器（Kaptcha 2.3.2）
- [UrlLimitAspect.java](src/main/java/com/zzzlew/aop/UrlLimitAspect.java) — 进程内限流切面（每 60s 清空计数器）
- [UserMapper.java](src/main/java/com/zzzlew/mapper/UserMapper.java) — 内联注解 SQL
- [UserMapper.xml](src/main/resources/mapper/UserMapper.xml) — 5 条 SQL（insertUserAuth / selectUserAuthListByUserIdList / selectUserInfoById / insertUserInfo / getByPhoneOrAccount）

## 数据库

操作表：`user_info`（完整用户资料）、`user_auth`（认证视图，供批量查询）

## Feign 调用其他服务

- `SocialClient.initFriendList()` — 登录后加载好友列表到 Redis
- `ChatClient.createConversation()` — 注册后创建 AI 会话

## 依赖

- `common-web`（拦截器、异常处理、Knife4j）
- `common-storage`（MinIO 头像上传）
- `im-api`（Feign 客户端）
- `mysql-connector-java`、`mybatis-spring-boot-starter`、`nacos-discovery`

## 已知问题/TODO

- 密码明文比较（解密代码已注释）
- CAPTCHA 验证已注释跳过
- 手机验证码明文返回（未接短信服务）
- `search()` 标注未来分库分片需改为分块查询
