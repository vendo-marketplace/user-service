package com.vendo.user_service.common.exception;

public class RedisValueExpiredException extends RuntimeException {
    public RedisValueExpiredException(String message) {
        super(message);
    }
}
