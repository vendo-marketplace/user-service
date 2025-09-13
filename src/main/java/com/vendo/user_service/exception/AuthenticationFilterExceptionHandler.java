package com.vendo.user_service.exception;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AuthenticationFilterExceptionHandler {

    public void handle(Exception e, HttpServletResponse response) {

        if (e instanceof ExpiredJwtException || e instanceof BadCredentialsException) {
            writeExceptionResponse(e.getMessage(), HttpStatus.SC_UNAUTHORIZED, response);
            return;
        }

        if (e instanceof AccessDeniedException) {
            writeExceptionResponse(e.getMessage(), HttpStatus.SC_FORBIDDEN, response);
        }

    }

    private void writeExceptionResponse(Object responseTarget, int statusCode, HttpServletResponse response) {
        try {
            if (responseTarget == null) {
                throw new IllegalArgumentException("Response target cannot be null");
            }

            response.setStatus(statusCode);
            response.getWriter().write(String.valueOf(responseTarget));
        } catch (IOException e) {
            log.error("IOException: ", e);
        }
    }
}
