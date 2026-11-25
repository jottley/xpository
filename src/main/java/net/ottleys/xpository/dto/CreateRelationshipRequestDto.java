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
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating a relationship between two elements in a GRAPH collection.
 *
 * <p>Relationships are directed from source to target with a type (e.g., "depends-on",
 * "references"). Optional JSON metadata can store additional relationship properties.
 */
@Schema(description = "Request to create a relationship between two elements in a GRAPH collection")
public class CreateRelationshipRequestDto {

  @NotNull(message = "Source element ID is required")
  @Schema(
      description = "UUID of the source element in the relationship",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID sourceElementId;

  @NotNull(message = "Target element ID is required")
  @Schema(
      description = "UUID of the target element in the relationship",
      example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
  private UUID targetElementId;

  @NotBlank(message = "Relationship type is required")
  @Size(max = 100, message = "Relationship type cannot exceed 100 characters")
  @Schema(
      description = "Type of the relationship (e.g., 'depends-on', 'references', 'relates-to')",
      example = "depends-on",
      maxLength = 100)
  private String relationshipType;

  @Size(max = 65535, message = "Relationship metadata cannot exceed 65535 characters")
  @Schema(
      description = "Optional JSON metadata for the relationship",
      example = "{\"strength\": \"strong\", \"notes\": \"Critical dependency\"}",
      maxLength = 65535)
  private String relationshipMetadata;

  // Constructors
  public CreateRelationshipRequestDto() {}

  public CreateRelationshipRequestDto(
      UUID sourceElementId, UUID targetElementId, String relationshipType) {
    this.sourceElementId = sourceElementId;
    this.targetElementId = targetElementId;
    this.relationshipType = relationshipType;
  }

  public CreateRelationshipRequestDto(
      UUID sourceElementId,
      UUID targetElementId,
      String relationshipType,
      String relationshipMetadata) {
    this.sourceElementId = sourceElementId;
    this.targetElementId = targetElementId;
    this.relationshipType = relationshipType;
    this.relationshipMetadata = relationshipMetadata;
  }

  // Getters and Setters
  public UUID getSourceElementId() {
    return sourceElementId;
  }

  public void setSourceElementId(UUID sourceElementId) {
    this.sourceElementId = sourceElementId;
  }

  public UUID getTargetElementId() {
    return targetElementId;
  }

  public void setTargetElementId(UUID targetElementId) {
    this.targetElementId = targetElementId;
  }

  public String getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(String relationshipType) {
    this.relationshipType = relationshipType;
  }

  public String getRelationshipMetadata() {
    return relationshipMetadata;
  }

  public void setRelationshipMetadata(String relationshipMetadata) {
    this.relationshipMetadata = relationshipMetadata;
  }
}
