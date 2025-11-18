package com.luke.playhub.controller;

import com.luke.playhub.dto.Result;
import com.luke.playhub.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Luke
 * @since 2025/11/16 15:39
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    private final VoucherOrderService voucherOrderService;

    @PostMapping("/seckill/withOversold/{voucherId}")
    public Result<Long> seckillWithOversold(@PathVariable long voucherId) {
        return voucherOrderService.createOrderWithOversold(voucherId);
    }

    @PostMapping("/seckill/stockAsVersion/{voucherId}")
    public Result<Long> seckillStockAsVersion(@PathVariable long voucherId) {
        return voucherOrderService.createOrderStockAsVersion(voucherId);
    }

    @PostMapping("/seckill/stockGreaterZero/{voucherId}")
    public Result<Long> seckillStockGreaterZero(@PathVariable long voucherId) {
        return voucherOrderService.createOrderStockGreaterZero(voucherId);
    }

    @PostMapping("/seckill/onePersonOneOrder/{voucherId}")
    public Result<Long> seckillOnePersonOneOrder(@PathVariable long voucherId) {
        return voucherOrderService.createOrderOnePersonOneOrder(voucherId);
    }

    @PostMapping("/seckill/onePersonOneOrderWithSynchronized/{voucherId}")
    public Result<Long> seckillOnePersonOneOrderWithSynchronized(@PathVariable long voucherId) {
        return voucherOrderService.createOrderOnePersonOneOrderWithSynchronized(voucherId);
    }
}
