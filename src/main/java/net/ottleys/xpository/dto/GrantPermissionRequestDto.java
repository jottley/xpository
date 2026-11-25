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
import jakarta.validation.constraints.NotNull;
import net.ottleys.xpository.domain.Permission.Action;
import net.ottleys.xpository.domain.Permission.ResourceType;

/**
 * DTO for granting a permission.
 *
 * @param resourceId the resource identifier (element UUID, collection UUID, or "system")
 * @param resourceType the resource type
 * @param action the action to grant
 */
@Schema(description = "Request for granting a permission")
public record GrantPermissionRequestDto(
    @NotBlank(message = "Resource ID is required")
        @Schema(
            description = "Resource identifier",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String resourceId,
    @NotNull(message = "Resource type is required")
        @Schema(description = "Resource type", example = "ELEMENT")
        ResourceType resourceType,
    @NotNull(message = "Action is required")
        @Schema(description = "Action to grant", example = "READ")
        Action action) {}
