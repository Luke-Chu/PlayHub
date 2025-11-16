package com.luke.playhub.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Luke
 * @since 2025/11/15 17:53
 */

@Data
public class Voucher {
    private long id;
    private long shopId;
    private int stock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
