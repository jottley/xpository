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
 * Response DTO for element relationships in GRAPH collections.
 *
 * <p>Represents a directed relationship from source element to target element with type and
 * optional metadata. Only public UUIDs are exposed, never internal Long IDs.
 */
@Schema(description = "Relationship between two elements in a GRAPH collection")
public record RelationshipResponseDto(
    @Schema(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID collectionId,
    @Schema(description = "Source element UUID", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        UUID sourceElementId,
    @Schema(description = "Target element UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID targetElementId,
    @Schema(
            description = "Relationship type (e.g., 'depends-on', 'references')",
            example = "depends-on")
        String relationshipType,
    @Schema(
            description = "Optional JSON metadata",
            example = "{\"strength\": \"strong\"}",
            nullable = true)
        String relationshipMetadata,
    @Schema(description = "Timestamp when relationship was created") LocalDateTime createdDate,
    @Schema(description = "User who created the relationship", example = "admin")
        String createdBy) {}
