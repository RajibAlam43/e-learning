package com.gii.api.controller;

import com.gii.api.exception.GiiApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GiiControllerAdvice {

  @ExceptionHandler(GiiApiException.class)
  public ProblemDetail handleGiiApiException(GiiApiException ex, HttpServletRequest request) {
    return problem(ex.getStatus(), ex.getTitle(), ex.getMessage(), request);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    String detail = ex.getReason() == null ? "Request failed" : ex.getReason();
    return problem(
        HttpStatus.valueOf(ex.getStatusCode().value()), "Request failed", detail, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.FORBIDDEN,
        "Access denied",
        "You do not have permission to perform this action.",
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream().map(this::toFieldError).toList();

    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "One or more fields are invalid.",
            request);

    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<Map<String, String>> errors =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
            .toList();

    ProblemDetail problem =
        problem(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "One or more request parameters are invalid.",
            request);

    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Invalid parameter",
        "Invalid value for parameter: " + ex.getName(),
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Malformed request",
        "Request body is missing or malformed.",
        request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ProblemDetail handleMissingRequestParam(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Missing parameter",
        "Missing required parameter: " + ex.getParameterName(),
        request);
  }

  @ExceptionHandler(MissingRequestCookieException.class)
  public ProblemDetail handleMissingCookie(
      MissingRequestCookieException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Missing cookie",
        "Missing required cookie: " + ex.getCookieName(),
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT, "Data conflict", "The request conflicts with existing data.", request);
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "Something went wrong. Please try again later.",
        request);
  }

  private ProblemDetail problem(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setType(URI.create("https://api.gii.com/problems/" + status.value()));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now());
    return problem;
  }

  private Map<String, String> toFieldError(FieldError error) {
    return Map.of(
        "field",
        error.getField(),
        "message",
        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
  }
}
