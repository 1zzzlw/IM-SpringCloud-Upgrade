-- KEYS：在线用户集合、用户路由、Access Token 信息、设备会话。
-- ARGV：用户 ID、设备 ID、节点 ID、连接 ID、在线 TTL（秒）、会话 ID。
if redis.call('EXISTS', KEYS[3]) == 0 then return -1 end
if ARGV[6] ~= '' and redis.call('HGET', KEYS[4], 'sessionId') ~= ARGV[6] then
    return -1
end

local ok, routes = pcall(cjson.decode, redis.call('HGET', KEYS[2], ARGV[1]) or '{}')
if not ok or type(routes) ~= 'table' then routes = {} end
local first = next(routes) == nil
routes[ARGV[2]] = {ARGV[3], ARGV[4]}
redis.call('HSET', KEYS[2], ARGV[1], cjson.encode(routes))
redis.call('SADD', KEYS[1], ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[5])
if ARGV[6] ~= '' then redis.call('HSET', KEYS[4], 'connectionId', ARGV[4]) end
return first and 1 or 0
