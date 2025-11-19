package com.luke.playhub.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

/**
 * @author Luke
 * @since 2025/11/19 16:06
 */
public class SimpleRedisLock implements ILock {

    private final String name;
    private final StringRedisTemplate redisTemplate;
    private final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID() + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.redisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = String.valueOf(Thread.currentThread().threadId());
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, ID_PREFIX + threadId, timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().threadId();
        // 获取锁中key的标识
        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断是不是自己线程的锁
        if (threadId.equals(id)) {
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
