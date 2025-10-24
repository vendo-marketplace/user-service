package com.vendo.user_service.common.exception;

public class InvalidOtpForEmailException extends RuntimeException {
    public InvalidOtpForEmailException(String message) {
        super(message);
    }
}
