package com.luke.playhub.service.impl;

import cn.hutool.core.util.IdUtil;
import com.luke.playhub.context.UserContext;
import com.luke.playhub.dto.Result;
import com.luke.playhub.entity.Voucher;
import com.luke.playhub.entity.VoucherOrder;
import com.luke.playhub.mapper.VoucherMapper;
import com.luke.playhub.mapper.VoucherOrderMapper;
import com.luke.playhub.service.VoucherOrderService;
import com.luke.playhub.utils.SimpleRedisLock;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * @author Luke
 * @since 2025/11/16 15:43
 */

@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private final VoucherMapper voucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Integer> seckillScript = new DefaultRedisScript<>();
    static {
        seckillScript.setLocation(new org.springframework.core.io.ClassPathResource("seckill.lua"));
        seckillScript.setResultType(Integer.class);
    }

    /**
     * 创建订单，不解决超卖问题
     */
    @Transactional
    @Override
    public Result<Long> createOrderWithOversold(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        // 3. 扣减库存
        int updateCount = voucherMapper.decreaseStockWithOversold(voucherId);
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 4. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(IdUtil.getSnowflakeNextId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserContext.getUserId());
        voucherOrderMapper.create(voucherOrder);
        return Result.success(voucherOrder.getId());
    }

    /**
     * 版本号解决超卖问题，存在成功率太低问题
     */
    @Transactional
    @Override
    public Result<Long> createOrderStockAsVersion(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        // 3. 扣减库存
        int updateCount = voucherMapper.decreaseStockAsVersion(voucherId, voucher.getStock());
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 4. 创建订单
        return Result.success(createVoucherOrder(voucherId).getId());
    }

    /**
     * 超卖问题最终解决方案：乐观锁 --> where 条件后带 stock > 0
     */
    @Override
    public Result<Long> createOrderStockGreaterZero(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        // 3. 扣减库存
        int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 4. 创建订单
        return Result.success(createVoucherOrder(voucherId).getId());
    }

    /**
     * 初始一人一单解决方案，存在并发问题
     */
    @Override
    public Result<Long> createOrderOnePersonOneOrder(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        // 3. 查询订单表，看看有没有数据
        VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(UserContext.getUserId(), voucherId);
        if (existingOrder != null) {
            return Result.error("每人限领一张");
        }

        // 4. 扣减库存
        int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 5. 创建订单
        return Result.success(createVoucherOrder(voucherId).getId());
    }

    /**
     * synchronized 加锁解决一人一单，存在事务问题
     */
    @Override
    public Result<Long> createOrderOnePersonOneOrderWithSynchronized(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        return createVoucherOrderSynchronized(voucherId);
    }

    /**
     * synchronized 加锁解决一人一单，并解决事务问题。
     */
    @Override
    public Result<Long> createOrderOnePersonOneOrderWithFinalMethod(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        Long userId = UserContext.getUserId();
        synchronized (userId.toString().intern()) {
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrderFinalMethod(voucherId);
        }
    }

    /**
     * 分布式锁Redis的SETNX命令来替代 synchronized 判断是否重复下单
     */
    @Override
    public Result<Long> createOrderDistributedLockWithRedis(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        Long userId = UserContext.getUserId();
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("voucher:" + voucherId + userId, redisTemplate);
        boolean isLock = simpleRedisLock.tryLock(1200);
        // 加锁失败
        if (!isLock) {
            return Result.error("请勿重复下单");
        }
        try {
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrderFinalMethod(voucherId);
        } finally {
            // 释放锁
            simpleRedisLock.unlock();
        }
    }

    /**
     * Redisson实现分布式锁：可重入、可重试、可续期
     */
    @Override
    public Result<Long> createOrderDistributedLockWithRedisson(long voucherId) {
        // 1. 查询优惠券信息
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return Result.error("优惠券不存在");
        }

        // 2. 判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.error("库存不足");
        }

        Long userId = UserContext.getUserId();
        // Redisson 获取分布式锁
        RLock lock = redissonClient.getLock("voucher:" + voucherId + userId);
        boolean isLock = lock.tryLock(); // 获取锁等待时间、锁超时释放时间、时间单位
        // 加锁失败
        if (!isLock) {
            return Result.error("请勿重复下单");
        }
        try {
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrderFinalMethod(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    /**
     * 秒杀优化方案：使用Redis + lua脚本判断是否有秒杀资格
     */
    @Override
    public Result<Long> createOrderOptimization(long voucherId) {
        // 1. 执行 Lua 判断资格
        int result = redisTemplate.execute(seckillScript,
                Collections.emptyList(),
                voucherId, UserContext.getUserId().toString());
        if (result != 0) {
            return Result.error(result == 1 ? "库存不足" : "请勿重复下单");
        }

        // 2. Redis 判断资格成功：生成订单ID
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(IdUtil.getSnowflakeNextId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserContext.getUserId());

        // 3. 发送MQ（异步下单）
        rabbitTemplate.convertAndSend("seckill.exchange", "seckill.order", voucherOrder);

        // 4. 返回订单ID
        return Result.success(voucherOrder.getId());
    }

    @Transactional
    public Result<Long> createVoucherOrderFinalMethod(long voucherId) {
        // 3. 查询订单表，看看有没有数据
        VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(UserContext.getUserId(), voucherId);
        if (existingOrder != null) {
            return Result.error("请勿重复下单");
        }

        // 4. 扣减库存
        int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
        if (updateCount <= 0) {
            return Result.error("库存不足");
        }

        // 5. 创建订单
        return Result.success(createVoucherOrder(voucherId).getId());
    }

    @Transactional
    public Result<Long> createVoucherOrderSynchronized(long voucherId) {
        Long userId = UserContext.getUserId();
        synchronized (userId.toString().intern()) {
            // 3. 查询订单表，看看有没有数据
            VoucherOrder existingOrder = voucherOrderMapper.findByUserIdAndVoucherId(UserContext.getUserId(), voucherId);
            if (existingOrder != null) {
                return Result.error("请勿重复下单");
            }

            // 4. 扣减库存
            int updateCount = voucherMapper.decreaseStockGreaterZero(voucherId);
            if (updateCount <= 0) {
                return Result.error("库存不足");
            }

            // 5. 创建订单
            return Result.success(createVoucherOrder(voucherId).getId());
        }
    }

    private VoucherOrder createVoucherOrder(long voucherId) {
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(IdUtil.getSnowflakeNextId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserContext.getUserId());
        voucherOrderMapper.create(voucherOrder);
        return voucherOrder;
    }

    @Override
    public void create(VoucherOrder voucherOrder) {
        voucherOrderMapper.create(voucherOrder);
    }

    @Override
    public boolean exists(Long voucherOrderId) {
        VoucherOrder voucherOrder = voucherOrderMapper.selectById(voucherOrderId);
        return voucherOrder != null;
    }
}
