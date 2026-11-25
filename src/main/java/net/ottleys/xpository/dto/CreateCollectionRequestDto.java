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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import net.ottleys.xpository.validation.ValidJson;

/**
 * Data Transfer Object for creating a new Collection via REST API.
 *
 * <p>Validates all required fields and constraints at the API boundary. Does not accept internal
 * IDs or system fields. The createdBy/modifiedBy fields will be set from the authenticated user
 * context.
 */
@Schema(description = "Request body for creating a new collection")
public class CreateCollectionRequestDto {

  @Schema(
      description = "Collection name (required)",
      example = "Documents",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Collection name is required")
  @Size(min = 1, max = 255, message = "Collection name must be between 1 and 255 characters")
  private String name;

  @Schema(description = "Collection description", example = "Document management collection")
  @Size(max = 65535, message = "Description cannot exceed 65535 characters")
  private String description;

  @Schema(
      description = "Collection type (required)",
      example = "LIST",
      allowableValues = {"LIST", "TREE", "GRAPH", "STACK"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Collection type is required")
  @Pattern(
      regexp = "LIST|TREE|GRAPH|STACK",
      message = "Collection type must be one of: LIST, TREE, GRAPH, STACK")
  private String type;

  @Schema(
      description = "Type-specific configuration as a JSON string (not a JSON object)",
      example = "{\\\"maxDepth\\\": 5}")
  @Size(max = 65535, message = "Configuration JSON cannot exceed 65535 characters")
  @ValidJson(message = "Configuration must be valid JSON")
  private String configurationJson;

  @Schema(
      description = "Set of allowed element type namespaces",
      example = "[\"xpository:core:document\", \"xpository:core:folder\"]")
  private Set<@NotBlank @Size(max = 255) String> allowedElementTypes;

  @Schema(
      description = "Custom metadata as a JSON string (not a JSON object)",
      example = "{\\\"department\\\": \\\"HR\\\"}")
  @Size(max = 65535, message = "Metadata JSON cannot exceed 65535 characters")
  @ValidJson(message = "Metadata must be valid JSON")
  private String metadataJson;

  @Schema(
      description = "Validation rules as a JSON string (not a JSON object)",
      example = "{\\\"type\\\": \\\"object\\\"}")
  @Size(max = 65535, message = "Validation rules JSON cannot exceed 65535 characters")
  @ValidJson(message = "Validation rules must be valid JSON")
  private String validationRulesJson;

  @Schema(
      description = "Parent collection UUID (null for top-level)",
      example = "660e8400-e29b-41d4-a716-446655440000")
  private UUID parentCollectionId;

  @Schema(
      description = "Whether to inherit permissions from parent (defaults to true)",
      example = "true")
  private Boolean inheritPermissions = true;

  // Constructors
  public CreateCollectionRequestDto() {}

  /**
   * Creates a request DTO with all fields.
   *
   * @param name the collection name
   * @param description the collection description
   * @param type the collection type (LIST, TREE, GRAPH, STACK)
   * @param configurationJson type-specific configuration JSON
   * @param allowedElementTypes set of allowed element type namespaces
   * @param metadataJson custom metadata JSON
   * @param validationRulesJson validation rules JSON
   * @param parentCollectionId parent collection UUID (null for top-level)
   * @param inheritPermissions whether to inherit parent permissions (defaults to true)
   */
  public CreateCollectionRequestDto(
      String name,
      String description,
      String type,
      String configurationJson,
      Set<String> allowedElementTypes,
      String metadataJson,
      String validationRulesJson,
      UUID parentCollectionId,
      Boolean inheritPermissions) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.configurationJson = configurationJson;
    this.allowedElementTypes = allowedElementTypes;
    this.metadataJson = metadataJson;
    this.validationRulesJson = validationRulesJson;
    this.parentCollectionId = parentCollectionId;
    this.inheritPermissions = inheritPermissions;
  }

  // Getters and Setters

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getConfigurationJson() {
    return configurationJson;
  }

  public void setConfigurationJson(String configurationJson) {
    this.configurationJson = configurationJson;
  }

  public Set<String> getAllowedElementTypes() {
    return allowedElementTypes;
  }

  public void setAllowedElementTypes(Set<String> allowedElementTypes) {
    this.allowedElementTypes = allowedElementTypes;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
  }

  public String getValidationRulesJson() {
    return validationRulesJson;
  }

  public void setValidationRulesJson(String validationRulesJson) {
    this.validationRulesJson = validationRulesJson;
  }

  public UUID getParentCollectionId() {
    return parentCollectionId;
  }

  public void setParentCollectionId(UUID parentCollectionId) {
    this.parentCollectionId = parentCollectionId;
  }

  public Boolean getInheritPermissions() {
    return inheritPermissions;
  }

  public void setInheritPermissions(Boolean inheritPermissions) {
    this.inheritPermissions = inheritPermissions;
  }
}
