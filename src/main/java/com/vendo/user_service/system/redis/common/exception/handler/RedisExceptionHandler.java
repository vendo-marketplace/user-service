package com.vendo.user_service.system.redis.common.exception.handler;

import com.vendo.common.exception.ExceptionResponse;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RedisExceptionHandler {

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ExceptionResponse> handleRedisValueExpiredException(OtpExpiredException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .type(OtpExpiredException.class.getSimpleName())
                .code(HttpStatus.GONE.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.GONE).body(exceptionResponse);
    }
}
