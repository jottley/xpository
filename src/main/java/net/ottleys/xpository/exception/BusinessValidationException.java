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

/**
 * Exception thrown when business validation rules are violated.
 *
 * <p>Used to indicate validation errors that are not simple input validation (handled by Bean
 * Validation) but rather complex business rules that require service-layer checks. Examples
 * include: collection type mismatches, relationship constraints, circular references, or
 * cross-entity validation.
 */
public class BusinessValidationException extends RuntimeException {

  public BusinessValidationException(String message) {
    super(message);
  }

  public BusinessValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
