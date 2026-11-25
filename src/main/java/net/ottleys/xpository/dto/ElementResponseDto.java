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
import java.util.UUID;

/**
 * Data Transfer Object for Element API responses.
 *
 * <p>Exposes only the public UUID (not internal Long ID) for security. Used by REST API to return
 * element data to clients. Excludes sensitive system fields and provides clean API contract.
 *
 * <p>Implemented as a Java record for immutability, automatic equals/hashCode, and concise syntax.
 * Jackson serialization works seamlessly with record components.
 *
 * @param elementId the public UUID
 * @param version the version number for optimistic locking
 * @param majorVersion the major semantic version number
 * @param minorVersion the minor semantic version number
 * @param versionString the version in major.minor format
 * @param elementTypeName the element type name (namespace format)
 * @param propertiesJson element properties JSON
 * @param createdBy username who created the element
 * @param createdDate creation timestamp
 * @param modifiedBy username who last modified the element
 * @param modifiedDate last modification timestamp
 */
@Schema(description = "Element response containing all element details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ElementResponseDto(
    @Schema(
            description = "Unique identifier for the element",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID elementId,
    @Schema(
            description =
                "Version number for optimistic locking - increment this when updating to prevent lost updates",
            example = "0")
        Long version,
    @Schema(description = "Major semantic version number", example = "1") Integer majorVersion,
    @Schema(description = "Minor semantic version number", example = "0") Integer minorVersion,
    @Schema(description = "Version string (major.minor)", example = "1.0") String versionString,
    @Schema(
            description = "Element type name (namespace format)",
            example = "xpository:core:standard")
        String elementTypeName,
    @Schema(
            description = "Element properties as a JSON string",
            example =
                "{\\\"title\\\": \\\"My Document\\\", \\\"description\\\": \\\"Sample document\\\"}")
        String propertiesJson,
    @Schema(description = "Username who created the element", example = "admin") String createdBy,
    @Schema(description = "Creation timestamp", example = "2026-06-05T10:15:30")
        LocalDateTime createdDate,
    @Schema(description = "Username who last modified the element", example = "admin")
        String modifiedBy,
    @Schema(description = "Last modification timestamp", example = "2026-06-05T14:30:00")
        LocalDateTime modifiedDate) {}
