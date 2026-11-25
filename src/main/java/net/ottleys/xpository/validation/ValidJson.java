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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation annotation to validate that a String field contains well-formed JSON.
 *
 * <p>Validates that the string can be parsed as valid JSON (object, array, or primitive). Null
 * values are considered valid - use @NotNull separately if null should be rejected.
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;ValidJson(message = "Configuration must be valid JSON")
 * private String configurationJson;
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidJsonValidator.class)
@Documented
public @interface ValidJson {

  /**
   * Validation error message.
   *
   * @return the error message template
   */
  String message() default "Must be valid JSON";

  /**
   * Validation groups.
   *
   * @return the groups the constraint belongs to
   */
  Class<?>[] groups() default {};

  /**
   * Payload for clients.
   *
   * @return the payload associated with the constraint
   */
  Class<? extends Payload>[] payload() default {};
}
