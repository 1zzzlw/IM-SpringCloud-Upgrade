# CLAUDE.md — im-server

## 模块职责

Netty WebSocket 服务端，**不是 REST 服务**——没有 HTTP Controller。负责实时消息收发、集群路由、在线状态管理、离线消息存储。默认 WebSocket 端口 **8000**，集群 ID 从 `ws.mq` JVM 系统属性或 `netty.websocket.cluster-id` 配置读取。

## 自定义协议帧（16 字节头部）

```
| 魔数(4B) | 版本(1B) | 序列化算法(1B) | 消息类型(1B) | 序列号(4B) | 填充(1B) | 内容长度(4B) | 正文(NB) |
```

- 魔数：`[1, 2, 3, 4]`
- `ProtocolFrameDecoder`：继承 `LengthFieldBasedFrameDecoder`（maxFrameLength=128KB, offset=12, lengthFieldLength=4）
- `MessageCodecSharable`：`MessageToMessageCodec<ByteBuf, Message>`，策略模式支持 JSON/Java 序列化（`Serializer.Algorithm` 枚举）

## Netty Pipeline（按顺序）

| # | Handler | 作用 |
|---|---------|------|
| 1 | `HttpServerCodec` | HTTP 编解码（WebSocket 握手） |
| 2 | `HttpObjectAggregator(128KB)` | 聚合 HTTP 分块 |
| 3 | `ChunkedWriteHandler` | 分块写入 |
| 4 | `HttpHeadersHandler` | 从 URI QueryString 提取 JWT → 校验 → `ChannelManageUtil` 注册 |
| 5 | `WebSocketServerProtocolHandler("/ws")` | WebSocket 升级 + ping/pong |
| 6 | `BinaryWebSocketFrameToByteBufHandler` | 入站：`BinaryWebSocketFrame` → `ByteBuf` |
| 7 | `ProtocolFrameDecoder` | 入站：粘包/半包处理 |
| 8 | `ByteBufToBinaryWebSocketFrameHandler` | 出站：`ByteBuf` → `BinaryWebSocketFrame` |
| 9 | `MessageCodecSharable` | 编解码：`ByteBuf` ↔ `Message` |
| 10 | `MessageDispatcherHandler` | **核心调度**：按 type 路由到 `MessageHandler` |
| 11 | `IdleStateHandler(90s)` | 90s 读空闲 → 触发心跳断开 |
| 12 | `HeartBeatHandler` | `READER_IDLE` 事件 → 关闭 Channel |
| 13 | `ConnectSuccessMessageHandler` | `HandshakeComplete` → 推送离线消息 + 通知在线 |
| 14 | `QuitLoginHandler` | `channelInactive` → 通知离线 + 清理映射 |

## 消息类型与 Handler 映射（`MessageDispatcherHandler`）

| type | 请求类 | Handler |
|------|--------|---------|
| 0 | HeartRequestDTO | 直接处理（刷新 Redis TTL） |
| 1 | PrivateChatRequestDTO | `PrivateChatHandler` → ACK + 单发 |
| 3 | GroupChatRequestDTO | `GroupChatHandler` → ACK + 群发 |
| 5 | FriendApplyRequestDTO | `FriendApplySendHandler` |
| 7 | GroupApplyRequestDTO | `GroupApplySendHandler` |
| 12 | SystemMessageRequestDTO | `SystemMessageHandler` → ACK + 群发 |
| 14 | FriendApplyDealRequestDTO | `FriendApplyDealHandler` |
| 17 | GroupApplyDealRequestDTO | `GroupApplyDealHandler` |

所有 Handler 返回 `MessageResult`（`response` + `receiverIds`），由 `MQMessagePublish.sendToCluster()` 分发。

## 集群消息路由（`MQMessagePublish`）

```
sendToCluster(MessageResult) 异步执行：
  1. Redis 批量查 receiverIds → serverId（netty:websocket:cluster:info Hash）
  2. 三向路由：
     - serverId 为空 → 离线：Lua 脚本存 Redis Sorted Set（TTL 20s）
     - serverId == 当前集群 → 本地：Channel.writeAndFlush() → 失败降级 MQ
     - serverId != 当前集群 → 远程：RabbitMQ(im.push.{serverId})
  3. 同时发持久化副本到 MQ(im.storage)
```

RabbitMQ Exchange: `im-topic-exchange`，每个 im-server 实例声明自己的持久队列 `im-push-queue-{clusterId}`。

## 在线/离线生命周期

### 上线（`ConnectSuccessMessageHandler.HandshakeComplete`）

1. 推送离线消息（Redis ZSet `user:offline:message:content:{userId}`，按 score 排序）
2. 通知所有好友 "xxx 上线"（MQ 集群广播）
3. 查询在线好友列表（Redis Set `user:online:status` ∩ 好友列表）
4. 加入在线状态集 + 写 `netty:websocket:cluster:info`（userId → clusterId）

### 离线（`QuitLoginHandler.channelInactive`）

1. 通知所有好友 "xxx 下线"（MQ 集群广播）
2. 记录离线时间到 Redis Hash `user:offline:quitTime`
3. 移除在线状态 + 删除集群映射 + 清理 `ChannelManageUtil`

## 离线消息存储

[store_offline_message.lua](src/main/resources/store_offline_message.lua)：ZADD 消息 JSON 到 `user:offline:message:content:{userId}`，score = 雪花 ID（保持发送顺序），TTL = 20s。

## 关键类

- [NettyWebSocketServer.java](src/main/java/com/zzzlew/websocket/NettyWebSocketServer.java) — 启动/关闭 Netty，声明 MQ 队列
- [MessageDispatcherHandler.java](src/main/java/com/zzzlew/handler/MessageDispatcherHandler.java) — 消息路由调度中心
- [MQMessagePublish.java](src/main/java/com/zzzlew/publish/MQMessagePublish.java) — 集群路由 + 离线存储
- [MQMessageListener.java](src/main/java/com/zzzlew/listener/MQMessageListener.java) — 接收远程集群消息
- [ChannelManageUtil.java](src/main/java/com/zzzlew/utils/ChannelManageUtil.java) — `Online_user: ConcurrentHashMap<Long, Channel>` + 反向映射
- [MessageCodecSharable.java](src/main/java/com/zzzlew/protocol/MessageCodecSharable.java) — 编解码器
- [Serializer.java](src/main/java/com/zzzlew/protocol/Serializer.java) — JSON/Java 可切换序列化
- [Message.java](src/main/java/com/zzzlew/domain/Message.java) — 消息基类（含 type→class 映射）
- [NettyConfig.java](src/main/java/com/zzzlew/config/NettyConfig.java) — 集群 ID/端口解析 + `getClusterQueueName()` Bean
- [NettyPlatformOptimizer.java](src/main/java/com/zzzlew/utils/NettyPlatformOptimizer.java) — Linux(Epoll)/Windows(NIO) 自动适配
- [ThreadPoolConfig.java](src/main/java/com/zzzlew/config/ThreadPoolConfig.java) — `imAsyncExecutor` 线程池

## Docker 部署

[Dockerfile](Dockerfile)：`openjdk:17-jdk-slim`，通过环境变量 `WS_MQ`、`WS_PORT` 指定集群 ID 和端口，启动 profile=docker。

## 依赖

- `common-model`（ClusterMessageWrapper、UserBaseDTO）
- `common-base`（JwtUtil、RabbitMQConstant）
- `common-redis`（RedisConstant）
- `netty-all` 4.1.94.Final
- `redisson-spring-boot-starter`（版本由父 POM 统一管理）
- `spring-boot-starter-amqp`（RabbitMQ 集群通信）
