package com.proofvault.authserver.controller;

import com.proofvault.authserver.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
    problem.setTitle("Invalid request");
    enrich(problem, request);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
    problem.setTitle("Invalid request");
    enrich(problem, request);
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getReason());
    problem.setTitle(status.getReasonPhrase());
    enrich(problem, request);
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required.");
    problem.setTitle("Unauthorized");
    enrich(problem, request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    problem.setTitle("Forbidden");
    enrich(problem, request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected auth server error.");
    problem.setTitle("Internal server error");
    enrich(problem, request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  private void enrich(ProblemDetail problem, HttpServletRequest request) {
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("requestId", request.getAttribute(RequestIdFilter.REQUEST_ID_MDC_KEY));
  }
}
