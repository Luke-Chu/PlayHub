package com.luke.playhub.utils;

/**
 * @author Luke
 * @since 2025/11/19 16:06
 */
public interface ILock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
