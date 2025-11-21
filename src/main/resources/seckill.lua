 -- 1. 参数列表
local voucherId = ARGV[1] -- 1.1 秒杀券ID
local userId = ARGV[2] -- 1.2 用户ID

 -- 2. 秒杀库存key
local stockKey = 'stock:voucher:' .. voucherId
 -- 3. 秒杀订单key
local orderKey = 'order:voucher:' .. voucherId

 -- 4. 判断库存是否充足
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1 -- 库存不足
end

if (redis.call('sismember', orderKey, userId) == 1) then
    return 2 -- 用户已购买
end

 -- 5. 扣减库存
redis.call('incrby', stockKey, -1)
 -- 6. 记录用户购买信息
redis.call('sadd', orderKey, userId)
 -- 7. 返回订单ID
return 0 -- 秒杀成功