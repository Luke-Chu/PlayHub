package com.luke.playhub.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Luke
 * @since 2025/11/15 17:54
 */

@Data
public class VoucherOrder {
    private long id;
    private long userId;
    private long voucherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
