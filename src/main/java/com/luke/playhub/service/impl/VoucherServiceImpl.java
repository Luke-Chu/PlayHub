package com.luke.playhub.service.impl;

import com.luke.playhub.entity.Voucher;
import com.luke.playhub.mapper.VoucherMapper;
import com.luke.playhub.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Luke
 * @since 2025/11/21 17:20
 */
@RequiredArgsConstructor
@Service
public class VoucherServiceImpl implements VoucherService {
    private final VoucherMapper voucherMapper;


    @Override
    public void save(Voucher voucher) {
        voucherMapper.insert(voucher);
    }
}
