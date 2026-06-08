# 朋友圈服务(MomentsService)安全性与并发问题分析报告

## 1. 核心问题概述

当前 `MomentsServiceImpl` 存在多个严重的并发安全问题和缓存一致性问题，主要集中在点赞功能和缓存更新逻辑上。

---

## 2. 详细问题分析

### 2.1 【严重】点赞功能的竞态条件问题

**问题代码位置：** `like()` 方法

**问题描述：**
```java
// 原子性添加用户到Set，返回true表示是新成员
Boolean isNewMember = stringRedisTemplate.opsForSet().add(key, userId) > 0;

if (isNewMember) {
    // 新点赞：DB+1，Redis+1
    momentsMapper.like(momentId, 1);
    stringRedisTemplate.expire(key, MOMENTS_LIKE_KEY_TTL, TimeUnit.SECONDS);
    stringRedisTemplate.opsForHash().increment(countKey, "like", 1);
} else {
    // 取消点赞：DB-1，Redis-1
    momentsMapper.like(momentId, -1);
    stringRedisTemplate.opsForSet().remove(key, userId);
    stringRedisTemplate.opsForHash().increment(countKey, "like", -1);
}
```

**核心问题：**
1. **非原子性操作链：** Redis Set 添加、数据库更新、Hash 计数器更新是三个独立操作，中间任何环节失败都会导致数据不一致
2. **重复点赞漏洞：** 用户快速点击多次，可能导致：
   - Set 中添加成功但 DB 更新失败
   - DB 更新成功但计数器更新失败
   - 最终点赞数与实际不符
3. **取消点赞的逻辑错误：** `isNewMember = false` 时直接取消点赞，但没有验证用户之前是否真的点赞过（Set 可能因为 TTL 过期而丢失数据）

**风险等级：** 🔴 严重
**影响范围：** 数据一致性、业务准确性、用户体验

---

### 2.2 【严重】缓存预热时的竞态条件

**问题代码位置：** `like()` 方法中的缓存预热逻辑

```java
if (BooleanUtil.isFalse(stringRedisTemplate.hasKey(countKey))) {
    MomentsVO momentsVO = getById(momentId);
    stringRedisTemplate.opsForHash().putIfAbsent(countKey, "like", String.valueOf(momentsVO.getLikeCount()));
    stringRedisTemplate.opsForHash().putIfAbsent(countKey, "comment", String.valueOf(momentsVO.getCommentCount()));
    stringRedisTemplate.expire(countKey, MOMENTS_COUNT_KEY_TTL, TimeUnit.SECONDS);
}
```

**核心问题：**
1. **检查-设置非原子性：** `hasKey()` 和后续的 `putIfAbsent()` 之间存在时间窗口
2. **并发预热冲突：** 多个线程同时检测到缓存不存在，都会去 DB 查询并写入，造成：
   - 数据库查询压力突增
   - 可能写入过期数据（如果 DB 查询时间不同）
3. **expire 覆盖问题：** 每次调用都重新设置 TTL，可能导致 TTL 被频繁刷新，缓存永不过期

**风险等级：** 🔴 严重
**影响范围：** 缓存击穿、数据库压力、缓存一致性

---

### 2.3 【高危】缓存与数据库的双写不一致

**问题代码位置：** `like()` 方法

**场景分析：**

**场景 1：数据库更新成功，Redis 更新失败**
```
1. Redis Set 添加成功 (userId 加入点赞集合)
2. DB 点赞数 +1 成功
3. Redis Hash 计数器 +1 失败 (网络抖动/Redis 挂了)
→ 结果：DB 显示已点赞，Redis 计数器未增加，前端显示的数字不准确
```

**场景 2：Redis 更新成功，数据库更新失败**
```
1. Redis Set 添加成功
2. DB 点赞数 +1 失败 (数据库锁等待超时/连接池耗尽)
3. Redis Hash 计数器 +1 成功
→ 结果：前端显示已点赞，但 DB 未记录，缓存失效后数据丢失
```

**场景 3：取消点赞的幽灵问题**
```
1. 用户点赞成功（Set + DB + Hash 都成功）
2. 24小时后，Redis 的 MOMENTS_LIKE_KEY 过期，Set 被清空
3. 用户再次点击点赞按钮
4. 代码检测 Set.add() 返回 false（因为重建的Set是空的）
5. 误判为"取消点赞"，执行 DB -1
→ 结果：用户实际想点赞，但系统执行了取消操作
```

**风险等级：** 🔴 严重
**影响范围：** 数据一致性、用户体验、业务逻辑错误

---

### 2.4 【中危】批量回填缓存的事务问题

**问题代码位置：** `replenishRedis()` 方法

```java
stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (MomentsVO moment : data) {
        connection.zAdd(...);
        connection.set(...);
        connection.expire(...);
        connection.hashCommands().hSet(...);
        connection.expire(...);
    }
    return null;
});
```

**核心问题：**
1. **Pipeline 非事务性：** Pipeline 只是批量发送命令，不保证原子性
2. **部分失败无感知：** 如果中间某个命令失败（如内存不足），无法回滚也无法感知
3. **expire 时序问题：** `set()` 和 `expire()` 分开执行，中间如果失败会导致 key 永不过期

**风险等级：** 🟡 中等
**影响范围：** 缓存一致性、内存泄漏风险

---

### 2.5 【中危】最热排行榜更新的竞态条件

**问题代码位置：** `like()` 方法末尾

```java
long score = Long.parseLong(stringRedisTemplate.opsForHash().get(countKey, "like").toString());
stringRedisTemplate.opsForZSet().add(MOMENTS_LIST_HOT_KEY, String.valueOf(momentId), score);
```

**核心问题：**
1. **读取与更新不原子：** 先读 Hash 计数器，再更新 ZSet，中间其他请求可能修改了计数器
2. **NPE 风险：** `get(countKey, "like")` 可能返回 null（缓存过期），直接 `toString()` 会抛出 NPE
3. **数据漂移：** 多线程并发时，ZSet 中的 score 可能与实际点赞数不一致

**风险等级：** 🟡 中等
**影响范围：** 排行榜准确性、系统稳定性

---

### 2.6 【低危】缓存穿透风险

**问题代码位置：** `list()` 方法

**问题描述：**
- 当查询不存在的 `momentId` 时，会直接穿透到数据库
- 没有使用布隆过滤器或空值缓存机制
- 恶意攻击者可以构造大量不存在的 ID 发起查询

**风险等级：** 🟢 较低
**影响范围：** 数据库压力、系统可用性

---

### 2.7 【低危】缓存雪崩风险

**问题描述：**
- 所有朋友圈缓存的 TTL 都设置为 24 小时（86400秒）
- 如果大批量帖子同时发布，会在 24 小时后同时失效
- 可能导致瞬时数据库压力激增

**风险等级：** 🟢 较低
**影响范围：** 系统稳定性

---

## 3. 必须使用 Lua 脚本的场景

### 3.1 点赞/取消点赞操作

**原因：** 需要保证以下操作的原子性
```lua
-- 伪代码逻辑
1. 检查 Set 中是否存在用户
2. 如果不存在：
   - Set 添加用户
   - Hash 计数器 +1
   - ZSet 更新排行榜
3. 如果存在：
   - Set 移除用户
   - Hash 计数器 -1
   - ZSet 更新排行榜
```

### 3.2 缓存预热操作

**原因：** 需要原子性地检查并设置缓存
```lua
-- 伪代码逻辑
1. 检查 countKey 是否存在
2. 如果不存在：
   - 返回特殊标识，由 Java 代码查询 DB
3. 如果存在：
   - 直接执行点赞逻辑
```

---

## 4. 优化建议

### 4.1 短期优化（无需 Lua）

1. **添加分布式锁（推荐使用 Redisson）**
   ```java
   RLock lock = redissonClient.getLock("moments:like:lock:" + momentId + ":" + userId);
   try {
       if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
           // 执行点赞逻辑
       }
   } finally {
       lock.unlock();
   }
   ```

2. **使用 Redis 事务（MULTI/EXEC）**
   - 将 Set、Hash、ZSet 操作放入同一事务

3. **添加异常处理和数据校验**
   - 捕获所有 Redis 操作的异常
   - NPE 防护
   - 数据存在性验证

4. **实现缓存一致性补偿机制**
   - 定时任务校对 Redis 与 MySQL 数据
   - 消息队列异步更新缓存

### 4.2 长期优化（推荐使用 Lua）

1. **点赞操作改为 Lua 脚本**
2. **缓存预热改为 Lua 脚本**
3. **使用延迟双删策略**
4. **引入布隆过滤器防止缓存穿透**
5. **为 TTL 添加随机偏移防止雪崩**

---

## 5. 风险评估总结

| 问题类别 | 风险等级 | 是否需要 Lua | 修复优先级 |
|---------|---------|-------------|-----------|
| 点赞竞态条件 | 🔴 严重 | **强烈推荐** | P0 |
| 缓存预热竞态 | 🔴 严重 | **推荐** | P0 |
| 双写不一致 | 🔴 严重 | **推荐** | P0 |
| 批量回填事务 | 🟡 中等 | 可选 | P1 |
| 排行榜竞态 | 🟡 中等 | **推荐** | P1 |
| 缓存穿透 | 🟢 较低 | 不需要 | P2 |
| 缓存雪崩 | 🟢 较低 | 不需要 | P2 |

---

## 6. 结论

**当前代码的核心问题在于将多个 Redis 操作和数据库操作分开执行，缺乏原子性保证。**

**强烈建议：**
1. **点赞功能必须使用 Lua 脚本重写**（避免数据不一致和重复点赞漏洞）
2. **缓存预热逻辑建议使用 Lua 脚本**（防止缓存击穿）
3. **排行榜更新建议使用 Lua 脚本**（保证数据一致性）

如果不使用 Lua 脚本，至少需要：
- 添加分布式锁（性能会下降 50% 以上）
- 完善异常处理和数据校验
- 实现数据一致性补偿机制

---

**分析时间：** 2026-06-08  
**分析人员：** Kiro AI Assistant  
**文档版本：** v1.0
