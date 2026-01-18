package com.vendo.user_service.adapter.out.user.common.exception;

public class UserAlreadyActivatedException extends RuntimeException {
  public UserAlreadyActivatedException(String message) {
    super(message);
  }
}
