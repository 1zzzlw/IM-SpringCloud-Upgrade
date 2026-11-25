-- KEYS：在线用户集合、用户路由、最后离线时间、当前连接的存活标记。
-- ARGV：用户 ID、设备 ID、连接 ID、离线时间。
redis.call('DEL', KEYS[4])
local ok, routes = pcall(cjson.decode, redis.call('HGET', KEYS[2], ARGV[1]) or '{}')
if not ok or type(routes) ~= 'table' then return 0 end
local route = routes[ARGV[2]]
if not route or route[2] ~= ARGV[3] then return 0 end

routes[ARGV[2]] = nil
if next(routes) then
    redis.call('HSET', KEYS[2], ARGV[1], cjson.encode(routes))
    return 2
end
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('SREM', KEYS[1], ARGV[1])
redis.call('HSET', KEYS[3], ARGV[1], ARGV[4])
return 1
