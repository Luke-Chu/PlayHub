package com.luke.playhub.mapper;

import com.luke.playhub.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Luke
 * @since 2025/11/15 17:57
 */

@Mapper
public interface VoucherOrderMapper {
    void create(VoucherOrder voucherOrder);
}
