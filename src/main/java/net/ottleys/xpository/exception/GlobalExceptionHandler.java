/*
 * Copyright 2024-2026 Jared Ottley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ottleys.xpository.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for REST API controllers.
 *
 * <p>Provides consistent error response format across all REST API endpoints using {@link
 * RestControllerAdvice}. Handles validation errors, common HTTP errors, and unexpected exceptions.
 * All error responses follow the standard format documented in DESIGN.md.
 *
 * <p>Generates trace IDs for correlation with server logs and includes them in error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TRACE_ID_KEY = "traceId";

  /**
   * Handles Bean Validation exceptions from @Valid on request bodies.
   *
   * <p>Returns 400 Bad Request with field-level validation errors.
   *
   * @param ex the validation exception
   * @param request the HTTP request
   * @return error response with validation details
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    List<ValidationError> validationErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error ->
                new ValidationError(
                    error.getField(), error.getRejectedValue(), error.getDefaultMessage()))
            .toList();

    logger.warn(
        "Validation failed for request: {} {} - {} errors [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        validationErrors.size(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.withValidationErrors(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed for request body",
            request.getRequestURI(),
            traceId,
            validationErrors);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handles constraint violations from @Valid on path variables or request parameters.
   *
   * <p>Returns 400 Bad Request with constraint violation details.
   *
   * @param ex the constraint violation exception
   * @param request the HTTP request
   * @return error response with validation details
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    List<ValidationError> validationErrors =
        ex.getConstraintViolations().stream()
            .map(violation ->
                new ValidationError(
                    violation.getPropertyPath().toString(),
                    violation.getInvalidValue(),
                    violation.getMessage()))
            .toList();

    logger.warn(
        "Constraint violation for request: {} {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.withValidationErrors(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation constraint violated",
            request.getRequestURI(),
            traceId,
            validationErrors);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handles malformed JSON or unreadable request bodies.
   *
   * <p>Returns 400 Bad Request with message about malformed input.
   *
   * @param ex the message not readable exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    logger.warn(
        "Malformed request body: {} {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Malformed JSON request body",
            request.getRequestURI(),
            traceId);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handles type mismatch errors (e.g., invalid UUID format in path variable).
   *
   * <p>Returns 400 Bad Request with message about invalid parameter type.
   *
   * @param ex the type mismatch exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    String message =
        String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

    logger.warn(
        "Type mismatch for parameter '{}': {} [traceId={}]", ex.getName(), ex.getValue(), traceId);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI(),
            traceId);

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Handles resource not found errors (404).
   *
   * <p>Returns 404 Not Found.
   *
   * @param ex the no resource found exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    logger.warn(
        "Resource not found: {} {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            "The requested resource was not found",
            request.getRequestURI(),
            traceId);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /**
   * Handles business logic validation errors from service layer.
   *
   * <p>Returns 422 Unprocessable Entity when business rules are violated (e.g., element type does
   * not exist, circular reference detected, system collection cannot be deleted).
   *
   * @param ex the business logic exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler({
    IllegalArgumentException.class,
    IllegalStateException.class,
    BusinessValidationException.class
  })
  public ResponseEntity<ErrorResponse> handleBusinessLogicException(
      RuntimeException ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    logger.warn(
        "Business logic validation failed: {} {} - {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        ex.getMessage(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            422,
            "Unprocessable Entity",
            ex.getMessage(),
            request.getRequestURI(),
            traceId);

    return ResponseEntity.status(422).body(errorResponse);
  }

  /**
   * Handles optimistic locking failures (concurrent updates to same entity).
   *
   * <p>Returns 409 Conflict when the version field doesn't match, indicating another user modified
   * the entity between read and update.
   *
   * @param ex the optimistic locking exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler({
    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
    jakarta.persistence.OptimisticLockException.class
  })
  public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
      Exception ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    logger.warn(
        "Optimistic locking failure: {} {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        traceId);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "The resource was modified by another user. Please refresh and try again.",
            request.getRequestURI(),
            traceId);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /**
   * Handles all unexpected exceptions as 500 Internal Server Error.
   *
   * <p>Logs full stack trace and returns generic error message to client.
   *
   * @param ex the unexpected exception
   * @param request the HTTP request
   * @return error response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
      Exception ex, HttpServletRequest request) {
    String traceId = getOrCreateTraceId();

    logger.error(
        "Unexpected error handling request: {} {} [traceId={}]",
        request.getMethod(),
        request.getRequestURI(),
        traceId,
        ex);

    ErrorResponse errorResponse =
        ErrorResponse.of(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred. Please contact support with trace ID: " + traceId,
            request.getRequestURI(),
            traceId);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /**
   * Gets or creates a trace ID for correlating error responses with server logs.
   *
   * <p>First checks MDC for existing trace ID (set by logging filter). If not present, generates a
   * new UUID.
   *
   * @return trace ID string
   */
  private String getOrCreateTraceId() {
    String traceId = MDC.get(TRACE_ID_KEY);
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
      MDC.put(TRACE_ID_KEY, traceId);
    }
    return traceId;
  }
}
