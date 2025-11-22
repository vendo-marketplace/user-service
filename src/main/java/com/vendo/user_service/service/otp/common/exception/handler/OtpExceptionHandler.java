package com.vendo.user_service.service.otp.common.exception.handler;

import com.vendo.common.exception.ExceptionResponse;
import com.vendo.integration.redis.common.exception.OtpExpiredException;
import com.vendo.user_service.service.otp.common.exception.InvalidOtpException;
import com.vendo.user_service.service.otp.common.exception.OtpAlreadySentException;
import com.vendo.user_service.service.otp.common.exception.TooManyOtpRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OtpExceptionHandler {

    @ExceptionHandler(OtpAlreadySentException.class)
    public ResponseEntity<ExceptionResponse> handlePasswordRecoveryEventAlreadySentException(OtpAlreadySentException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exceptionResponse);
    }

    @ExceptionHandler(TooManyOtpRequestsException.class)
    public ResponseEntity<ExceptionResponse> handleOtpTooManyRequestsException(TooManyOtpRequestsException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(exceptionResponse);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ExceptionResponse> handleInvalidOtpForEmailException(InvalidOtpException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.GONE.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.GONE).body(exceptionResponse);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ExceptionResponse> handleRedisValueExpiredException(OtpExpiredException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.GONE.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.GONE).body(exceptionResponse);
    }

}
