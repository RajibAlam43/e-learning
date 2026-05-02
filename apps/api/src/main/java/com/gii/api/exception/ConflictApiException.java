package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public class ConflictApiException extends GiiApiException {
  public ConflictApiException(String message) {
    super(HttpStatus.CONFLICT, "Data conflict", message);
  }
}
