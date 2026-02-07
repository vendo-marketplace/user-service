package com.vendo.user_service.adapter.out.user.common.exception.handler;

import com.vendo.common.exception.ExceptionResponse;
import com.vendo.security.common.exception.UserBlockedException;
import com.vendo.security.common.exception.UserEmailNotVerifiedException;
import com.vendo.security.common.exception.UserIsUnactiveException;
import com.vendo.user_service.adapter.out.user.common.exception.UserAlreadyActivatedException;
import com.vendo.user_service.adapter.out.user.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.domain.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exceptionResponse);
    }

    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<ExceptionResponse> handleUserBlockedException(UserBlockedException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);
    }

    @ExceptionHandler(UserAlreadyActivatedException.class)
    public ResponseEntity<ExceptionResponse> handleUserAlreadyActivatedException(UserAlreadyActivatedException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exceptionResponse);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(UserEmailNotVerifiedException.class)
    public ResponseEntity<ExceptionResponse> handleUserEmailNotVerifiedException(UserEmailNotVerifiedException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);
    }

    @ExceptionHandler(UserIsUnactiveException.class)
    public ResponseEntity<ExceptionResponse> handleUserIsUnactiveException(UserIsUnactiveException e, HttpServletRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(e.getMessage())
                .code(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);
    }

}
