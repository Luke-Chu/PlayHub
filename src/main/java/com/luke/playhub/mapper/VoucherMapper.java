package com.luke.playhub.mapper;

import com.luke.playhub.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * @author Luke
 * @since 2025/11/15 17:57
 */

@Mapper
public interface VoucherMapper {
    Voucher selectById(long voucherId);

    @Update("update voucher set stock = stock - 1 where id = #{voucherId}")
    int decreaseStockWithOversold(long voucherId);
}
