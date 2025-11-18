package com.luke.playhub.service.impl;

import cn.hutool.core.util.IdUtil;
import com.luke.playhub.context.UserContext;
import com.luke.playhub.dto.Result;
import com.luke.playhub.entity.Voucher;
import com.luke.playhub.entity.VoucherOrder;
import com.luke.playhub.mapper.VoucherMapper;
import com.luke.playhub.mapper.VoucherOrderMapper;
import com.luke.playhub.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luke
 * @since 2025/11/16 15:43
 */

@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private final VoucherMapper voucherMapper;
    private final VoucherOrderMapper voucherOrderMapper;

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

    private VoucherOrder createVoucherOrder(long voucherId) {
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(IdUtil.getSnowflakeNextId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserContext.getUserId());
        voucherOrderMapper.create(voucherOrder);
        return voucherOrder;
    }
}
