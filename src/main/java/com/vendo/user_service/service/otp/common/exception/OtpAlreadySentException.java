package com.vendo.user_service.service.otp.common.exception;

public class OtpAlreadySentException extends RuntimeException {
    public OtpAlreadySentException(String message) {
        super(message);
    }
}
