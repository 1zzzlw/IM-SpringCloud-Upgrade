# CLAUDE.md — common-web

## 职责

Spring Web MVC 横切关注点模块。所有 REST 微服务的**核心依赖**。提供拦截器、全局异常处理、分布式锁、MinIO 配置、RabbitMQ 配置、Knife4j。

## 关键类

### Web 层配置

| 类 | 作用 |
|----|------|
| `config/WebMvcConfig.java` | 注册 `UserInfoInterceptor` 拦截所有 `/**` 路径 |
| `interceptors/UserInfoInterceptor.java` | `preHandle`：从 `user-info` 请求头反序列化 JSON → `UserHolder.save(UserBaseDTO)`。`afterCompletion`：`UserHolder.removeUser()` 清理 ThreadLocal |

### 全局异常处理

| 类 | 作用 |
|----|------|
| `handler/GlobalExceptionHandler.java` | `@RestControllerAdvice`，捕获 `BaseException` 及其子类 → 返回 `Result.error(msg)` |

### 分布式锁

| 类 | 作用 |
|----|------|
| `lock/DistributedLockUtil.java` | 基于 Redisson：`tryLock(lockKey, waitTime, leaseTime, supplier)` 标准锁；`executeIfAbsent(idempotentKey, ttl, runnable)` 幂等校验（SETNX 语义） |

### RabbitMQ 基础配置

| 类 | 作用 |
|----|------|
| `config/IMCommonRabbitMQConfig.java` | 声明 `im-topic-exchange`（持久 TopicExchange）+ `Jackson2JsonMessageConverter` + `RabbitAdmin` Bean |

### MinIO 配置

| 类 | 作用 |
|----|------|
| `config/MinIOConfig.java` | 从 `MinIOConfigProperties` 创建 `MinioClient` Bean |
| `config/MinioBucketInit.java` | `@PostConstruct` 检查并创建 7 个桶，设置公共读策略 |

### 文档

| 类 | 作用 |
|----|------|
| `config/Knife4jConfig.java` | `@ConditionalOnClass`，Swagger/OpenAPI 文档配置 |

## 依赖

### 强制依赖（所有引入 common-web 的服务都会获得）

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | Web MVC 基础设施（拦截器、异常处理、嵌入式 Tomcat） |
| `common-model` | Result/DTO/VO 数据模型 |
| `common-redis`（传递 common-base） | Redis 常量、JWT 属性、UserHolder、JwtUtil |
| `knife4j-openapi3-jakarta-spring-boot-starter` | API 文档（`@ConditionalOnClass`） |
| `fastjson` | JSON 序列化 |
| `hutool-all` | 通用工具类 |

### 可选依赖（`<optional>true</optional>`，不传递，需服务显式引入）

| 依赖 | 激活条件 | 激活的 Bean |
|------|---------|-------------|
| `common-storage` | 服务引入 `common-storage` | `MinIOConfig`（MinioClient Bean）、`MinioBucketInit`（桶初始化） |
| `spring-boot-starter-amqp` | 服务引入 `spring-boot-starter-amqp` | `IMCommonRabbitMQConfig`（TopicExchange + MessageConverter + RabbitAdmin） |
| `redisson-spring-boot-starter` | 服务引入 `redisson-spring-boot-starter` | `DistributedLockUtil`（tryLock / executeIfAbsent） |

### Bean 激活机制

`MinIOConfig` 和 `MinioBucketInit` 已有 `@ConditionalOnClass(MinioClient.class)`，仅当 MinIO 在 classpath 上时才生效。
`IMCommonRabbitMQConfig` 有 `@ConditionalOnClass(AmqpTemplate.class)`，仅当 RabbitMQ 在 classpath 上时才生效。
`DistributedLockUtil` 有 `@ConditionalOnClass(RedissonClient.class)`，仅当 Redisson 在 classpath 上时才生效。
