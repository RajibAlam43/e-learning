package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedApiException extends GiiApiException {
  public UnauthorizedApiException(String message) {
    super(HttpStatus.UNAUTHORIZED, "Authentication failed", message);
  }
}
