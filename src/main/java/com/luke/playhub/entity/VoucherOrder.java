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
    private int userId;
    private int voucherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
