package com.luke.playhub.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Luke
 * @since 2025/11/19 16:06
 */
public class SimpleRedisLock implements ILock {

    private final String name;
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "lock:";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.redisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = String.valueOf(Thread.currentThread().threadId());
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        redisTemplate.delete(KEY_PREFIX + name);
    }
}
