# CLAUDE.md — common-base

## 职责

共享常量、枚举、异常、工具类。依赖 `common-model`。

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
