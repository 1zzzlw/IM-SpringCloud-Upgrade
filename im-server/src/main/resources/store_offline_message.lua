-- 存储离线消息：直接存JSON到Sorted Set
-- KEYS[1]: 离线消息Sorted Set的key (user:offline:message:content:{userId})
-- ARGV[1]: 消息JSON字符串
-- ARGV[2]: 雪花ID（score，用于按发送顺序排序）
-- ARGV[3]: 过期时间（秒）

-- 1. 添加消息到Sorted Set（JSON作为member，时间戳作为score）
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 2. 检查并设置过期时间
local ttl = redis.call('TTL', KEYS[1])
if ttl == -1 or ttl == -2 then
    redis.call('EXPIRE', KEYS[1], ARGV[3])
end

-- 3. 返回当前消息数量
return redis.call('ZCARD', KEYS[1])