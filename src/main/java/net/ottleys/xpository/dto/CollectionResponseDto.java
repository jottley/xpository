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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for Collection API responses.
 *
 * <p>Exposes only the public UUID (not internal Long ID) for security. Used by REST API to return
 * collection data to clients. Excludes sensitive system fields and provides clean API contract.
 *
 * <p>Implemented as a Java record for immutability, automatic equals/hashCode, and concise syntax.
 * Jackson serialization works seamlessly with record components.
 *
 * @param collectionId the public UUID
 * @param version the version number for optimistic locking
 * @param name the collection name
 * @param description the collection description
 * @param type the collection type (LIST, TREE, GRAPH, STACK)
 * @param configurationJson type-specific configuration JSON
 * @param allowedElementTypes set of allowed element type namespaces
 * @param metadataJson custom metadata JSON
 * @param parentCollectionId parent collection UUID (null for top-level)
 * @param inheritPermissions whether to inherit parent permissions (nullable)
 * @param systemCollection whether this is a system collection (nullable)
 * @param createdBy username who created the collection
 * @param createdDate creation timestamp
 * @param modifiedBy username who last modified the collection
 * @param modifiedDate last modification timestamp
 */
@Schema(description = "Collection response containing all collection details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollectionResponseDto(
    @Schema(
            description = "Unique identifier for the collection",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID collectionId,
    @Schema(
            description =
                "Version number for optimistic locking - increment this when updating to prevent lost updates",
            example = "0")
        Long version,
    @Schema(description = "Collection name", example = "Documents") String name,
    @Schema(description = "Collection description", example = "Document management collection")
        String description,
    @Schema(
            description = "Collection type",
            example = "LIST",
            allowableValues = {"LIST", "TREE", "GRAPH", "STACK"})
        String type,
    @Schema(
            description = "Type-specific configuration as a JSON string",
            example = "{\\\"maxDepth\\\": 5}")
        String configurationJson,
    @Schema(
            description = "Set of allowed element type namespaces",
            example = "[\"xpository:core:document\", \"xpository:core:folder\"]")
        Set<String> allowedElementTypes,
    @Schema(
            description = "Custom metadata as a JSON string",
            example = "{\\\"department\\\": \\\"HR\\\"}")
        String metadataJson,
    @Schema(
            description = "Parent collection UUID (null for top-level)",
            example = "660e8400-e29b-41d4-a716-446655440000")
        UUID parentCollectionId,
    @Schema(description = "Whether to inherit permissions from parent", example = "true")
        Boolean inheritPermissions,
    @Schema(description = "Whether this is a system-managed collection", example = "false")
        Boolean systemCollection,
    @Schema(description = "Username who created the collection", example = "admin")
        String createdBy,
    @Schema(description = "Creation timestamp", example = "2026-06-04T10:15:30")
        LocalDateTime createdDate,
    @Schema(description = "Username who last modified the collection", example = "admin")
        String modifiedBy,
    @Schema(description = "Last modification timestamp", example = "2026-06-04T14:30:00")
        LocalDateTime modifiedDate) {}
