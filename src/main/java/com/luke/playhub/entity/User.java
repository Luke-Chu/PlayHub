package com.luke.playhub.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Luke
 * @since 2025/11/15 17:45
 */
@Data
public class User {
    private int id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
