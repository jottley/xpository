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
package net.ottleys.xpository.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidJson} annotation.
 *
 * <p>Uses Jackson ObjectMapper to validate that a string contains well-formed JSON. Accepts JSON
 * objects, arrays, and primitives. Null or blank values are considered valid
 * (use @NotNull/@NotBlank separately if those should be rejected).
 */
public class ValidJsonValidator implements ConstraintValidator<ValidJson, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void initialize(ValidJson constraintAnnotation) {
    // No initialization needed
  }

  /**
   * Validates that the value is well-formed JSON.
   *
   * @param value the string to validate
   * @param context the validation context
   * @return true if value is null, blank, or valid JSON; false if invalid JSON
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null and blank values are valid - use @NotNull/@NotBlank to reject them
    if (value == null || value.isBlank()) {
      return true;
    }

    try {
      // Try to parse the JSON - will throw JsonProcessingException if invalid
      objectMapper.readTree(value);
      return true;
    } catch (JsonProcessingException e) {
      // Invalid JSON
      return false;
    }
  }
}
