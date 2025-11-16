package com.luke.playhub.dto;

import java.io.Serializable;

/**
 * @author Luke
 * @since 2025/11/16 15:21
 */
public class Result<T> implements Serializable {
    private boolean success;
    private String errorMsg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.success = true;
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String errorMsg) {
        Result<T> result = new Result<>();
        result.success = false;
        result.errorMsg = errorMsg;
        return result;
    }
}
