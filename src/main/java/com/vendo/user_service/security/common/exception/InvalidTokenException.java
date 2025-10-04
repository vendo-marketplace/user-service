package com.vendo.user_service.security.common.exception;

import io.jsonwebtoken.JwtException;

// TODO Refactor #16: Move to common
public class InvalidTokenException extends JwtException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
