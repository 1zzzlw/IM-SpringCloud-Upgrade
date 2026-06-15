-- 删除帖子时清理所有相关Redis缓存的原子操作
-- KEYS[1]: moments:list:info:{momentId}  (String类型，帖子详情缓存)
-- KEYS[2]: moments:count:{momentId}      (Hash类型，点赞数+评论数计数器)
-- KEYS[3]: moments:like:{momentId}       (Set类型，帖子点赞用户列表)
-- KEYS[4]: moments:list:new              (ZSet类型，最新排行榜)
-- KEYS[5]: moments:list:hot              (ZSet类型，热度排行榜)
-- ARGV[1]: momentId                      (帖子ID，用于从ZSet中移除)
-- 返回值: 删除的key数量

local infoKey    = KEYS[1]
local countKey   = KEYS[2]
local likeSetKey = KEYS[3]
local newRankKey = KEYS[4]
local hotRankKey = KEYS[5]
local momentId   = ARGV[1]

local deleted = 0

-- 1. 删除帖子详情缓存 (String)
deleted = deleted + redis.call('DEL', infoKey)

-- 2. 删除计数器 (Hash)
deleted = deleted + redis.call('DEL', countKey)

-- 3. 删除点赞用户集合 (Set)
deleted = deleted + redis.call('DEL', likeSetKey)

-- 4. 从最新排行榜中移除 (ZSet)
redis.call('ZREM', newRankKey, momentId)

-- 5. 从热度排行榜中移除 (ZSet)
redis.call('ZREM', hotRankKey, momentId)

return deleted
