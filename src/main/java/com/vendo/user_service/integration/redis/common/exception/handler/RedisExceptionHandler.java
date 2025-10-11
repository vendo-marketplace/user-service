package com.vendo.user_service.integration.redis.common.exception.handler;

import com.vendo.user_service.integration.redis.common.exception.RedisValueExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RedisExceptionHandler {

    @ExceptionHandler(RedisValueExpiredException.class)
    public ResponseEntity<String> handleRedisValueExpiredException(RedisValueExpiredException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(e.getMessage());
    }

}
