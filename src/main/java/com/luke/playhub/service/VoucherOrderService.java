package com.luke.playhub.service;

import com.luke.playhub.dto.Result;

/**
 * @author Luke
 * @since 2025/11/16 15:42
 */

public interface VoucherOrderService {
    Result<Long> seckillVoucher(int voucherId);
}
