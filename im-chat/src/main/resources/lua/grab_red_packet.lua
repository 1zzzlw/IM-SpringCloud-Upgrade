-- 抢红包 Lua 脚本（原子性保证）
-- KEYS[1] = "red_packet:{id}"           红包 Hash key
-- KEYS[2] = "red_packet:{id}:grabbed"   已领取用户 Set key
-- ARGV[1] = userId
-- ARGV[2] = redPacketType  (0=拼手气, 1=普通)
-- 返回: -1=已领取, -2=已领完或不存在, 其他=领取金额（单位：分）

local info = redis.call('HMGET', KEYS[1], 'remain_count', 'remain_amount', 'status')
if info[1] == false then return -2 end   -- key 不存在
if info[3] ~= '0' then return -2 end     -- 不在进行中
local remainCount = tonumber(info[1])
if remainCount <= 0 then return -2 end

-- 是否已领取
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -1 end

local remainAmount = tonumber(info[2])   -- 单位：分（存入时已乘100）

-- 计算金额
local amount
if remainCount == 1 then
    amount = remainAmount
elseif ARGV[2] == '1' then
    -- 普通均分
    local totalCount = tonumber(redis.call('HGET', KEYS[1], 'total_count'))
    local totalAmount = tonumber(redis.call('HGET', KEYS[1], 'total_amount'))
    amount = math.floor(totalAmount / totalCount)
    -- 最后一人领剩余
    if remainAmount - amount < amount then amount = remainAmount end
else
    -- 拼手气：二倍均值法
    local avg = remainAmount / remainCount
    local max = avg * 2
    local min = 1  -- 最小 1 分
    if max <= min then
        amount = min
    else
        -- 使用 userId + Redis 时间微秒 作为随机种子，保证每次调用的随机性
        local micros = redis.call('TIME')[2]
        math.randomseed(tonumber(ARGV[1]) % 100000 + micros)
        -- 修正：使用 (max - min + 1) 使上限可达 max
        amount = math.floor(min + math.random() * (max - min + 1))
    end
    if amount > remainAmount then amount = remainAmount end
end

-- 原子扣减（全部使用 HINCRBY，金额为整数分）
redis.call('HINCRBY', KEYS[1], 'remain_count', -1)
redis.call('HINCRBY', KEYS[1], 'remain_amount', -amount)
redis.call('SADD', KEYS[2], ARGV[1])

return amount
