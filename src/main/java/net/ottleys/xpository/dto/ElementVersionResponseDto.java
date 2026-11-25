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
 * Response DTO for element version snapshots.
 *
 * <p>Represents a historical version of an element, including the version numbers, properties at
 * that point in time, and audit information.
 */
@Schema(description = "Historical version snapshot of an element")
public record ElementVersionResponseDto(
    @Schema(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID elementId,
    @Schema(description = "Major version number", example = "1") Integer majorVersion,
    @Schema(description = "Minor version number", example = "5") Integer minorVersion,
    @Schema(description = "Version string (major.minor)", example = "1.5") String versionString,
    @Schema(description = "Element type name", example = "xpository:core:standard")
        String elementTypeName,
    @Schema(
            description = "Element properties as JSON at this version",
            example = "{\"title\": \"My Document\", \"status\": \"draft\"}")
        String propertiesJson,
    @Schema(
            description = "Optional comment describing changes in this version",
            example = "Updated status to draft",
            nullable = true)
        String versionComment,
    @Schema(description = "Timestamp when this version was created") LocalDateTime createdDate,
    @Schema(description = "User who created this version", example = "john.doe")
        String createdBy) {}
