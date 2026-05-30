package com.vendo.user_service.adapter.spring.in.exception;

import com.vendo.security_lib.exception.response.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
class CommonExceptionHandler {

    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class})
    protected ResponseEntity<ExceptionResponse> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error(e.getMessage());
        return ResponseEntity.internalServerError().body(forInternalError(request));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ExceptionResponse> handleException(Exception e, HttpServletRequest request) {
        log.error(e.getMessage());
        return ResponseEntity.internalServerError().body(forInternalError(request));
    }

    private ExceptionResponse forInternalError(HttpServletRequest request) {
        return ExceptionResponse.builder()
                .message("Internal server error.")
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI()).build();
    }
}
