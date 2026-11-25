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
import net.ottleys.xpository.domain.Permission.Action;
import net.ottleys.xpository.domain.Permission.ResourceType;
import net.ottleys.xpository.domain.Permission.SubjectType;

/**
 * DTO for permission response.
 *
 * @param subjectId the subject identifier
 * @param subjectType the subject type
 * @param resourceId the resource identifier
 * @param resourceType the resource type
 * @param action the permitted action
 * @param inherited whether this permission was inherited
 * @param createdDate the creation timestamp
 * @param createdBy the username who created this permission
 */
@Schema(description = "Permission response")
public record PermissionResponseDto(
    @Schema(description = "Subject identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String subjectId,
    @Schema(description = "Subject type", example = "USER") SubjectType subjectType,
    @Schema(description = "Resource identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        String resourceId,
    @Schema(description = "Resource type", example = "ELEMENT") ResourceType resourceType,
    @Schema(description = "Permitted action", example = "READ") Action action,
    @Schema(description = "Inherited flag", example = "false") boolean inherited,
    @Schema(description = "Creation timestamp") LocalDateTime createdDate,
    @Schema(description = "Creator username", example = "admin") String createdBy) {}
