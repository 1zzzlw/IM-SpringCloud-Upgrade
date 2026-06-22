# CLAUDE.md — im-social

## 模块职责

社交关系微服务，端口 **8084**，Nacos 注册名 `im-social`。负责好友管理、好友申请处理、群聊申请处理。

## 端点一览

### 好友（`/friend`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/friend/init/list?isInit=` | 全量/增量获取好友列表（增量用 Redis 离线时间过滤） |
| `DELETE` | `/friend/delete?friendId=` | 双向删除好友关系 |
| `PUT` | `/friend/remark` | 更新好友备注 |
| `PUT` | `/friend/status` | 更新好友关系状态（0=未同意, 1=正常, 2=拉黑） |

### 申请（`/apply`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/apply/send` | 发送好友申请 |
| `POST` | `/apply/groupApply` | 发送群聊邀请给多个好友 |
| `GET` | `/apply/list` | 获取待处理的好友申请列表 |
| `GET` | `/apply/groupApplyList` | 获取待处理的群聊申请列表 |
| `POST` | `/apply/deal` | 处理好友申请（同意/拒绝）`@Transactional` |
| `POST` | `/apply/groupApply/deal` | 处理群聊申请（含群头像上传 MinIO）`@Transactional` |

## 核心业务逻辑

### 同意好友申请（`ApplyServiceImpl.dealApply`）

1. 标记申请为已处理 `dealApply(dealApplyDTO)`
2. 如果同意：生成确定性 `conversationId` = `{minId}_{maxId}`
3. **Feign** 调用 `chatClient.createConversation()` 创建双方会话
4. 双向写入 `friend_relation` 表
5. 双方好友 ID 存入 Redis SET `user:friend:list:{userId}`
6. 检查申请发送者是否在线（Redis Set `user:online:status`）→ 返回 `isOnline` 标志

### 删除好友（双向删除）

```
friendMapper.deleteFriend(userId, friendId)
friendMapper.deleteFriend(friendId, userId)
```

### 状态更新（双向同步）

```
friendMapper.updateFriendRelationStatus(userId, friendId, status)
friendMapper.updateFriendRelationStatus(friendId, userId, status)
```

## 关键类

- [FriendServiceImpl.java](src/main/java/com/zzzlew/server/impl/FriendServiceImpl.java) — 好友 CRUD，Redis 离线增量过滤
- [ApplyServiceImpl.java](src/main/java/com/zzzlew/server/impl/ApplyServiceImpl.java) — 申请处理 + Feign 调用创建会话
- [FriendMapper.xml](src/main/resources/mapper/FriendMapper.xml) — JOIN `user_info` 查询好友详情
- [ApplyMapper.xml](src/main/resources/mapper/ApplyMapper.xml) — `friend_apply` / `group_apply` 操作

## 数据库表

- `friend_relation`：user_id, friend_id, remark, relation_status（0/1/2）
- `friend_apply`：apply_id, from_user_id, to_user_id, apply_msg, is_dealt, deal_result
- `group_apply`：user_id（群主）, member_id（受邀者）, conversation_id, status

## Feign 依赖

- `ChatClient.createConversation()` — 同意好友申请后创建会话
- `ChatClient.inviteFriends()` — 同意群申请后添加成员
- `ChatClient.updateGroupInfo()` — 更新群头像
- `ChatClient.queryConversation()` — 查询会话详情返回

## Maven 依赖

- `common-web`（拦截器、异常处理、Knife4j）
- `common-storage`（MinIO 群头像上传）
- `im-api`、`mysql-connector-java`、`mybatis-spring-boot-starter`、`nacos-discovery`

## Redis 键

- `user:friend:list:{userId}` — 好友 ID SET（TTL 可配置）
- `user:online:status` — 在线用户 SET（判断实时通知）
- `user:offline:info` — 离线信息 Hash（增量过滤用）
