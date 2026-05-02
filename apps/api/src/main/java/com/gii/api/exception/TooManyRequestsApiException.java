package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsApiException extends GiiApiException {
  public TooManyRequestsApiException(String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded", message);
  }
}
