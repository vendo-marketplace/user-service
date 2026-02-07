package com.vendo.user_service.domain.user.exception;

// TODO move to user-common and from auth-service
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
