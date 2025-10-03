package com.vendo.user_service.common.exception;

public class RedisTokenExpiredException extends RuntimeException {
    public RedisTokenExpiredException(String message) {
        super(message);
    }
}
