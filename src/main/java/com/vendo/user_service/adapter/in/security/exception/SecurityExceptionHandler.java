package com.vendo.user_service.adapter.in.security.exception;

import com.vendo.common.exception.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);
    }
}
