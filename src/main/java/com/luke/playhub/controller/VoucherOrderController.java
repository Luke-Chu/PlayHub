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

    @PostMapping("/seckill/{voucherId}")
    public Result<Long> seckillVoucher(@PathVariable int voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
