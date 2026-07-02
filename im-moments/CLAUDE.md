# CLAUDE.md — im-moments

## 模块职责

朋友圈（Moments）微服务，端口 **8085**，Nacos 注册名 `im-moments`。负责帖子发布、点赞、评论/回复、打赏、缓存管理、Canal 缓存一致性。

## 技术标签

- Redis 多层级缓存架构（ZSet 时间线 + String 详情 + Hash 计数 + Set 点赞集合，四种数据结构协同一体）
- 4 个 Lua 脚本原子操作（点赞 SISMEMBER/SADD/SREM 切换 + HINCRBY 计数 + ZADD 热榜 + 缓存预热 SETNX）
- 游标分页（lastId 偏移，解决传统分页在实时新增场景下的数据重复问题）
- Canal + MySQL binlog 异步缓存失效（监听 UPDATE 事件 → DEL Redis 缓存，最终一致性）
- Cache-Aside 缓存模式（读未命中 → 查 DB → 回写缓存 + TTL 随机化 ±10% 防雪崩）
- 评论二级嵌套结构（parent_id 区分顶级评论与回复，两级加载策略）
- 打赏 MQ 异步解耦（发 MQ → im-pay 处理 → MQ 回调结果，幂等键防重）

> 我设计并实现了一套基于 "Cache-Aside + Canal binlog 异步双写" 的朋友圈信息流系统，通过多层级 Redis 数据结构（ZSet 时间线 + Hash 计数 + Set 点赞集合）承载列表/详情/计数/点赞四种缓存形态，通过 4 个 Lua 脚本实现点赞切换、评论计数、缓存预热等原子操作消除并发竞态，通过 TTL 随机化 ±10% 防止缓存雪崩，通过 Canal 监听 MySQL binlog 实现异步缓存失效保证最终一致性，通过游标分页解决实时新增下的分页偏移问题，本质上是一个**高并发、最终一致的社交信息流微服务**。

## 端点一览（`/moments`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/moments/uploadImage` | 上传帖子图片到 MinIO `zzz-im-moment` 桶 |
| `POST` | `/moments/publish` | 发布帖子 → 写 DB + 加 ZSet `moments:list:new` |
| `GET` | `/moments/list/new?lastId=` | **游标分页**加载最新帖子（20 条/次） |
| `GET` | `/moments/list/hot?page=&pageSize=` | 分页加载最热帖子（按 like_count DESC） |
| `POST` | `/moments/like/{momentId}` | 点赞/取消点赞 `@Transactional`（Lua 原子切换） |
| `GET` | `/moments/detail/{momentId}` | 帖子详情（Cache-Aside） |
| `GET` | `/moments/comments/query` | 分页查询顶级评论（like_count DESC） |
| `GET` | `/moments/comment/reply/{commentId}` | 分页查询回复（publish_time ASC） |
| `POST` | `/moments/comment/publish` | 发布顶级评论 `@Transactional` |
| `POST` | `/moments/comment/reply/publish` | 发布回复 `@Transactional` |
| `POST` | `/moments/comment/like/{commentId}` | 评论点赞/取消（Lua 切换） |
| `GET` | `/moments/my?page=&pageSize=` | 我的帖子 |
| `DELETE` | `/moments/delete/{momentId}` | 删除帖子（DB 硬删除 + Redis 清理）`@Transactional` |
| `PUT` | `/moments/update` | 编辑帖子内容（仅改 DB，Canal 删 Redis 缓存） |
| `POST` | `/moments/reward?momentId=&amount=` | 打赏帖子 → 发 MQ 到 `im.reward.create` |
| `GET` | `/moments/search?keyword=&page=&pageSize=` | 搜索帖子（**stub，未实现**） |

## Redis 缓存结构

| 数据结构 | Key | 说明 |
|----------|-----|------|
| String | `moments:list:info:{id}` | 帖子 JSON 详情（24h + 随机 TTL） |
| ZSet | `moments:list:new` | 最新列表（score=postId，最多 2000 条） |
| ZSet | `moments:list:hot` | 热门列表（score=likeCount，缓存缺失时从 DB 重建） |
| Hash | `moments:count:{id}` | `like` + `comment` 计数（24h + 随机 TTL） |
| Set | `moments:like:{id}` | 点赞用户 ID 集合（永久） |
| Set | `moments:comment:like:{id}` | 评论点赞用户 ID 集合（永久） |

**TTL 随机 ±10%** 防止缓存雪崩。

## 4 个 Lua 脚本（[src/main/resources/lua/](src/main/resources/lua/)）

| 脚本 | 功能 |
|------|------|
| `moments_like.lua` | 原子点赞切换：SISMEMBER/SADD/SREM + HINCRBY + ZADD 热榜更新 |
| `moments_comment_like.lua` | 评论点赞切换（简单 SISMEMBER toggle） |
| `moments_comment_count_incr.lua` | 原子增减评论计数（缓存缺时返 -1，触发 warmup） |
| `moments_warmup_cache.lua` | 缓存预热：SETNX 模式初始化 count Hash |

所有 Lua 脚本都在 [RedisLuaScriptConfig.java](src/main/java/com/zzzlew/config/RedisLuaScriptConfig.java) 注册为 `DefaultRedisScript<Long>` Bean。

## Canal 缓存一致性

- [MomentsHandler.java](src/main/java/com/zzzlew/canal/MomentsHandler.java) — 连接到 `127.0.0.1:11111`（destination: `zzz-im-canal`）
- 订阅 `zzz-im-server\.moments` 表的 binlog
- **仅处理 UPDATE 事件**：提取 `id` → `DEL moments:list:info:{id}`
- 编辑帖子时不改 Redis，由 Canal 异步删缓存。下次读取触发 Cache-Aside 重建。

## 打赏流程

```
im-moments → RabbitMQ(im.reward.create) → im-pay(RewardMessageListener)
        ← RabbitMQ(im.reward.result) ← im-pay(transfer success/failure)
        → RewardsResultListener → 更新 moments.reward_amount
```

- 幂等键：`{fromUserId}_{momentId}_{timestamp}`
- 校验：0.01 ≤ amount ≤ 200，不能给自己打赏

## 关键类

- [MomentsServiceImpl.java](src/main/java/com/zzzlew/server/impl/MomentsServiceImpl.java) — **核心**，所有缓存/点赞/评论/打赏逻辑
- [MomentsController.java](src/main/java/com/zzzlew/controller/MomentsController.java) — 所有 REST 端点
- [MomentsHandler.java](src/main/java/com/zzzlew/canal/MomentsHandler.java) — Canal binlog 消费者
- [RewardResultListener.java](src/main/java/com/zzzlew/listener/RewardResultListener.java) — 打赏结果回调
- [RedisLuaScriptConfig.java](src/main/java/com/zzzlew/config/RedisLuaScriptConfig.java) — Lua 脚本 Bean 注册
- [RewardResultQueueConfig.java](src/main/java/com/zzzlew/config/RewardResultQueueConfig.java) — 声明结果队列
- [MomentsMapper.xml](src/main/resources/mapper/MomentsMapper.xml) — 全部 SQL

## 数据库表

- `moments`：id, user_id, username, avatar, content, publish_time, like_count, comment_count, reward_amount, is_deleted
- `moment_comments`：id, moment_id, user_id, username, avatar, content, publish_time, parent_id（0=顶级）, reply_to_user_id, reply_to_username, like_count, is_deleted

## 依赖

- `common-web`（UserHolder、MinIOFileStorgeUtil、Result、PageResult）
- `common-storage`（MinIO 朋友圈图片上传）
- `spring-boot-starter-amqp`（打赏结果 MQ 回调）
- `im-api`（Feign 接口）
- `canal.client` + `canal.protocol` 1.1.7
- `pagehelper-spring-boot-starter`
