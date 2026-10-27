# 双 Token 认证体系设计与多端扩展方案

> 项目：IM-SpringCloud（仿微信全栈即时通讯系统）  
> 模块：im-auth（认证服务）、im-gateway（网关）  
> 日期：2026-07-24  
> 作者：zzzlew

---

## 一、概述

本项目采用 **双 Token 认证体系**：短期 Access Token（1小时）+ 长期 Refresh Token（365天），通过 Redis Lua 脚本实现原子化登录，通过 Spring Cloud Gateway 全局过滤器实现无感刷新和用户上下文透传。

本文档从**数据结构设计 → 登录流程 → 网关鉴权 → Token 刷新 → 安全性**五个维度梳理现有实现，并在此基础上给出**多端登录扩展方案**。

---

## 二、Redis 数据结构设计

### 2.1 三层 Key 模型

```
┌─────────────────────────────────────────────────────────────────┐
│  Layer 1: 索引层（SET）                                          │
│  login:user:tokenList:{userId}                                   │
│  存储: {accessToken, refreshToken}                               │
│  作用: 记录用户所有活跃 Token，提供索引，方便批量清理            │
│  TTL:  36000 分钟                                                │
├─────────────────────────────────────────────────────────────────┤
│  Layer 2: 短期鉴权层（HASH）                                      │
│  login:user:Info:accessToken:{accessToken}                       │
│  存储: {id, username, avatar, account, phone, gender}            │
│  作用: 每次请求网关从这里读用户信息 → 透传到下游                 │
│  TTL:  36000 分钟                                                │
├─────────────────────────────────────────────────────────────────┤
│  Layer 3: 长期鉴权层（HASH）                                      │
│  login:user:Info:refreshToken:{refreshToken}                     │
│  存储: {id, username, avatar, account, phone, gender}（同上）     │
│  作用: Token 刷新时从这里读用户信息 → 不需要查 MySQL              │
│  TTL:  36000 分钟                                                │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 设计理由

| 问题 | 解决方案 |
|------|---------|
| 用户重新登录时，如何找到并清理上次登录留下的旧 Token？ | SET `tokenList:{userId}` 作为索引，SMEMBERS 获取旧 Token 列表，拼接出对应的 Hash key 批量 DEL |
| 网关每次请求如何获取用户信息以做上下文透传？ | 用 accessToken 作为 key，从 HASH `accessToken:{token}` 中读出用户信息 → 序列化 JSON → 放入 `user-info` Header |
| Access Token 过期后，用 Refresh Token 刷新时如何拿到用户信息？ | 用 refreshToken 作为 key，从 HASH `refreshToken:{token}` 中读出用户信息 → 用这些信息重新生成 JWT claims，无需查库 |
| 两个 Token 的 HASH 为什么存完全相同的用户信息？ | 解耦：短期 Token 只负责日常鉴权，长期 Token 只负责刷新——各自独立读取，互不依赖 |

---

## 三、登录流程（Token 生成与存储）

### 3.1 整体流程

```
客户端                        im-auth                         Redis
  │                             │                               │
  │  POST /user/login           │                               │
  │  {account, password}        │                               │
  │ ──────────────────────────> │                               │
  │                             │  1. 校验账号密码               │
  │                             │  2. 生成 accessToken（JWT）    │
  │                             │     签名密钥: access-secret-key │
  │                             │     过期时间: 1 小时           │
  │                             │  3. 生成 refreshToken（JWT）   │
  │                             │     签名密钥: fresh-secret-key  │
  │                             │     过期时间: 365 天           │
  │                             │                               │
  │                             │  4. 执行 Lua 原子脚本          │
  │                             │ ─────────────────────────────> │
  │                             │    ① SMEMBERS 旧 Token        │
  │                             │    ② DEL 旧 Hash（access+refresh）
  │                             │    ③ DEL 旧 SET               │
  │                             │    ④ SADD 新 Token 到 SET      │
  │                             │    ⑤ HSET 用户信息到两个 Hash  │
  │                             │    ⑥ EXPIRE 全部 Key          │
  │                             │ <───────────────────────────── │
  │                             │                               │
  │  Response Headers:          │                               │
  │  Authorization: Bearer xxx  │                               │
  │  refreshtoken: xxx          │                               │
  │ <────────────────────────── │                               │
```

### 3.2 Lua 原子脚本详解

位置：[`im-auth/src/main/resources/login_token_atomic_operation.lua`](im-auth/src/main/resources/login_token_atomic_operation.lua)

```lua
-- KEYS[1]: login:user:tokenList:{userId}       -- SET 索引
-- KEYS[2]: login:user:Info:accessToken:{token}  -- Access Token HASH
-- KEYS[3]: login:user:Info:refreshToken:{token} -- Refresh Token HASH

-- 步骤1: 读取旧 Token 列表
local oldTokens = redis.call('SMEMBERS', KEYS[1])

-- 步骤2: 批量删除旧 Token 对应的 HASH
if #oldTokens > 0 then
    local keysToDelete = {}
    for i, token in ipairs(oldTokens) do
        table.insert(keysToDelete, 'login:user:Info:accessToken:' .. token)
        table.insert(keysToDelete, 'login:user:Info:refreshToken:' .. token)
    end
    redis.call('DEL', unpack(keysToDelete))
    redis.call('DEL', KEYS[1])
end

-- 步骤3: 写入新 Token 到 SET
redis.call('SADD', KEYS[1], ARGV[1], ARGV[2])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]) * 60)

-- 步骤4: 解析用户信息 JSON，HSET 到两个 HASH
local userInfo = cjson.decode(ARGV[6])
-- HSET + EXPIRE 到 accessToken HASH
-- HSET + EXPIRE 到 refreshToken HASH

return 1  -- 成功
```

**为什么用 Lua 脚本？**  
Java 侧如果分步执行（SMEMBERS → 循环 DEL → SADD → HSET），在高并发下可能出现竞态：线程A的 SMEMBERS 还没执行完 DEL，线程B又 SMEMBERS 拿到了被A标记为"待删除"的旧 Token。Lua 脚本在 Redis 服务端原子执行，整个操作不可分割，避免了并发安全问题。

### 3.3 Token 刷新流程

```
客户端                  im-gateway                    im-auth                 Redis
  │                         │                            │                      │
  │  GET /user/refresh     │                            │                      │
  │  Authorization:        │                            │                      │
  │    Bearer {refreshToken}│                            │                      │
  │ ──────────────────────> │                            │                      │
  │                         │  isRefreshPath → true      │                      │
  │                         │  用 fresh-secret-key 验签   │                      │
  │                         │ ─────────────────────────────────────────────────> │
  │                         │  HGETALL refreshToken:{token}                      │
  │                         │ <───────────────────────────────────────────────── │
  │                         │  expire refreshToken Hash  │                      │
  │                         │  透传 user-info Header      │                      │
  │                         │ ──────────────────────────> │                      │
  │                         │                            │ pendingLogin()       │
  │                         │                            │ 从 UserHolder 拿用户  │
  │                         │                            │ generateAndStore...()│
  │                         │                            │ ────────────────────>│
  │                         │                            │ Lua 原子脚本（同上）  │
  │                         │                            │ <────────────────────│
  │  Response Headers:      │                            │                      │
  │  Authorization: newAT   │                            │                      │
  │  refreshtoken: newRT    │                            │                      │
  │ <────────────────────── │ <───────────────────────── │                      │
```

---

## 四、网关鉴权体系

### 4.1 过滤器链路

位置：[`im-gateway/src/main/java/com/zzzlew/filters/AuthGlobalFilter.java`](im-gateway/src/main/java/com/zzzlew/filters/AuthGlobalFilter.java)

```
请求进入 AuthGlobalFilter (Order=0)
  │
  ├─ isExclude(path)? ──── 是 ──> 放行（登录/注册/验证码/Swagger...）
  │
  ├─ 提取 Authorization Header 中的 Token
  │
  ├─ isRefreshPath(path)?
  │    │
  │    ├─ 是（长期 Token 路径）
  │    │    ├─ 用 fresh-secret-key 解析 JWT
  │    │    ├─ 从 Redis HASH refreshToken:{token} 读用户信息
  │    │    ├─ 刷新 Redis key TTL
  │    │    └─ passThrough(): 用户信息 JSON → user-info Header → 转发
  │    │
  │    └─ 否（短期 Token 路径）
  │         ├─ 用 access-secret-key 解析 JWT
  │         ├─ 从 Redis HASH accessToken:{token} 读用户信息
  │         ├─ 刷新 Redis key TTL
  │         └─ passThrough(): 用户信息 JSON → user-info Header → 转发
  │
  └─ 异常: Token 为空 / JWT 过期 / Redis 中不存在 → 401/400
```

### 4.2 刷新路径判断

网关通过配置项 `auth.refreshPaths` 区分哪些路径接受 Refresh Token：

```yaml
auth:
  refreshPaths:
    - /user/refreshToken/**
    - /message/verifyUploadToken/**
    - /message/merge
    - /message/init/list/**
    - /conversation/init/list
    - /friend/init/list
```

设计意图：普通业务接口只能用短期 Access Token（1小时过期，泄露影响有限）；只有专门的刷新接口和少量初始化接口才接受长期 Refresh Token。

### 4.3 用户上下文传播链

```
Gateway AuthGlobalFilter
  → 从 Redis HASH 读用户信息
  → JSON 序列化
  → 放入 user-info HTTP Header
  → 转发到下游微服务
      │
      ├─ im-auth / im-chat / im-social / ...
      │   → common-web: UserInfoInterceptor (preHandle)
      │   → 从 user-info Header 反序列化
      │   → 存入 UserHolder (ThreadLocal)
      │   → Controller/Service 通过 UserHolder.getUser() 获取
      │   → afterCompletion: UserHolder.removeUser() 清理
      │
      └─ Feign 跨服务调用
          → RequestInterceptor 自动携带 user-info Header
          → 下游服务同样通过拦截器解析
```

---

## 五、用户上下文透传机制

### 5.1 设计动机

在微服务架构中，用户信息需要在服务间传递。常见的方案对比：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 每个服务独立解析 JWT | 去中心化，无需网络调用 | JWT 无法主动失效；每个服务需持有密钥；claims 内信息有限 |
| 共享 Redis 读取 | 可主动失效；信息完整 | 每次请求需网络 IO |
| 网关透传（本项目方案） | Redis 只读一次；下游零开销 | Header 有大小限制；下游需信任网关 |

本项目选择**网关透传**方案：网关从 Redis 中读出用户信息后，序列化为 JSON 放入 HTTP Header，下游服务直接解析 Header 即可获得用户信息，不需要再次查 Redis 或解析 JWT。

### 5.2 实现细节

**网关侧** — [`AuthGlobalFilter.passThrough()`](im-gateway/src/main/java/com/zzzlew/filters/AuthGlobalFilter.java#L174-L180)：
```java
private Mono<Void> passThrough(ServerWebExchange exchange, GatewayFilterChain chain,
                               Map<Object, Object> userMap) {
    String userInfoJson = JSONUtil.toJsonStr(userMap);
    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header("user-info", userInfoJson)
            .build();
    return chain.filter(exchange.mutate().request(modifiedRequest).build());
}
```

**下游拦截器侧** — `common-web` 模块的 `UserInfoInterceptor`（所有 REST 服务自动装配）：
```java
// preHandle: 
//   从 request.getHeader("user-info") 获取 JSON
//   反序列化为 UserBaseDTO
//   UserHolder.save(userBaseDTO)

// afterCompletion:
//   UserHolder.removeUser()  // 防止内存泄漏
```

**Feign 传播** — `common-web` 模块的 `RequestInterceptor`：
```java
// 从 UserHolder 获取当前用户信息
// 序列化后设置到下游请求的 user-info Header
// 确保跨服务 Feign 调用时用户上下文不丢失
```

### 5.3 为什么不直接用 JWT Claims 透传？

1. **信息完整性**：JWT Claims 中只存储了有限的字段（id, username 等），而 Redis HASH 可以存储完整的用户信息（phone, gender, avatar 等），且可以随时扩展字段。
2. **可失效性**：Redis 中的数据可以主动删除（踢人、封号、修改密码），JWT 签发后无法撤销。
3. **TTL 灵活控制**：JWT 过期时间和 Redis HASH 过期时间可以不一致——JWT 短（1h），Redis 长（25天）——即使 JWT 已过期，Redis 中仍保留用户信息以支持无缝刷新。
4. **减少 JWT 体积**：Claims 越多，JWT 字符串越长，每个请求的 Header 就越大。

---

## 六、安全性分析

### 6.1 已实现的安全措施

| 措施 | 实现 | 位置 |
|------|------|------|
| 双密钥分离 | Access Token 用 `access-secret-key`，Refresh Token 用 `fresh-secret-key` | `application.yml` → `JwtProperties` |
| JWT 短期过期 | Access Token 1小时，降低泄露风险窗口 | `jwt.accessExpiration` |
| Redis 存储校验 | JWT 验签通过后，还要检查 Redis 中是否存在（支持主动失效） | `AuthGlobalFilter` |
| Lua 原子登录 | 并发登录场景下，旧 Token 清理 + 新 Token 写入不可分割 | `login_token_atomic_operation.lua` |
| 接口限流 | AOP + ConcurrentHashMap 进程内限流（登录接口 30次/分钟/IP） | `UrlLimitAspect` |
| 验证码 | Kaptcha 图形验证码 + Redis 5min TTL | `UserServiceImpl.createCode()` |

### 6.2 当前安全弱点

| 问题 | 风险 | 建议 |
|------|------|------|
| 密码明文比较 | 数据库泄露 = 密码全暴露 | 接入 BCrypt 哈希 |
| 验证码校验已注释跳过 | 登录接口可被暴力破解 | 恢复验证码校验 |
| Refresh Token 365天过期太长 | 一旦泄露长期有效 | 缩短至 30 天，或引入 Refresh Token Rotation |
| 限流单实例 | 多 Pod 部署时各自计数 | 改用 Redis 滑动窗口限流 |
| 无设备/地域校验 | Token 被盗用无法检测 | 记录登录 IP + User-Agent，异常时告警 |

---

## 七、多端登录扩展方案

### 7.1 当前限制

现有 Lua 脚本中 `SMEMBERS` 拿到旧 Token 后**无条件全部删除**：

```lua
-- 当前逻辑: 任何新登录都会踢掉所有旧会话
if #oldTokens > 0 then
    redis.call('DEL', unpack(keysToDelete))  -- 全部删除
    redis.call('DEL', KEYS[1])
end
```

这意味着：
- 手机登录 → 踢掉电脑
- 电脑登录 → 踢掉手机
- 同设备重登 → 也走全量删除（虽然是同设备，但实现上仍然是全删后重写）

### 7.2 扩展目标

- **多端共存**：Web、iOS、Android、Desktop 各端独立会话
- **同设备重登替换**：同一台手机上重新登录，替换旧会话而非新增
- **最大设备数限制**：最多 N 台设备同时在线，超限自动踢出最久未活跃的设备
- **设备管理**：用户可查看、主动踢出指定设备
- **安全事件**：修改密码后所有设备强制下线

### 7.3 数据结构演进

**现有（单设备）：**
```
login:user:tokenList:1001          SET  {"accessToken_abc", "refreshToken_xyz"}
login:user:Info:accessToken:accessToken_abc     HASH  {id, username, avatar, ...}
login:user:Info:refreshToken:refreshToken_xyz   HASH  {id, username, avatar, ...}
```

**扩展后（多设备）：**
```
# Layer 1: 设备维度的 Token 索引（每个 deviceId 有独立的 accessToken + refreshToken）
login:user:tokenList:1001          SET  {
    "web_chrome:accessToken_abc",
    "ios_iphone:accessToken_def",
    "android_xiaomi:accessToken_ghi"
}

# Layer 2 & 3: 不变，仍然是 token → 用户信息 HASH
login:user:Info:accessToken:accessToken_abc     HASH  {id, username, ..., deviceId, platform}
login:user:Info:refreshToken:refreshToken_xyz   HASH  {id, username, ..., deviceId, platform}

# Layer 4: 新增设备会话元数据
login:user:sessions:1001           HASH {
    "web_chrome"    → '{"platform":"web","deviceName":"Chrome/Win11","ip":"1.2.3.4","loginTime":1721...,"lastActive":1721...}'
    "ios_iphone"    → '{"platform":"ios","deviceName":"iPhone 15 Pro","ip":"5.6.7.8","loginTime":1721...,"lastActive":1721...}'
}
```

### 7.4 Lua 脚本改造

核心变化：**从"全量删除"改为"同设备替换 + 设备数上限控制"**。

```lua
-- 新增参数:
-- ARGV[7] = deviceId        (如 "web_chrome")
-- ARGV[8] = maxDevices      (最大设备数，如 5)
-- ARGV[9] = deviceSessionJson (设备元数据 JSON)

-- ===== 第1步: 同设备旧 Token 替换 =====
local oldTokens = redis.call('SMEMBERS', KEYS[1])
local devicePrefix = ARGV[7] .. ':'

for i, item in ipairs(oldTokens) do
    -- item 格式: "deviceId:tokenValue"
    if string.sub(item, 1, #devicePrefix) == devicePrefix then
        local oldToken = string.sub(item, #devicePrefix + 1)
        -- 删除同设备旧 Hash
        redis.call('DEL', 'login:user:Info:accessToken:' .. oldToken)
        redis.call('DEL', 'login:user:Info:refreshToken:' .. oldToken)
        redis.call('SREM', KEYS[1], item)
    end
end

-- ===== 第2步: 设备数上限控制 =====
local currentCount = redis.call('SCARD', KEYS[1])
local maxDevices = tonumber(ARGV[8])
if currentCount >= maxDevices * 2 then  -- *2 因为每个设备有2个 token
    -- 策略: 找出最久未活跃的设备 → 踢出
    -- 从 sessions:{userId} HASH 中读取设备列表，按 lastActive 排序
    -- 删除最旧的设备对应的 Token
    -- （此处简化，实际需要从 sessions HASH 读取比较）
end

-- ===== 第3步: 写入新 Token =====
redis.call('SADD', KEYS[1], ARGV[7] .. ':' .. ARGV[1])
redis.call('SADD', KEYS[1], ARGV[7] .. ':' .. ARGV[2])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]) * 60)

-- ===== 第4步: 存储用户信息到 HASH（同现有逻辑）=====
-- ... hsetWithExpire(KEYS[2], ..., ARGV[4])
-- ... hsetWithExpire(KEYS[3], ..., ARGV[5])

-- ===== 第5步: 更新设备会话元数据 =====
local sessionKey = 'login:user:sessions:' .. userId  -- 需要从 ARGV 或 KEYS 传入
redis.call('HSET', sessionKey, ARGV[7], ARGV[9])
redis.call('EXPIRE', sessionKey, tonumber(ARGV[3]) * 60)

return 1
```

### 7.5 各层改动清单

#### 7.5.1 客户端改动

- 首次启动生成 UUID 作为 `deviceId`，持久化到本地存储（SharedPreferences / Keychain / localStorage）
- 登录请求新增三个 Header 或 Body 字段：
  - `deviceId`: UUID（必填）
  - `deviceName`: 设备名称，如 "iPhone 15 Pro"（选填，用于展示）
  - `platform`: `web` / `ios` / `android` / `desktop`（必填）
- `deviceId` 一旦生成不随登出改变，除非卸载重装

#### 7.5.2 im-auth 改动

| 文件 | 改动内容 |
|------|---------|
| `UserLoginDTO.java` | 新增 `deviceId`, `deviceName`, `platform` 字段 |
| `UserServiceImpl.java` | `generateAndStoreWithUpdateToken()` 传入 deviceId；JWT claims 中加入 deviceId |
| `login_token_atomic_operation.lua` | 改为按 deviceId 粒度清理，加入设备上限控制 |
| 新增 `DeviceSessionVO.java` | 设备会话视图对象：`{deviceId, platform, deviceName, ip, loginTime, lastActive, isCurrent}` |
| 新增 `SessionController.java` | `GET /user/sessions` — 设备列表；`DELETE /user/sessions/{deviceId}` — 踢设备；`POST /user/sessions/revoke-all` — 踢全部 |

#### 7.5.3 im-gateway 改动

| 文件 | 改动内容 |
|------|---------|
| `AuthGlobalFilter.java` | `passThrough()` 透传时增加 `deviceId` 字段（从 JWT claims 解析或从 Redis HASH 中读取） |
| `application.yml` | `auth.maxDevices: 5` 新增配置项 |

#### 7.5.4 im-common 改动

| 文件 | 改动内容 |
|------|---------|
| `RedisConstant.java` | 新增 `LOGIN_USER_SESSIONS_KEY = "login:user:sessions:"` 及 TTL |
| `UserBaseDTO.java` | 新增 `deviceId` 字段（可选，兼容旧客户端） |

### 7.6 新增 API 设计

#### GET /user/sessions — 查询活跃设备列表

```
Response:
{
  "code": 200,
  "data": [
    {
      "deviceId": "web_chrome_abc123",
      "platform": "web",
      "deviceName": "Chrome 126 / Windows 11",
      "ip": "192.168.1.100",
      "loginTime": "2026-07-24 10:30:00",
      "lastActive": "2026-07-24 14:22:00",
      "isCurrent": true          // 标记当前请求的设备
    },
    {
      "deviceId": "ios_iphone15_def456",
      "platform": "ios",
      "deviceName": "iPhone 15 Pro",
      "ip": "192.168.1.101",
      "loginTime": "2026-07-23 08:15:00",
      "lastActive": "2026-07-24 09:00:00",
      "isCurrent": false
    }
  ]
}
```

#### DELETE /user/sessions/{deviceId} — 踢出指定设备

```
Response:
{
  "code": 200,
  "message": "设备已下线"
}
```

实现逻辑：
1. 从 `tokenList:{userId}` SET 中找出匹配 `deviceId:` 前缀的 token
2. 删除对应的 accessToken 和 refreshToken 两个 HASH
3. 从 SET 中 SREM 移除
4. 从 `sessions:{userId}` HASH 中 HDEL 移除

#### POST /user/sessions/revoke-all — 踢出所有其他设备

用于修改密码后、安全事件响应等场景。

### 7.7 渐进式实施路线

```
Phase 1: 最小改造 — 同设备替换
  ├─ Lua 脚本改为按 deviceId 匹配删除（不引入设备数上限）
  ├─ 登录接口新增 deviceId 参数
  ├─ 网关/拦截器透传 deviceId
  └─ 验证: 多设备登录互不干扰，同设备重登替换旧会话

Phase 2: 设备管理与上限
  ├─ 新增 sessions:{userId} HASH
  ├─ 实现最大设备数限制 + 超限自动踢出最久未活跃设备
  ├─ GET /user/sessions 查询接口
  └─ 验证: 超限后旧设备自动下线，新设备可登录

Phase 3: 安全增强
  ├─ DELETE /user/sessions/{deviceId} 主动踢设备
  ├─ POST /user/sessions/revoke-all 踢全部
  ├─ 修改密码 → 调用 revoke-all
  ├─ 异常登录检测（新设备/新IP 通知）
  └─ 验证: 安全事件后所有未授权设备被强制下线
```

### 7.8 设计决策参考

| 决策点 | 建议 | 理由 |
|--------|------|------|
| 最大设备数 | 5 台 | 参考微信（手机+平板+PC=3台），作为练手项目放宽 |
| 超限策略 | 踢最久未活跃的设备 | 比直接拒绝登录体验更好 |
| 同设备判定 | 仅靠 deviceId 匹配 | UUID v4 碰撞概率可忽略 |
| Refresh Token Rotation | 暂不引入 | 增加复杂度；当前 Refresh Token 365天有效期足够用 |

---

## 八、关键文件索引

| 文件 | 说明 |
|------|------|
| [`im-auth/src/main/resources/login_token_atomic_operation.lua`](im-auth/src/main/resources/login_token_atomic_operation.lua) | 登录原子化 Lua 脚本 |
| [`im-auth/src/main/java/com/zzzlew/server/impl/UserServiceImpl.java`](im-auth/src/main/java/com/zzzlew/server/impl/UserServiceImpl.java) | 核心业务逻辑，`generateAndStoreWithUpdateToken()` |
| [`im-auth/src/main/java/com/zzzlew/controller/UserController.java`](im-auth/src/main/java/com/zzzlew/controller/UserController.java) | 登录/注册/刷新等 REST 端点 |
| [`im-gateway/src/main/java/com/zzzlew/filters/AuthGlobalFilter.java`](im-gateway/src/main/java/com/zzzlew/filters/AuthGlobalFilter.java) | 网关全局鉴权过滤器（双 Token 分流校验） |
| [`im-gateway/src/main/resources/application.yml`](im-gateway/src/main/resources/application.yml) | 网关路由 + JWT 密钥 + 排除/刷新路径配置 |
| [`im-common/common-redis/src/main/java/com/zzzlew/constant/RedisConstant.java`](im-common/common-redis/src/main/java/com/zzzlew/constant/RedisConstant.java) | 全项目 Redis Key 常量定义 |
| [`im-common/common-redis/src/main/java/com/zzzlew/properties/JwtProperties.java`](im-common/common-redis/src/main/java/com/zzzlew/properties/JwtProperties.java) | JWT 密钥/过期时间配置属性 |
| [`im-common/common-base/src/main/java/com/zzzlew/utils/JwtUtil.java`](im-common/common-base/src/main/java/com/zzzlew/utils/JwtUtil.java) | JWT 生成/解析工具类 |
| [`im-common/common-web/src/main/java/com/zzzlew/interceptor/UserInfoInterceptor.java`](im-common/common-web/src/main/java/com/zzzlew/interceptor/UserInfoInterceptor.java) | 下游服务用户信息拦截器 |

---

## 附录：Token 认证体系术语对照

| 术语 | 别名 | Redis Key 前缀 | JWT 签名密钥 | 过期时间 | 使用场景 |
|------|------|---------------|-------------|---------|---------|
| Access Token | 短期 Token / 访问令牌 | `login:user:Info:accessToken:` | `access-secret-key` | 1 小时 | 所有日常 API 请求 |
| Refresh Token | 长期 Token / 刷新令牌 | `login:user:Info:refreshToken:` | `fresh-secret-key` | 365 天 | Token 刷新 / 部分初始化接口 |
| Token List | Token 索引 / Token 集合 | `login:user:tokenList:` | — | ~25天（Redis TTL） | 记录用户所有活跃 Token，用于清理和审计 |
