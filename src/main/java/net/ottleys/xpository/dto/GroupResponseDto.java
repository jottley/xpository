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
 * DTO for group response.
 *
 * @param groupId the public group UUID
 * @param name the group name
 * @param description the group description
 * @param systemGroup whether this is a system group
 * @param inheritPermissions whether permissions are inherited from parent
 * @param parentGroupId the parent group UUID
 * @param memberCount the number of members in the group
 * @param createdDate the creation timestamp
 * @param createdBy the username who created this group
 * @param modifiedDate the last modification timestamp
 * @param modifiedBy the username who last modified this group
 */
@Schema(description = "Group response")
public record GroupResponseDto(
    @Schema(description = "Public group UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID groupId,
    @Schema(description = "Group name", example = "developers") String name,
    @Schema(description = "Group description", example = "Development team members")
        String description,
    @Schema(description = "System group flag", example = "false") boolean systemGroup,
    @Schema(description = "Inherit permissions from parent", example = "true")
        boolean inheritPermissions,
    @Schema(description = "Parent group UUID") UUID parentGroupId,
    @Schema(description = "Number of members", example = "5") long memberCount,
    @Schema(description = "Creation timestamp") LocalDateTime createdDate,
    @Schema(description = "Creator username", example = "admin") String createdBy,
    @Schema(description = "Last modification timestamp") LocalDateTime modifiedDate,
    @Schema(description = "Last modifier username", example = "admin") String modifiedBy) {}
