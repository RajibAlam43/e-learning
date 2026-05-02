package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityApiException extends GiiApiException {
  public UnprocessableEntityApiException(String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", message);
  }
}
