package com.vendo.user_service.service.user.common.exception;

public class UserEmailNotVerifiedException extends RuntimeException {
  public UserEmailNotVerifiedException(String message) {
    super(message);
  }
}
