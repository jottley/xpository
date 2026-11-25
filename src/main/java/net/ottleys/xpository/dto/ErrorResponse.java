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
package net.ottleys.xpository.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Standard error response record for REST API error responses.
 *
 * <p>Provides consistent error response format across all REST API endpoints. Includes HTTP status
 * information, error details, and optional field-level validation errors. Follows the error
 * response format documented in DESIGN.md.
 *
 * @param timestamp ISO 8601 timestamp when error occurred
 * @param status HTTP status code
 * @param error HTTP status text
 * @param message human-readable error summary
 * @param path request path that caused the error
 * @param traceId correlation ID for log tracking (optional)
 * @param errors array of field-specific validation errors (optional)
 */
@Schema(description = "Standard error response for all API errors")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(
            description = "ISO 8601 timestamp when error occurred",
            example = "2026-06-05T04:35:24.708434Z")
        Instant timestamp,
    @Schema(description = "HTTP status code", example = "400") int status,
    @Schema(description = "HTTP status text", example = "Bad Request") String error,
    @Schema(
            description = "Human-readable error message",
            example = "Validation failed for one or more fields")
        String message,
    @Schema(description = "Request path that caused the error", example = "/api/v1/collections")
        String path,
    @Schema(
            description = "Trace ID for log correlation",
            example = "0e51903d-124a-4352-aaf3-5ab0aa8e4973")
        String traceId,
    @Schema(description = "Field-level validation errors (only present for validation failures)")
        List<ValidationError> errors) {

  /**
   * Creates an error response without field-level validation errors.
   *
   * @param timestamp when the error occurred
   * @param status HTTP status code
   * @param error HTTP status text
   * @param message error message
   * @param path request path
   * @param traceId trace ID for correlation
   * @return ErrorResponse with no validation errors
   */
  public static ErrorResponse of(
      Instant timestamp, int status, String error, String message, String path, String traceId) {
    return new ErrorResponse(timestamp, status, error, message, path, traceId, null);
  }

  /**
   * Creates an error response with field-level validation errors.
   *
   * @param timestamp when the error occurred
   * @param status HTTP status code
   * @param error HTTP status text
   * @param message error message
   * @param path request path
   * @param traceId trace ID for correlation
   * @param errors list of field validation errors
   * @return ErrorResponse with validation errors
   */
  public static ErrorResponse withValidationErrors(
      Instant timestamp,
      int status,
      String error,
      String message,
      String path,
      String traceId,
      List<ValidationError> errors) {
    return new ErrorResponse(timestamp, status, error, message, path, traceId, errors);
  }
}
