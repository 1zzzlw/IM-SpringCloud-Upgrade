-- 抢红包 Lua 脚本（原子性保证）
-- KEYS[1] = "red_packet:{id}"           红包 Hash key
-- KEYS[2] = "red_packet:{id}:grabbed"   已领取用户 Set key
-- ARGV[1] = userId
-- ARGV[2] = redPacketType  (0=拼手气, 1=普通)
-- 返回: -1=已领取, -2=已领完或不存在, 其他=领取金额（单位：分）

-- 拿到红包的剩余数量，剩余金额，红包状态三种信息
local info = redis.call('HMGET', KEYS[1], 'remain_count', 'remain_amount', 'status')
if info[1] == false then
    return -2
end   -- key 不存在
if info[3] ~= '0' then
    return -2
end     -- 不在进行中
-- 把字符串转成数字，转换失败返回 nil
local remainCount = tonumber(info[1])
if remainCount <= 0 then
    return -2
end

-- 是否已领取
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end

local remainAmount = tonumber(info[2])   -- 单位：分（存入时已乘100）

-- 计算金额
local amount
-- 如果红包只剩下1个，就全部分给该用户
if remainCount == 1 then
    amount = remainAmount
    -- 如果是普通红包，就平均分配给该用户
elseif ARGV[2] == '1' then
    -- 普通均分
    local totalCount = tonumber(redis.call('HGET', KEYS[1], 'total_count'))
    local totalAmount = tonumber(redis.call('HGET', KEYS[1], 'total_amount'))
    amount = math.floor(totalAmount / totalCount)
    -- 最后一人领剩余
    if remainAmount - amount < amount then
        amount = remainAmount
    end
else
    -- 拼手气：二倍均值法，算法的目标：在保证每个人都能抢到钱（>0）的前提下，让金额尽量随机且公平，避免出现“前面的人抢99%，后面的人抢1分”的极端情况
    -- 先计算剩余平均值
    local avg = remainAmount / remainCount
    -- 把本次随机金额的范围锁定在：(1, 2 * 剩余平均值]
    local max = avg * 2
    local min = 1  -- 最小 1 分
    if max <= min then
        -- 如果剩余钱极少（比如只剩 1 分钱，但还有 2 个人），计算出的 max 可能小于等于 1。此时不再随机，直接返回 1分，确保最后一个人总能抢到最小单位
        amount = min
    else
        -- 使用 userId + Redis 时间微秒 作为随机种子，保证每次调用的随机性
        local micros = redis.call('TIME')[2]
        math.randomseed(tonumber(ARGV[1]) % 100000 + micros)
        -- 使用 (max - min + 1) 使上限可达 max
        amount = math.floor(min + math.random() * (max - min + 1))
    end
    if amount > remainAmount then
        amount = remainAmount
    end
end

-- 原子扣减（全部使用 HINCRBY，金额为整数分）
redis.call('HINCRBY', KEYS[1], 'remain_count', -1)
redis.call('HINCRBY', KEYS[1], 'remain_amount', -amount)
redis.call('SADD', KEYS[2], ARGV[1])

return amount
