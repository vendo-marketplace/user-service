package com.vendo.user_service.common.exception.handler;

import com.vendo.common.exception.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

import static com.vendo.common.constants.Delimiters.COLON_DELIMITER;
import static com.vendo.common.constants.Delimiters.COMMA_DELIMITER;

@ControllerAdvice
public class MainExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {

        String invalidFields = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(COMMA_DELIMITER));

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(invalidFields)
                .type("ConstraintViolationException")
                .code(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(exceptionResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {

        String invalidFields = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + COLON_DELIMITER + fieldError.getDefaultMessage())
                .collect(Collectors.joining(COMMA_DELIMITER));

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .message(invalidFields)
                .type("MethodArgumentNotValidException")
                .code(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(exceptionResponse);
    }

}
