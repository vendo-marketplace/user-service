package com.vendo.user_service.common.exception;

public class OtpTooManyRequestsException extends RuntimeException {
  public OtpTooManyRequestsException(String message) {
    super(message);
  }
}
