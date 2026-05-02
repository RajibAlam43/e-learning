package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenApiException extends GiiApiException {
  public ForbiddenApiException(String message) {
    super(HttpStatus.FORBIDDEN, "Forbidden", message);
  }
}
