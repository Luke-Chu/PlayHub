package com.luke.playhub.mapper;

import com.luke.playhub.entity.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author Luke
 * @since 2025/11/15 17:57
 */

@Mapper
public interface VoucherOrderMapper {
    void create(VoucherOrder voucherOrder);

    @Select("select * from voucher_order where user_id = #{userId} and voucher_id = #{voucherId} limit 1")
    VoucherOrder findByUserIdAndVoucherId(Long userId, long voucherId);

    @Select("select * from voucher_order where id = #{voucherOrderId}")
    VoucherOrder selectById(Long voucherOrderId);
}
