# CLAUDE.md — common-base

## 职责

共享常量、枚举、异常、工具类。依赖 `common-model`。

## 技术标签

- ThreadLocal 用户上下文持有者（UserHolder：save → getUser → removeUser，请求级隔离）
- JWT 工具类（HS256 对称签名，createJWT / parseJWT，静态方法无状态设计）
- 分层异常体系（BaseException 基类 + 9 个语义化子类：Token过期/密码错误/账号不存在/手机号已存在 等）
- 系统消息枚举（13 种消息类型 + getByValue() 查找，覆盖撤回/删除/添加好友/踢出群等场景）
- RabbitMQ 全局常量管理（Exchange 名 / 队列前缀 / Routing Key 集中定义，多模块引用）

> 我设计了一套共享基础设施层，通过 ThreadLocal + 拦截器实现请求级用户上下文隔离（写入 → 读取 → 清理的完整生命周期），通过 BaseException + 9 个子类构建分层异常体系（每种异常对应明确的业务语义和 HTTP 状态码），通过 JwtUtil 封装 HS256 对称签名 JWT 的创建与解析，通过 RabbitMQConstant 集中管理 MQ 路由拓扑常量，本质上是一个**为所有微服务提供公共语言（常量/枚举/异常/工具）的基础设施层**。

## 关键类

### 常量

| 类 | 用途 |
|----|------|
| `constant/RabbitMQConstant.java` | MQ Exchange 名、队列前缀、路由键（所有模块引用） |
| `constant/MessageConstant.java` | 面向用户的错误消息字符串 |
| `constant/RegexPatterns.java` | 手机号正则 `PHONE_REGEX` |
| `constant/JwtClaimsConstant.java` | JWT Claim 键常量 |

### 枚举

| 类 | 用途 |
|----|------|
| `enums/SystemMessage.java` | 13 种系统消息类型（撤回、删除、添加好友、踢出群等），`getByValue()` 查找 |
| `enums/LimitKeyType.java` | ID / IP |

### 异常

`exception/BaseException`（继承 RuntimeException）及 7 个子类：
- `AccountNotFoundException`、`PasswordErrorException`、`PhoneErrorException`、`PhoneAlreadyExistsException`
- `TokenExpiredException`、`TokenNotFoundException`、`TokenGenerateException`
- `IPException`、`LoginCodeGenerateException`

### 工具类

| 类 | 用途 |
|----|------|
| `utils/UserHolder.java` | **ThreadLocal<UserBaseDTO>**：`save()`、`getUser()`、`removeUser()`。Web 拦截器写入，Feign 拦截器读取，请求结束清理 |
| `utils/JwtUtil.java` | 静态 JWT 创建/解析（HS256），`createJWT(secret, ttl, claims)`、`parseJWT(secret, token)` |
| `utils/RegexUtils.java` | 手机号校验 `isPhoneInvalid(phone)` |

## 依赖

- `common-model`
- `jjwt`
