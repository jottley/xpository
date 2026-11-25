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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ValidJson annotation and ValidJsonValidator.
 *
 * <p>Tests validation of JSON strings including valid JSON objects, arrays, primitives, and invalid
 * JSON.
 */
class ValidJsonValidatorTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void shouldAcceptValidJsonObject() {
    // Given: Valid JSON object
    TestDto dto = new TestDto("{\"key\": \"value\"}");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptValidJsonArray() {
    // Given: Valid JSON array
    TestDto dto = new TestDto("[1, 2, 3]");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptValidJsonString() {
    // Given: Valid JSON string
    TestDto dto = new TestDto("\"hello\"");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptValidJsonNumber() {
    // Given: Valid JSON number
    TestDto dto = new TestDto("42");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptValidJsonBoolean() {
    // Given: Valid JSON boolean
    TestDto dto = new TestDto("true");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptValidJsonNull() {
    // Given: Valid JSON null
    TestDto dto = new TestDto("null");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptComplexNestedJson() {
    // Given: Complex nested JSON
    TestDto dto =
        new TestDto(
            "{\"users\": [{\"name\": \"Alice\", \"age\": 30}, {\"name\": \"Bob\", \"age\": 25}]}");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptNullValue() {
    // Given: Null value (should be valid - use @NotNull to reject nulls)
    TestDto dto = new TestDto(null);

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptBlankString() {
    // Given: Blank string (should be valid - use @NotBlank to reject blanks)
    TestDto dto = new TestDto("   ");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: No violations
    assertThat(violations).isEmpty();
  }

  @Test
  void shouldRejectInvalidJson() {
    // Given: Invalid JSON (missing closing brace)
    TestDto dto = new TestDto("{\"key\": \"value\"");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: One violation
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Must be valid JSON");
  }

  @Test
  void shouldRejectInvalidJsonWithTrailingComma() {
    // Given: Invalid JSON (trailing comma)
    TestDto dto = new TestDto("{\"key\": \"value\",}");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: One violation
    assertThat(violations).hasSize(1);
  }

  @Test
  void shouldRejectInvalidJsonWithUnquotedKeys() {
    // Given: Invalid JSON (unquoted key)
    TestDto dto = new TestDto("{key: \"value\"}");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: One violation
    assertThat(violations).hasSize(1);
  }

  @Test
  void shouldRejectPlainText() {
    // Given: Plain text (not JSON)
    TestDto dto = new TestDto("This is not JSON");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: One violation
    assertThat(violations).hasSize(1);
  }

  @Test
  void shouldRejectMalformedArray() {
    // Given: Malformed array
    TestDto dto = new TestDto("[1, 2, 3");

    // When: Validating
    Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

    // Then: One violation
    assertThat(violations).hasSize(1);
  }

  // Test DTO class with @ValidJson annotation
  private static class TestDto {
    @ValidJson private String jsonField;

    TestDto(String jsonField) {
      this.jsonField = jsonField;
    }
  }
}
