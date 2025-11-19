package com.luke.playhub.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
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

    // 加载unlock.lua脚本
    private static final DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    static {
        redisScript.setLocation(new ClassPathResource("unlock.lua"));
        redisScript.setResultType(Long.class);
    }

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
        // 调用lua脚本进行解锁
        redisTemplate.execute(
                redisScript,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().threadId()
        );
    }

    /*@Override
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().threadId();
        // 获取锁中key的标识
        String id = redisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断是不是自己线程的锁
        if (threadId.equals(id)) {
            redisTemplate.delete(KEY_PREFIX + name);
        }
    }*/
}
