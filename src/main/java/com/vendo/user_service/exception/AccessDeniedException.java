package com.vendo.user_service.exception;

import org.springframework.security.core.AuthenticationException;

public class AccessDeniedException extends AuthenticationException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
