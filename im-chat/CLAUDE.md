# CLAUDE.md — im-chat

## 模块职责

聊天/会话/消息微服务，端口 **8082**，Nacos 注册名 `im-chat`。负责消息持久化、会话管理、AI 聊天、文件分片上传、红包。

## 技术标签

- 文件分片上传与断点续传（雪花 ID 凭证 + MD5 校验 + ZSet 分块索引 + MinIO 服务端合并）
- SSE 服务端推送流式 AI 对话（Spring AI + 智谱 GLM / Ollama 双模型可切换）
- Redis Lua 脚本红包二倍均值法金额分配算法（以分为单位防浮点精度丢失）
- MQ 异步消息持久化（INSERT IGNORE 幂等写入，REST + MQ 双通道防丢失）
- 群聊完整管理模型（邀请/踢人/禁言/设置管理员/转让群主/解散群聊）
- 消息撤回（硬删除）与历史清空

> 我设计并实现了一个覆盖消息、会话、AI、文件、红包的综合业务微服务，通过文件分片 MD5 校验 + ZSet 索引 + MinIO composeObject 合并实现大文件断点续传，通过二倍均值法 Lua 脚本实现红包随机金额分配（最后一人兜底），通过 Spring AI 集成多模型实现 SSE 流式对话，通过 MQ 异步 + INSERT IGNORE 实现消息持久化的幂等解耦，本质上是一个**高内聚的即时通讯业务中台**。

## 端点一览

### 消息 `/message`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/message/init/list/{conversationIds}` | 初始化消息列表（最多 100 条/会话） |
| `GET` | `/message/pull/list` | 分页拉取历史消息（按 maxMessageId 偏移，20 条/页） |
| `POST` | `/message/send` | 发送消息 → 写入 DB + 更新会话未读计数 |
| `GET` | `/message/verifyUploadToken/{fileId}` | 获取文件上传凭证（雪花ID + MinIO 路径） |
| `POST` | `/message/uploadChunk` | 上传文件分块（校验 MD5 → MinIO） |
| `GET` | `/message/checkUploaded` | 检查已上传的分块索引 |
| `POST` | `/message/merge` | 合并文件分块 |
| `PUT` | `/message/updateFileSendStatus` | 更新文件发送状态 |
| `DELETE` | `/message/recallMessage` | 撤回消息（硬删除） |
| `DELETE` | `/message/clearHistoryMessage/{conversationId}` | 清空历史消息 |

### AI 聊天 `/ai-message`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/ai-message/loadMessage` | 加载历史 AI 消息（最近 20 条） |
| `POST` | `/ai-message/sendMessage` | 发送 AI 消息 → **SSE 流式响应** |
| `POST` | `/ai-message/sendImageMessage` | 发送 AI 图片消息（**未实现**） |
| `POST` | `/ai-message/createPersonality` | 创建 AI 个性配置（含头像上传 MinIO） |
| `POST` | `/ai-message/updatePersonality` | 更新 AI 个性内容 |
| `DELETE` | `/ai-message/deletePersonality/{id}` | 删除 AI 个性 |
| `POST` | `/ai-message/switchPersonality/{id}` | 切换激活的 AI 个性 |
| `GET` | `/ai-message/listPersonality` | 获取 AI 个性列表 |

### 会话 `/conversation`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/conversation/init/list` | 初始化会话列表（含用户/群/AI） |
| `GET` | `/conversation/groupMemberList/{conversationId}` | 获取群成员列表 |
| `PUT` | `/conversation/isReaded/{conversationId}` | 清除未读计数 |
| `POST` | `/conversation/updateTopStatus` | 更新置顶状态 |
| `POST` | `/conversation/updateMuteStatus` | 更新免打扰 |
| `DELETE` | `/conversation/delete` | 删除会话 |
| `DELETE` | `/conversation/exitGroup` | 退出群聊 |
| `POST` | `/conversation/create` | 创建单聊会话 |
| `POST` | `/conversation/createGroup` | 创建群聊（含头像上传） |
| `POST` | `/conversation/inviteFriend` | 邀请好友入群 |
| `POST` | `/conversation/updateGroupInfo` | 更新群信息 |
| `POST` | `/conversation/internal/updateGroupAvatar` | 内部更新群头像 |
| `GET` | `/conversation/query` | 查询会话详情 |
| `GET` | `/conversation/groupDetail/{conversationId}` | 查询群详情 |
| `DELETE` | `/conversation/kickMember` | 踢出群成员 |
| `DELETE` | `/conversation/dissolveGroup` | 解散群聊 |
| `POST` | `/conversation/setAdmin` | 设置/撤销管理员 |
| `POST` | `/conversation/muteMember` | 禁言成员 |
| `POST` | `/conversation/transferOwner` | 转让群主 |
| `POST` | `/conversation/batchInvite` | 批量邀请入群 |

### 红包 `/redPacket`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/redPacket/send` | 发红包（Feign 扣款 → 写 DB → 预热 Redis） |
| `POST` | `/redPacket/grab/{redPacketId}` | 抢红包（Lua 原子 → MQ 异步到账） |
| `GET` | `/redPacket/detail/{redPacketId}` | 红包详情（含领取记录） |

## 关键架构

### 文件分片上传流程

```
1. GET /message/verifyUploadToken/{fileId} → 雪花ID 作凭证 + MinIO 路径 → 存 Redis
2. POST /message/uploadChunk → 校验凭证 → 校验 MD5 → MinIO "file-chunk/{fileHash}/{chunkIndex}"
3. GET /message/checkUploaded → Redis ZSet 查已上传分块索引
4. POST /message/merge → MinIO 合并所有分块 → 清理分块文件
```

Redis Key: `FILE_UPLOAD_VERIFY_KEY + fileId`（String, 凭证），`FILE_CHUNK_INDEX_KEY + fileId`（ZSet, 分块索引）

### 抢红包 Lua 脚本（[grab_red_packet.lua](src/main/resources/lua/grab_red_packet.lua)）

- KEYS[1] = `red_packet:{id}` (Hash: remain_count/amount/total_count/amount/status/type)
- KEYS[2] = `red_packet:{id}:grabbed` (Set: 已抢用户)
- 二倍均值法分配（拼手气） / 均分（普通红包），最后一人领全部剩余
- 金额以**分**为单位避免浮点精度

### AI 聊天

- `ChatClientConfig` 提供两个 `ChatClient` Bean：`ollamaChatClient`（本地 qwen2.5:14b）和 `zhipuChatClient`（云端 glm-5.1）
- 当前 `sendMessage()` 实际使用 `zhipuChatClient`
- `AITools` 只注册一个工具：`getCurrentDateTime()`
- System Prompt 要求简洁口语化回复（1-3 句话）
- **SSE 流式返回**（`text/event-stream`）

### MQ 消息持久化

- `MQMessageListener` 监听 `im-storage-queue`，收到 `ClusterMessageWrapper<MessageDTO>` → `messageService.sendMessage()`
- `INSERT IGNORE` 保证幂等——REST 和 MQ 可能双重调用
- 仅**新插入**的消息才更新会话未读计数

### 消息类型

`msg_type`：1=文本、2=图片、3=语音、4=视频、5=文件、99=系统消息

## 关键类

- [MessageServiceImpl.java](src/main/java/com/zzzlew/server/impl/MessageServiceImpl.java) — 核心消息发送 + 文件处理逻辑
- [AIMessageServiceImpl.java](src/main/java/com/zzzlew/server/impl/AIMessageServiceImpl.java) — AI 对话 + SSE 流式
- [RedPacketServiceImpl.java](src/main/java/com/zzzlew/server/impl/RedPacketServiceImpl.java) — 红包完整逻辑
- [ConvversationImpl.java](src/main/java/com/zzzlew/server/impl/ConversationImpl.java) — 会话/群聊管理
- [MQMessageListener.java](src/main/java/com/zzzlew/listener/MQMessageListener.java) — 异步持久化消费者
- [ChatClientConfig.java](src/main/java/com/zzzlew/config/ChatClientConfig.java) — AI Chat Client Bean
- [SpringBootRabbitMQConfig.java](src/main/java/com/zzzlew/config/SpringBootRabbitMQConfig.java) — 声明存储队列

## 数据库表

`message`、`conversation`、`group_conversation`、`group_member`、`ai_message`、`ai_personality`、`red_packet`、`red_packet_record`

## Feign 依赖

- `PayClient.deduct()` — 红包发前扣款
- `AuthClient.getUserListByIds()` — 批量查询用户信息
- `SocialClient.sendGroupApply()` — 发送群申请

## Maven 依赖

- `common-web`（拦截器、异常处理、Knife4j）
- `common-storage`（MinIO 文件/头像上传）
- `spring-boot-starter-amqp`（MQ 异步持久化）
- `im-api`、`mysql-connector-java`、`mybatis-spring-boot-starter`、`nacos-discovery`
- `spring-ai-starter-model-ollama` + `spring-ai-starter-model-zhipuai`
