-- KEYS：设备 Token 索引、新 access/refresh 信息、旧版 Token 集合、待轮换的 refresh 信息。
-- ARGV：新双 Token（退出时为空）、三项 TTL（分钟）、用户信息 JSON、设备 ID、是否替换连接、旧 refresh、旧版标记。
local expected = ARGV[9]
local legacy = ARGV[10] == '1'
local oldRefresh = redis.call('HGET', KEYS[1], 'refresh')

if expected ~= '' then
    if redis.call('EXISTS', KEYS[5]) == 0 then return 0 end
    if legacy then
        if oldRefresh then return 0 end
    elseif oldRefresh ~= expected then
        return 0
    end
end

local fields = {}
if ARGV[1] ~= '' then
    local profile = cjson.decode(ARGV[6])
    profile.deviceId = ARGV[7]
    for k, v in pairs(profile) do
        if v ~= cjson.null and v ~= '' then
            fields[#fields + 1] = k
            fields[#fields + 1] = tostring(v)
        end
    end
end

local accessPrefix = 'login:user:Info:accessToken:'
local refreshPrefix = 'login:user:Info:refreshToken:'
local oldAccess = redis.call('HGET', KEYS[1], 'access')
if oldAccess then redis.call('DEL', accessPrefix .. oldAccess) end
if oldRefresh then redis.call('DEL', refreshPrefix .. oldRefresh) end

-- 旧客户端登录或持旧凭据恢复时，清理单端版本的 Token。
if legacy then
    for _, token in ipairs(redis.call('SMEMBERS', KEYS[4])) do
        redis.call('DEL', accessPrefix .. token, refreshPrefix .. token)
    end
    redis.call('DEL', KEYS[4])
    if expected ~= '' then redis.call('DEL', KEYS[5]) end
end

if ARGV[1] == '' then
    redis.call('DEL', KEYS[1])
    return 1
end

-- 登录时让旧连接失效，普通续期保留当前连接。
if ARGV[8] == '1' then redis.call('HDEL', KEYS[1], 'connectionId') end
-- 在设备登录或续期时清理原有索引中的旧字段。
redis.call('HDEL', KEYS[1], 'sessionId')
redis.call('HSET', KEYS[1], 'access', ARGV[1], 'refresh', ARGV[2])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]) * 60)
redis.call('HSET', KEYS[2], unpack(fields))
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4]) * 60)
redis.call('HSET', KEYS[3], unpack(fields))
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[5]) * 60)
return 1
