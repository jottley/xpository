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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new user.
 *
 * @param username the username (required, unique, max 100 chars)
 * @param email the email address (required, unique, valid email format)
 * @param displayName the display name (optional, max 255 chars)
 * @param password the password (required, min 8 chars)
 * @param enabled whether the user account is enabled (default: true)
 */
@Schema(description = "Request for creating a new user")
public record CreateUserRequestDto(
    @NotBlank(message = "Username is required")
        @Size(max = 100, message = "Username cannot exceed 100 characters")
        @Schema(description = "Username for login", example = "john.doe")
        String username,
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
        @Schema(description = "Display name for UI", example = "John Doe")
        String displayName,
    @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Schema(description = "Password (min 8 characters)", example = "SecurePass123!")
        String password,
    @Schema(description = "Whether the user account is enabled", example = "true")
        Boolean enabled) {

  /**
   * Constructor with default value for enabled.
   *
   * @param username the username
   * @param email the email
   * @param displayName the display name
   * @param password the password
   * @param enabled whether enabled (defaults to true if null)
   */
  public CreateUserRequestDto {
    if (enabled == null) {
      enabled = true;
    }
  }
}
