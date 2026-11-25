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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * <p>Tests error response format, status codes, and field-level validation error handling.
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;
  private HttpServletRequest mockRequest;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getRequestURI()).thenReturn("/api/v1/collections");
  }

  @Test
  void shouldHandleMethodArgumentNotValidException() {
    // Create mock validation errors
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "test");
    bindingResult.addError(
        new FieldError("test", "name", null, false, null, null, "Collection name is required"));
    bindingResult.addError(
        new FieldError(
            "test",
            "type",
            "INVALID",
            false,
            null,
            null,
            "Collection type must be one of: LIST, TREE, GRAPH, STACK"));

    MethodParameter mockParameter = mock(MethodParameter.class);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(mockParameter, bindingResult);

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleValidationException(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(400);
    assertThat(errorResponse.error()).isEqualTo("Bad Request");
    assertThat(errorResponse.message()).isEqualTo("Validation failed for request body");
    assertThat(errorResponse.path()).isEqualTo("/api/v1/collections");
    assertThat(errorResponse.traceId()).isNotNull();
    assertThat(errorResponse.timestamp()).isNotNull();

    // Verify validation errors
    assertThat(errorResponse.errors()).hasSize(2);
    ValidationError nameError = errorResponse.errors().get(0);
    assertThat(nameError.field()).isEqualTo("name");
    assertThat(nameError.rejectedValue()).isNull();
    assertThat(nameError.message()).isEqualTo("Collection name is required");

    ValidationError typeError = errorResponse.errors().get(1);
    assertThat(typeError.field()).isEqualTo("type");
    assertThat(typeError.rejectedValue()).isEqualTo("INVALID");
    assertThat(typeError.message())
        .isEqualTo("Collection type must be one of: LIST, TREE, GRAPH, STACK");
  }

  @Test
  void shouldHandleConstraintViolationException() {
    ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
    Path mockPath = mock(Path.class);

    when(mockViolation.getPropertyPath()).thenReturn(mockPath);
    when(mockPath.toString()).thenReturn("collectionId");
    when(mockViolation.getInvalidValue()).thenReturn("invalid-uuid");
    when(mockViolation.getMessage()).thenReturn("must be a valid UUID");

    ConstraintViolationException exception =
        new ConstraintViolationException("Validation failed", Set.of(mockViolation));

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleConstraintViolation(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(400);
    assertThat(errorResponse.message()).isEqualTo("Validation constraint violated");
    assertThat(errorResponse.errors()).hasSize(1);

    ValidationError error = errorResponse.errors().get(0);
    assertThat(error.field()).isEqualTo("collectionId");
    assertThat(error.rejectedValue()).isEqualTo("invalid-uuid");
  }

  @Test
  void shouldHandleHttpMessageNotReadableException() {
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleHttpMessageNotReadable(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(400);
    assertThat(errorResponse.message()).isEqualTo("Malformed JSON request body");
    assertThat(errorResponse.errors()).isNull(); // No field-level errors for JSON parsing issues
  }

  @Test
  void shouldHandleMethodArgumentTypeMismatchException() {
    MethodParameter mockParameter = mock(MethodParameter.class);
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "invalid-uuid", java.util.UUID.class, "collectionId", mockParameter, null);

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleTypeMismatch(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(400);
    assertThat(errorResponse.message())
        .contains("Invalid value 'invalid-uuid' for parameter 'collectionId'");
    assertThat(errorResponse.message()).contains("Expected type: UUID");
  }

  @Test
  void shouldHandleNoResourceFoundException() {
    NoResourceFoundException exception = mock(NoResourceFoundException.class);

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleNoResourceFound(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(404);
    assertThat(errorResponse.error()).isEqualTo("Not Found");
    assertThat(errorResponse.message()).isEqualTo("The requested resource was not found");
  }

  @Test
  void shouldHandleUnexpectedException() {
    RuntimeException exception = new RuntimeException("Unexpected database error");

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleUnexpectedException(exception, mockRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();

    ErrorResponse errorResponse = response.getBody();
    assertThat(errorResponse.status()).isEqualTo(500);
    assertThat(errorResponse.error()).isEqualTo("Internal Server Error");
    assertThat(errorResponse.message()).contains("An unexpected error occurred");
    assertThat(errorResponse.message()).contains("trace ID");
    assertThat(errorResponse.traceId()).isNotNull();
  }

  @Test
  void shouldIncludeTraceIdInAllErrorResponses() {
    RuntimeException exception = new RuntimeException("Test error");

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleUnexpectedException(exception, mockRequest);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().traceId())
        .isNotNull()
        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }

  @Test
  void shouldIncludeTimestampInAllErrorResponses() {
    RuntimeException exception = new RuntimeException("Test error");

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleUnexpectedException(exception, mockRequest);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().timestamp()).isNotNull();
  }

  @Test
  void shouldIncludeRequestPathInAllErrorResponses() {
    RuntimeException exception = new RuntimeException("Test error");

    ResponseEntity<ErrorResponse> response =
        exceptionHandler.handleUnexpectedException(exception, mockRequest);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().path()).isEqualTo("/api/v1/collections");
  }
}
