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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user response.
 *
 * @param userId the public user UUID
 * @param username the username
 * @param email the email address
 * @param displayName the display name
 * @param enabled whether the account is enabled
 * @param accountLocked whether the account is locked
 * @param accountExpired whether the account is expired
 * @param credentialsExpired whether the credentials are expired
 * @param lastLoginDate the last login timestamp
 * @param createdDate the creation timestamp
 * @param createdBy the username who created this user
 * @param modifiedDate the last modification timestamp
 * @param modifiedBy the username who last modified this user
 */
@Schema(description = "User response")
public record UserResponseDto(
    @Schema(description = "Public user UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,
    @Schema(description = "Username", example = "john.doe") String username,
    @Schema(description = "Email address", example = "john.doe@example.com") String email,
    @Schema(description = "Display name", example = "John Doe") String displayName,
    @Schema(description = "Account enabled", example = "true") boolean enabled,
    @Schema(description = "Account locked", example = "false") boolean accountLocked,
    @Schema(description = "Account expired", example = "false") boolean accountExpired,
    @Schema(description = "Credentials expired", example = "false") boolean credentialsExpired,
    @Schema(description = "Last login timestamp") LocalDateTime lastLoginDate,
    @Schema(description = "Creation timestamp") LocalDateTime createdDate,
    @Schema(description = "Creator username", example = "admin") String createdBy,
    @Schema(description = "Last modification timestamp") LocalDateTime modifiedDate,
    @Schema(description = "Last modifier username", example = "admin") String modifiedBy) {}
