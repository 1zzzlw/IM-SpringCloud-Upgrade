-- 原子性增加评论计数，若 key 不存在则返回 -1 由 Java 层预热后重试
-- KEYS[1]: moments:count:{momentId}
-- ARGV[1]: delta (1 或 -1)
-- 返回值: 新的评论数，-1 表示 key 不存在需要预热

local countKey = KEYS[1]
local delta = tonumber(ARGV[1])

local exists = redis.call('EXISTS', countKey)
if exists == 0 then
    return -1  -- 缓存不存在，Java 层负责预热
end

return redis.call('HINCRBY', countKey, 'comment', delta)
