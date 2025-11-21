package com.luke.playhub.service.impl;

import com.luke.playhub.entity.Voucher;
import com.luke.playhub.mapper.VoucherMapper;
import com.luke.playhub.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luke
 * @since 2025/11/21 17:20
 */
@RequiredArgsConstructor
@Service
public class VoucherServiceImpl implements VoucherService {
    private final VoucherMapper voucherMapper;
    private final StringRedisTemplate stringRedisTemplate;


    @Transactional
    @Override
    public void save(Voucher voucher) {
        voucherMapper.insert(voucher);
        // 保存秒杀信息到Redis
        stringRedisTemplate.opsForValue().set("stock:voucher:" + voucher.getId(), String.valueOf(voucher.getStock()));
    }
}
