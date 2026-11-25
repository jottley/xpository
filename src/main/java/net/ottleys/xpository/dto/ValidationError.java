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

/**
 * Represents a field-level validation error in an error response.
 *
 * <p>Used within {@link ErrorResponse} to provide detailed information about which fields failed
 * validation and why. Helps clients identify and correct specific validation issues.
 *
 * @param field name of the field that failed validation
 * @param rejectedValue the invalid value that was provided
 * @param message specific validation error message
 */
@Schema(description = "Field-level validation error details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationError(
    @Schema(description = "Field name that failed validation", example = "name") String field,
    @Schema(description = "The invalid value that was provided", example = "") Object rejectedValue,
    @Schema(description = "Validation error message", example = "Collection name is required")
        String message) {}
