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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO for creating a new group.
 *
 * @param name the group name (required, unique, max 100 chars)
 * @param description the group description (optional, max 1000 chars)
 * @param inheritPermissions whether to inherit permissions from parent group (default: true)
 * @param parentGroupId the parent group UUID (optional)
 */
@Schema(description = "Request for creating a new group")
public record CreateGroupRequestDto(
    @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Group name cannot exceed 100 characters")
        @Schema(description = "Group name", example = "developers")
        String name,
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Group description", example = "Development team members")
        String description,
    @Schema(description = "Whether to inherit permissions from parent", example = "true")
        Boolean inheritPermissions,
    @Schema(description = "Parent group UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID parentGroupId) {

  /**
   * Constructor with default value for inheritPermissions.
   *
   * @param name the group name
   * @param description the description
   * @param inheritPermissions whether to inherit (defaults to true if null)
   * @param parentGroupId the parent group UUID
   */
  public CreateGroupRequestDto {
    if (inheritPermissions == null) {
      inheritPermissions = true;
    }
  }
}
