package com.vendo.user_service.common.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class MainExceptionHandler {

    private static final String COLON_DELIMITER = ": ";

    private static final String COMMA_DELIMITER = ", ";

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException exception, HttpServletRequest request, HttpServletResponse response) {

        String invalidFields = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(COMMA_DELIMITER));

        return ResponseEntity.badRequest().body(invalidFields);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletResponse response, HttpServletRequest request) {

        String invalidFields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> COLON_DELIMITER + fieldError.getDefaultMessage())
                .collect(Collectors.joining(COMMA_DELIMITER));

        return ResponseEntity.badRequest().body(invalidFields);
    }

}
