package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class BadRequestApiException extends GiiApiException {
  public BadRequestApiException(String message) {
    super(HttpStatus.BAD_REQUEST, "Bad request", message);
  }
}
