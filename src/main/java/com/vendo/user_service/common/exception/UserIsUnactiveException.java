package com.vendo.user_service.common.exception;

public class UserIsUnactiveException extends RuntimeException {
    public UserIsUnactiveException(String message) {
        super(message);
    }
}
