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
import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing user.
 *
 * @param username the username (optional, max 100 chars)
 * @param email the email address (optional, valid email format)
 * @param displayName the display name (optional, max 255 chars)
 * @param enabled whether the user account is enabled
 * @param accountLocked whether the account is locked
 * @param accountExpired whether the account is expired
 * @param credentialsExpired whether the credentials are expired
 */
@Schema(description = "Request for updating an existing user")
public record UpdateUserRequestDto(
    @Size(max = 100, message = "Username cannot exceed 100 characters")
        @Schema(description = "Username for login", example = "john.doe")
        String username,
    @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,
    @Size(max = 255, message = "Display name cannot exceed 255 characters")
        @Schema(description = "Display name for UI", example = "John Doe")
        String displayName,
    @Schema(description = "Whether the user account is enabled", example = "true") Boolean enabled,
    @Schema(description = "Whether the account is locked", example = "false") Boolean accountLocked,
    @Schema(description = "Whether the account is expired", example = "false")
        Boolean accountExpired,
    @Schema(description = "Whether the credentials are expired", example = "false")
        Boolean credentialsExpired) {}
