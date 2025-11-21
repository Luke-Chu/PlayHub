package com.luke.playhub.controller;

import com.luke.playhub.dto.Result;
import com.luke.playhub.entity.Voucher;
import com.luke.playhub.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Luke
 * @since 2025/11/21 17:18
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/voucher")
public class VoucherController {
    private final VoucherService voucherService;

    @PostMapping("/create")
    public Result<Long> createVoucher(@RequestParam long shopId, @RequestParam int stock) {
        Voucher voucher = new Voucher();
        voucher.setShopId(shopId);
        voucher.setStock(stock);
        voucherService.save(voucher);
        return Result.success(voucher.getId());
    }
}
