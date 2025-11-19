package com.luke.playhub.service;

import com.luke.playhub.dto.Result;

/**
 * @author Luke
 * @since 2025/11/16 15:42
 */

public interface VoucherOrderService {
    Result<Long> createOrderWithOversold(long voucherId);

    Result<Long> createOrderStockAsVersion(long voucherId);

    Result<Long> createOrderStockGreaterZero(long voucherId);

    Result<Long> createOrderOnePersonOneOrder(long voucherId);

    Result<Long> createOrderOnePersonOneOrderWithSynchronized(long voucherId);

    Result<Long> createOrderOnePersonOneOrderWithFinalMethod(long voucherId);

    Result<Long> createVoucherOrderFinalMethod(long voucherId);

    Result<Long> createOrderDistributedLockWithRedis(long voucherId);
}
