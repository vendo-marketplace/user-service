package com.vendo.user_service.service.user.common.exception;

public class UserAlreadyActivatedException extends RuntimeException {
  public UserAlreadyActivatedException(String message) {
    super(message);
  }
}
