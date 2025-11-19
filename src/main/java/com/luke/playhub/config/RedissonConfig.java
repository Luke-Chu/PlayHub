package com.luke.playhub.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luke
 * @since 2025/11/19 19:07
 */
@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        // 创建Redisson配置
        Config config = new Config();
        // 配置单节点模式的Redis服务器地址
        config.useSingleServer().setAddress("redis://127.0.0.1:6378");
        // 创建Redisson客户端实例
        return Redisson.create(config);
    }
}
