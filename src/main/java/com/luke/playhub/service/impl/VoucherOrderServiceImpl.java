package com.luke.playhub.service.impl;

import com.luke.playhub.dto.Result;
import com.luke.playhub.service.VoucherOrderService;
import org.springframework.stereotype.Service;

/**
 * @author Luke
 * @since 2025/11/16 15:43
 */

@Service
public class VoucherOrderServiceImpl implements VoucherOrderService {
    @Override
    public Result<Long> seckillVoucher(int voucherId) {
        return Result.success(200L);
    }
}
