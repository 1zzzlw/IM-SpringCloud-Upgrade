# CLAUDE.md — common-redis

## 职责

Redis 相关配置、常量、工具。依赖 `common-base`。

## 关键类

### `constant/RedisConstant.java`

定义**全部** Redis Key 模板和 TTL。核心 Key 包括：

| Key 模式 | 用途 | 使用者 |
|----------|------|--------|
| `login:code:{code}` | 图形验证码（5min TTL） | im-auth |
| `register:code:{phone}` | 注册手机验证码 | im-auth |
| `login:user:tokenList:{userId}` | 用户活跃 Token SET | im-auth, im-gateway |
| `login:user:Info:accessToken:{token}` | Access Token Hash（用户信息） | im-auth, im-gateway |
| `login:user:Info:refreshToken:{token}` | Refresh Token Hash | im-auth, im-gateway |
| `login:user:friendList:{userId}` | 好友 ID SET | im-auth, im-social |
| `user:friend:list:{userId}` | 好友列表 | im-social |
| `user:online:status` | 在线用户 SET | im-server, im-social |
| `user:offline:message:content:{userId}` | 离线消息 Sorted Set | im-server |
| `user:offline:quitTime` | 离线时间戳 Hash | im-server, im-social |
| `netty:websocket:cluster:info` | userId→clusterId Hash | im-server |
| `moments:list:info:{id}` | 帖子详情 String | im-moments |
| `moments:list:new` / `moments:list:hot` | 帖子 ZSet | im-moments |
| `moments:count:{id}` | 帖子点赞/评论计数 Hash | im-moments |
| `moments:like:{id}` | 帖子点赞 SET | im-moments |
| `moments:comment:like:{id}` | 评论点赞 SET | im-moments |
| `red_packet:{id}` | 红包 Hash | im-chat |
| `red_packet:{id}:grabbed` | 已抢红包用户 SET | im-chat |
| `FILE_UPLOAD_VERIFY_KEY + ...` | 文件上传凭证 | im-chat |
| `FILE_CHUNK_INDEX_KEY + ...` | 分块索引 ZSet | im-chat |

### `properties/JwtProperties.java`

`@ConfigurationProperties(prefix = "jwt")`：access/fresh 密钥、过期时间、Token 名称。

### `utils/CacheClient.java`

Spring Bean，注入 `StringRedisTemplate`，骨架代码（待实现缓存方法）。

## 依赖

- `common-base`
- `spring-boot-starter-data-redis`
