-- 朋友圈点赞/取消点赞原子操作
-- KEYS[1]: moments:like:{momentId} (Set类型，存储点赞用户ID)
-- KEYS[2]: moments:count:{momentId} (Hash类型，存储点赞数和评论数)
-- KEYS[3]: moments:list:hot (ZSet类型，最热排行榜)
-- ARGV[1]: userId (用户ID)
-- ARGV[2]: momentId (帖子ID)
-- ARGV[3]: countTTL (计数Hash的过期时间，秒)
-- 返回值: 1-点赞成功，0-取消点赞成功，-1-缓存不存在需要预热

local likeSetKey = KEYS[1]
local countKey = KEYS[2]
local hotRankKey = KEYS[3]
local userId = ARGV[1]
local momentId = ARGV[2]
local countTTL = tonumber(ARGV[3])

-- 检查计数器是否存在（判断缓存是否预热）
local countExists = redis.call('EXISTS', countKey)
if countExists == 0 then
    return -1  -- 缓存未预热，需要Java层处理
end

-- 检查用户是否已点赞
local isMember = redis.call('SISMEMBER', likeSetKey, userId)

if isMember == 1 then
    -- 用户已点赞，执行取消点赞
    redis.call('SREM', likeSetKey, userId)
    local newCount = redis.call('HINCRBY', countKey, 'like', -1)
    
    -- 更新热度排行榜（使用最新的点赞数作为分数）
    redis.call('ZADD', hotRankKey, newCount, momentId)
    
    -- 刷新计数器过期时间
    redis.call('EXPIRE', countKey, countTTL)
    
    -- 注意：点赞Set不设置过期时间，保证点赞关系永久准确
    
    return 0  -- 取消点赞成功
else
    -- 用户未点赞，执行点赞
    redis.call('SADD', likeSetKey, userId)
    local newCount = redis.call('HINCRBY', countKey, 'like', 1)
    
    -- 更新热度排行榜
    redis.call('ZADD', hotRankKey, newCount, momentId)
    
    -- 只刷新计数器过期时间，点赞Set不过期
    redis.call('EXPIRE', countKey, countTTL)
    
    return 1  -- 点赞成功
end
