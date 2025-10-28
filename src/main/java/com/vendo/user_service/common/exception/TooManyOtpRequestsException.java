package com.vendo.user_service.common.exception;

public class TooManyOtpRequestsException extends RuntimeException {
  public TooManyOtpRequestsException(String message) {
    super(message);
  }
}
