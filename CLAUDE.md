# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

仿微信全栈即时通讯系统，基于 Spring Cloud Alibaba 微服务架构。后端通过 Netty WebSocket 实现实时消息推送，通过 RabbitMQ 解耦集群通信和异步持久化。

## 构建与运行

```bash
# 构建全部模块
mvn clean package -DskipTests

# 构建单个模块（含依赖）
mvn clean install -pl im-common -am -DskipTests

# 运行单个微服务（指定 dev 环境）
mvn spring-boot:run -pl im-auth -Dspring-boot.run.profiles=dev
```

- Java 17, Spring Boot 3.2.10, Spring Cloud 2023.0.3
- 没有 Maven Wrapper，需本机安装 Maven
- 本地开发使用 `application-dev.yml`（MySQL/Redis/Nacos 指向 `127.0.0.1`，MinIO/RabbitMQ 指向远程 `39.106.183.13`）
- Docker 部署使用 `application-docker.yml`

## 模块架构

```
im-gateway (8080)  ← Spring Cloud Gateway 入口，全局鉴权过滤器
im-auth    (8081)  ← 用户注册/登录/Token管理
im-chat    (8082)  ← 消息持久化、会话管理、AI聊天、文件上传、红包
im-content (8083)  ← 收藏/笔记 CRUD
im-social  (8084)  ← 好友管理、好友/群组申请
im-moments (8085)  ← 朋友圈（帖子/点赞/评论/打赏），Canal 缓存同步
im-pay     (8086)  ← 钱包管理、余额扣减（红包/打赏）
im-server  (8000)  ← Netty WebSocket 服务端，实时消息收发
im-api     (JAR)   ← Feign 客户端接口库，供跨服务 REST 调用
im-common  (POM)   ← 共享库（6个子模块，见下）
```

## im-common 共享库依赖链

```
common-model        ← Result/DTO/VO/ClusterMessageWrapper（无内部依赖，仅 fastjson + jackson-annotations）
  └ common-base     ← 常量/枚举/异常/UserHolder(ThreadLocal)/JwtUtil（依赖 model，不含重量级框架）
      └ common-redis ← RedisConstant/JwtProperties/CacheClient（依赖 base + spring-boot-starter-data-redis）
common-storage      ← MinIO 文件存储工具类（独立，仅依赖 spring-web + minio，无 Tomcat）
common-web          ← WebMvcConfig/UserInfoInterceptor/GlobalExceptionHandler/Knife4jConfig
                      （强制：spring-boot-starter-web + model + redis）
                      （可选：common-storage / spring-boot-starter-amqp / redisson，@ConditionalOnClass 激活）
common-core         ← 聚合模块，无代码，一键引入 model+base+redis（已移除 common-storage）
```

- 需要常量和异常的模块引入 `common-base`
- 需要 ThreadLocal 用户上下文 + 全局异常处理的 REST 服务引入 `common-web`
- 需要 MinIO 文件存储的模块**显式**引入 `common-storage`
- 需要分布式锁的模块**显式**引入 `redisson-spring-boot-starter`（父 POM 管理版本）
- 需要 RabbitMQ 的模块**显式**引入 `spring-boot-starter-amqp`

## 核心架构模式

### 1. 用户上下文传递链路

```
Gateway AuthGlobalFilter → 校验JWT → 查Redis获取用户信息
  → 序列化为JSON放入 user-info HTTP Header
  → 下游服务的 UserInfoInterceptor → 解析存入 UserHolder(ThreadLocal)
  → Feign RequestInterceptor 自动携带 user-info 头传递到再下游
```

- Gateway 使用 Reactive Redis（Lettuce），`AuthProperties` 控制排除路径
- `UserHolder` 在 `common-base`，请求结束由拦截器 `afterCompletion` 清理

### 2. Netty 集群消息路由（im-server）

- 每个 `im-server` 实例有自己的 RabbitMQ 队列 `im-push-queue-{clusterId}`
- Exchange: `im-topic-exchange`，Routing Key: `im.push.{clusterId}`
- `MQMessagePublish.sendToCluster()` 的三向路由逻辑：
  1. **本地**：接收者在同一实例 → 直接通过 `Channel.writeAndFlush()` 发送
  2. **远程**：接收者在其他集群 → 通过 RabbitMQ 投递到对应队列
  3. **离线**：接收者不在线 → Redis Lua 脚本原子存储离线消息标记
- `ChannelManageUtil` 维护 `ConcurrentHashMap<Long, Channel>` 内存映射
- 消息通过 `CompletableFuture.runAsync()` 异步投递，释放 Netty 线程

### 3. 消息处理器策略模式（im-server）

`MessageDispatcherHandler` 维护 `Map<Integer, MessageHandler>`，根据消息类型码路由到对应的 `MessageHandler` 实现（PrivateChat、GroupChat、SystemMessage、好友申请等）。新增消息类型只需添加新的 Handler 实现类。

### 4. 自定义 Netty 协议

```
| 魔数(4B) | 版本(1B) | 序列化算法(1B) | 消息类型(1B) | 序列号(4B) | 填充(1B) | 内容长度(4B) | 正文(NB) |
```

- `ProtocolFrameDecoder` 继承 `LengthFieldBasedFrameDecoder` 解决粘包半包
- `MessageCodecSharable` 策略模式支持 JSON/Java/Kryo/Protobuf 序列化切换

### 5. 分布式锁与幂等

`common-web` 提供 `DistributedLockUtil`（基于 Redisson）：
- `tryLock(lockKey, waitTime, leaseTime, supplier)` — 标准分布式锁
- `executeIfAbsent(idempotentKey, ttl, runnable)` — SetNX 幂等校验

### 6. 缓存与数据一致性

- **Canal**: `im-moments` 通过 Canal 监听 MySQL binlog，自动更新 Redis 缓存
- **Lua 脚本**: 登录 Token 清理/存储、离线消息标记均使用 Lua 保证原子性
- **异步持久化**: `im-chat` 通过 RabbitMQ `im-storage-queue` 异步存储消息，避免阻塞 Netty

## RabbitMQ 队列结构

所有队列绑定到 Topic Exchange `im-topic-exchange`：

| 队列                        | 用途           | 消费者     |
| --------------------------- | -------------- | ---------- |
| `im-push-queue-{clusterId}` | 集群间消息推送 | im-server  |
| `im-storage-queue`          | 异步消息持久化 | im-chat    |
| `im-reward-create-queue`    | 打赏/扣款请求  | im-pay     |
| `im-reward-result-queue`    | 打赏结果回调   | im-moments |
| `im-redpacket-grab-queue`   | 红包抢单       | im-pay     |

## 数据库

数据库名 `zzz-im-server`，表随业务模块分布：`user_info`/`user_auth`（im-auth）、`message`/`conversation`/`group_conversation`/`red_packet`（im-chat）、`friend_relation`/`friend_apply`（im-social）、`moments`/`moment_comments`（im-moments）、`favorites`（im-content）、`wallet`/`wallet_record`（im-pay）。

## 认证体系

双 Token 无感刷新：Access Token（短期，1小时）+ Refresh Token（长期，1年）。Token 存储在 Redis，登录时通过 Lua 脚本原子清理旧 Token 并写入新 Token，避免单用户高并发下的竞态条件。

## 依赖的外部服务

- Nacos（注册中心/配置中心）: `127.0.0.1:8848`（本地）
- MySQL: `127.0.0.1:3306`（本地）
- Redis: `39.106.183.13:6379`（远程，dev环境）
- RabbitMQ: `39.106.183.13:5672`（远程，dev环境）
- MinIO: `39.106.183.13:9000`（远程，dev环境）
- Canal: 监听 MySQL binlog，用于 im-moments 缓存同步

## 架构约束

以下规则在**任何模块重构、依赖调整、新增模块**时必须遵守，违反会导致运行时问题。

### 依赖方向约束

```
im-api / im-common  ←  被依赖层（不可依赖业务模块）
       ↑
  业务模块 (im-auth, im-chat, im-content, im-social, im-moments, im-pay, im-server, im-gateway)
```

- **im-common 子模块之间**：`common-model` ← `common-base` ← `common-redis` ← `common-web`，单向不可逆
- **im-api** 只依赖 `common-model` + `common-base`，不依赖 `common-web` 或任何业务模块
- **业务模块之间** 只能通过 Feign（im-api）互相调用，不可直接依赖对方的 Maven 坐标

### common-web 使用边界

| 模块类型 | 应依赖 | 不应依赖 | 原因 |
|---------|--------|---------|------|
| REST 微服务（im-auth/chat/content/social/moments/pay） | `common-web` ✅ | — | 需要 WebMvcConfig、拦截器、全局异常处理 |
| Netty 服务（im-server） | `common-model` + `common-base` + `common-redis` | `common-web` ❌ | 会引入嵌入式 Tomcat，与 Netty 冲突 |
| Gateway（im-gateway） | `common-model` + `common-redis` | `common-web` ❌ | WebFlux 与 Spring MVC 互斥 |

### im-common 子模块选择指南

| 需要的能力 | 引入的模块 | 注意 |
|-----------|-----------|------|
| Result / DTO / VO | `common-model` | 无 Spring 依赖，可被任何模块使用 |
| 常量 / 枚举 / 异常 / UserHolder / JwtUtil | `common-base` | 传递引入 common-model |
| Redis 缓存 / JWT 属性 | `common-redis` | 传递引入 common-base + common-model |
| WebMvcConfig / 拦截器 / 全局异常处理 | `common-web` | ⚠️ 会引入 Tomcat，仅 REST 服务使用 |
| MinIO 文件存储 | `common-storage` | 独立模块，按需显式引入 |
| 分布式锁（Redisson） | `redisson-spring-boot-starter` | 按需显式引入，父 POM 统一管理版本 3.27.2 |
| RabbitMQ | `spring-boot-starter-amqp` | 按需显式引入，会激活 common-web 中的 `IMCommonRabbitMQConfig` |

**原则**：只引入你真正需要的。多引入一个 `common-web` 就会拉进 Tomcat + Knife4j + Hutool + Fastjson。

## 已知技术债务

以下是最近一次架构审查（2026-06-23）发现的问题。已修复的标记为 ✅，未修复的待后续重构时处理。

### ✅ 已修复（2026-06-23）

| # | 问题 | 修复内容 |
|---|------|---------|
| 1 | `UserBaseDTO` 重复 | 删除 `im-server/.../domain/UserBaseDTO.java`，4 处 import 改为 `com.zzzlew.domain.dto.UserBaseDTO`（common-model） |
| 2 | `ConversationVO` 重复 | 删除 `im-social/.../domain/vo/ConversationVO.java`，包名相同无需改 import，自动走 common-model 传递依赖 |
| 3 | Redisson 版本不一致 | `im-server/pom.xml` 移除硬编码版本号，改为继承父 POM 管理的 3.27.2 |
| 4 | 6 个模块缺少 `spring-boot-maven-plugin` | `im-auth/chat/content/social/moments/pay` 的 pom.xml 添加 `<build>` 段，`spring-boot:repackage` 目标已验证通过 |

### 🟢 低危：冗余显式依赖

| 模块 | 冗余依赖 | 原因 |
|------|---------|------|
| im-gateway | `jaxb-api:2.3.1` | 已通过 `common-redis → common-base` 传递引入，版本相同，重复声明无害 |

### 🟢 低危：多余的 @ConditionalOnClass

`common-web` 中的 `Knife4jConfig.java` 使用了 `@ConditionalOnClass(GroupedOpenApi.class)`，但 knife4j 是 common-web 的强制依赖（非 optional），该条件注解永远为 true。无害，但语义不正确。

## 架构变更审查清单

当进行以下任何一种操作时，在提交前逐项确认：

### 新建模块时
- [ ] 只引入了实际需要的 im-common 子模块（而非整个 common-web 或 common-core）
- [ ] 如果是非 REST 模块，确认没有传递引入 `spring-boot-starter-web`（Tomcat）
- [ ] 显式声明了 `spring-boot-starter-amqp` / `redisson-spring-boot-starter` / `common-storage`（而非依赖 common-web 的可选传递）
- [ ] 版本号跟随父 POM 管理，不在子模块硬编码（除非有充分理由）
- [ ] 添加了 `spring-boot-maven-plugin`（如果是可运行服务）

### 调整依赖关系时
- [ ] 改动不破坏依赖方向：`im-common/im-api ← 业务模块`
- [ ] 没有出现循环依赖
- [ ] 没有业务模块直接依赖另一个业务模块的 Maven 坐标
- [ ] 运行 `mvn dependency:tree -pl <changed-module>` 确认无意外传递依赖

### 添加/移动 Domain 类时
- [ ] 跨模块共享的 DTO/VO/Entity 放在 `common-model`
- [ ] 仅单模块使用的 DTO/VO/Entity 保留在业务模块内
- [ ] **搜索全项目**确认没有同名类已在其他模块存在（避免重复）
- [ ] 如果是将类迁移到 common-model，**删除所有模块中的旧副本**并更新所有 import

### 重构现有模块时
- [ ] 处理了"已知技术债务"中相关的条目
- [ ] 重构后运行 `mvn clean compile -pl <module> -am` 确认编译通过
- [ ] 如果涉及 im-common 子模块改动，运行全量编译 `mvn clean compile`（所有模块都可能受影响）

各模块有独立的 CLAUDE.md，深入描述模块内部的端点、类、架构细节：

| 模块           | 指南                                            | 说明                                            |
| -------------- | ----------------------------------------------- | ----------------------------------------------- |
| im-gateway     | [CLAUDE.md](im-gateway/CLAUDE.md)               | Gateway 鉴权过滤器、路由表、Reactive Redis      |
| im-auth        | [CLAUDE.md](im-auth/CLAUDE.md)                  | Token 体系、Lua 原子登录、限流 AOP              |
| im-chat        | [CLAUDE.md](im-chat/CLAUDE.md)                  | 消息/会话/AI/文件分片/红包，MQ 异步持久化       |
| im-content     | [CLAUDE.md](im-content/CLAUDE.md)               | 收藏/笔记 CRUD                                  |
| im-social      | [CLAUDE.md](im-social/CLAUDE.md)                | 好友管理、申请处理、双向操作                    |
| im-moments     | [CLAUDE.md](im-moments/CLAUDE.md)               | 朋友圈缓存策略、Lua 点赞、Canal 一致性、打赏    |
| im-pay         | [CLAUDE.md](im-pay/CLAUDE.md)                   | 钱包扣款、三层锁策略、MQ 转账                   |
| im-server      | [CLAUDE.md](im-server/CLAUDE.md)                | Netty Pipeline、集群路由、在线/离线、自定义协议 |
| im-api         | [CLAUDE.md](im-api/CLAUDE.md)                   | Feign 客户端接口库、用户上下文传播              |
| common-model   | [CLAUDE.md](im-common/common-model/CLAUDE.md)   | Result/DTO/VO/ClusterMessageWrapper             |
| common-base    | [CLAUDE.md](im-common/common-base/CLAUDE.md)    | 常量/枚举/异常/UserHolder/JwtUtil               |
| common-redis   | [CLAUDE.md](im-common/common-redis/CLAUDE.md)   | Redis 全部 Key 常量/JwtProperties               |
| common-storage | [CLAUDE.md](im-common/common-storage/CLAUDE.md) | MinIO 文件存储工具                              |
| common-web     | [CLAUDE.md](im-common/common-web/CLAUDE.md)     | 拦截器/异常处理/分布式锁/RabbitMQ/MinIO 配置    |
| common-core    | [CLAUDE.md](im-common/common-core/CLAUDE.md)    | 聚合模块（无代码）                              |
