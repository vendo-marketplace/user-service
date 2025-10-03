package com.vendo.user_service.security.common.exception.handler;

import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.security.common.exception.InvalidTokenException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AuthenticationFilterExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: ", e);
        return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("InvalidTokenException: ", e);
        return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<String> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("ExpiredJwtException: ", e);
        return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("Token has expired");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<String> handleJwtException(JwtException e) {
        log.warn("JwtException: ", e);
        return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(e.getMessage());
    }
}
