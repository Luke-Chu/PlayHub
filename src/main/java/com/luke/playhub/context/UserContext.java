package com.luke.playhub.context;

/**
 * @author Luke
 * @since 2025/11/16 20:10
 */
public class UserContext {
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getUserId() {
        return threadLocal.get();
    }

    public static void removeUserId() {
        threadLocal.remove();
    }
}
