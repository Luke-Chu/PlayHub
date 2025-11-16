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
    public Result<Long> seckillVoucher(int voucherId) {
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
        int updateCount = voucherMapper.decreaseStock(voucherId);
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
}
