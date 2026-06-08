-- 朋友圈缓存预热原子操作（检查并初始化）
-- KEYS[1]: moments:count:{momentId} (Hash类型，存储点赞数和评论数)
-- ARGV[1]: likeCount (从DB查询的点赞数)
-- ARGV[2]: commentCount (从DB查询的评论数)
-- ARGV[3]: countTTL (过期时间，秒)
-- 返回值: 1-成功初始化，0-已存在无需初始化

local countKey = KEYS[1]
local likeCount = ARGV[1]
local commentCount = ARGV[2]
local countTTL = tonumber(ARGV[3])

-- 检查是否已存在
local exists = redis.call('EXISTS', countKey)
if exists == 1 then
    return 0  -- 已存在，无需初始化
end

-- 原子性地设置 Hash 和过期时间
redis.call('HSET', countKey, 'like', likeCount, 'comment', commentCount)
redis.call('EXPIRE', countKey, countTTL)

return 1  -- 成功初始化
