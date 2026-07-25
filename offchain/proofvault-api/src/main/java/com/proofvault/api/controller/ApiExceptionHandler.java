package com.proofvault.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.slf4j.MDC;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> badRequest(
    IllegalArgumentException exception,
    HttpServletRequest request
  ) {
    return problem(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage(), request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> conflict(
    IllegalStateException exception,
    HttpServletRequest request
  ) {
    return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validationError(
    MethodArgumentNotValidException exception,
    HttpServletRequest request
  ) {
    String message = exception.getBindingResult().getFieldErrors().stream()
      .findFirst()
      .map(error -> error.getDefaultMessage() == null ? "Invalid request" : error.getDefaultMessage())
      .orElse("Invalid request");

    return problem(HttpStatus.BAD_REQUEST, "Validation failed", message, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> constraintViolation(
    ConstraintViolationException exception,
    HttpServletRequest request
  ) {
    String message = exception.getConstraintViolations().stream()
      .findFirst()
      .map(violation -> violation.getMessage() == null ? "Invalid request" : violation.getMessage())
      .orElse("Invalid request");

    return problem(HttpStatus.BAD_REQUEST, "Validation failed", message, request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetail> unauthorized(
    AuthenticationException exception,
    HttpServletRequest request
  ) {
    return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", exception.getMessage(), request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> forbidden(
    AccessDeniedException exception,
    HttpServletRequest request
  ) {
    return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> unexpected(Exception exception, HttpServletRequest request) {
    return problem(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Internal server error",
      "An unexpected error occurred.",
      request
    );
  }

  private ResponseEntity<ProblemDetail> problem(
    HttpStatus status,
    String title,
    String detail,
    HttpServletRequest request
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));

    String requestId = MDC.get("requestId");
    if (requestId != null && !requestId.isBlank()) {
      problem.setProperty("requestId", requestId);
    }

    return ResponseEntity.status(status).body(problem);
  }
}
