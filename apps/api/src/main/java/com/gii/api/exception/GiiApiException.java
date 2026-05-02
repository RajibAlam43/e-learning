package com.gii.api.exception;

import org.springframework.http.HttpStatus;

public abstract class GiiApiException extends RuntimeException {

  private final HttpStatus status;
  private final String title;

  protected GiiApiException(HttpStatus status, String title, String message) {
    super(message);
    this.status = status;
    this.title = title;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getTitle() {
    return title;
  }
}
