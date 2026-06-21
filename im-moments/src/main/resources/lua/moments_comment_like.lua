-- 评论点赞/取消点赞原子操作
-- KEYS[1]: moments:comment:like:{commentId} (Set类型，存储点赞用户ID)
-- ARGV[1]: userId
-- 返回值: 1-点赞成功，0-取消点赞成功

local likeSetKey = KEYS[1]
local userId = ARGV[1]

local isMember = redis.call('SISMEMBER', likeSetKey, userId)

if isMember == 1 then
    redis.call('SREM', likeSetKey, userId)
    return 0  -- 取消点赞
else
    redis.call('SADD', likeSetKey, userId)
    return 1  -- 点赞
end
