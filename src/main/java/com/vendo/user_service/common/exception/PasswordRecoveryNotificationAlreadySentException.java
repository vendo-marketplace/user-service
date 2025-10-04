package com.vendo.user_service.common.exception;

public class PasswordRecoveryNotificationAlreadySentException extends RuntimeException {
    public PasswordRecoveryNotificationAlreadySentException(String message) {
        super(message);
    }
}
