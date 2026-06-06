-- KEYS参数说明：
-- KEYS[1] = login:user:tokenList:{userId} （用户token集合key）
-- KEYS[2] = login:user:Info:accessToken:{accessToken} （accessToken用户信息key）
-- KEYS[3] = login:user:Info:refreshToken:{refreshToken} （refreshToken用户信息key）

-- ARGV参数说明：
-- ARGV[1] = accessToken （新生成的短期token）
-- ARGV[2] = refreshToken （新生成的长期token）
-- ARGV[3] = tokenListTTL （用户token集合过期时间，单位：分钟）
-- ARGV[4] = accessTokenTTL （accessToken用户信息过期时间，单位：分钟）
-- ARGV[5] = refreshTokenTTL （refreshToken用户信息过期时间，单位：分钟）
-- ARGV[6] = userInfoJson （用户信息JSON字符串）

-- 首先检查该用户id下是否有已登录的短期token数据集合
local oldTokens = redis.call('SMEMBERS', KEYS[1])

-- 如果有该用户id下，有旧Token，需要把上次登录留下的旧token信息删除  #取表长度
if #oldTokens > 0 then
    -- 构造删除的 key 列表
    local keysToDelete = {}

    -- 遍历旧 tokens，构造需要删除的 key
    for i, token in ipairs(oldTokens) do
        table.insert(keysToDelete, 'login:user:Info:accessToken:' .. token)
        table.insert(keysToDelete, 'login:user:Info:refreshToken:' .. token)
    end

    -- 批量删除
    if #keysToDelete > 0 then
        redis.call('DEL', unpack(keysToDelete))
    end

    -- 删除旧的 userKey 集合
    redis.call('DEL', KEYS[1])
end

-- 存储新 tokens 到集合
redis.call('SADD', KEYS[1], ARGV[1], ARGV[2])
-- 设置 userKey 过期时间
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]) * 60)

-- 解析用户信息JSON并存储
local function hsetWithExpire(key, fields, ttl)
    if #fields > 0 then
        redis.call('HSET', key, unpack(fields))
        redis.call('EXPIRE', key, tonumber(ttl) * 60)
    end
end

local userInfoFields = {}
local ok, userInfo = pcall(function()
    return cjson.decode(ARGV[6])
end) -- 解析JSON
if ok and userInfo then
    for k, v in pairs(userInfo) do
        if v and v ~= "" then
            -- 彻底空值校验
            table.insert(userInfoFields, k)
            table.insert(userInfoFields, tostring(v))
        end
    end
end

hsetWithExpire(KEYS[2], userInfoFields, ARGV[4])
hsetWithExpire(KEYS[3], userInfoFields, ARGV[5])

return 1