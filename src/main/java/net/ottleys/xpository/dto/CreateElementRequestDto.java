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
import net.ottleys.xpository.validation.ValidJson;

/**
 * Data Transfer Object for creating a new Element via REST API.
 *
 * <p>Validates all required fields and constraints at the API boundary. Does not accept internal
 * IDs or system fields. The createdBy/modifiedBy fields will be set from the authenticated user
 * context. Element type is specified by name (namespace) and will be resolved by the service layer.
 *
 * <p>Supports nested list elements via the optional parentElementId field. When provided, the
 * element will be created as a child of the specified parent element within the same collection,
 * with depth automatically calculated as parent.depth + 1.
 */
@Schema(description = "Request body for creating a new element")
public class CreateElementRequestDto {

  @Schema(
      description = "Element type name (namespace format)",
      example = "xpository:core:standard",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Element type is required")
  @Size(max = 255, message = "Element type name cannot exceed 255 characters")
  private String elementTypeName;

  @Schema(
      description = "Element properties as a JSON string (not a JSON object)",
      example =
          "{\\\"title\\\": \\\"My Document\\\", \\\"description\\\": \\\"Sample document\\\"}")
  @Size(max = 65535, message = "Properties JSON cannot exceed 65535 characters")
  @ValidJson(message = "Properties must be valid JSON")
  private String propertiesJson;

  @Schema(
      description =
          "Parent element UUID for nested lists. If provided, the element will be added as a child of the specified parent element.",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID parentElementId;

  // Constructors
  public CreateElementRequestDto() {}

  /**
   * Creates a request DTO with all fields.
   *
   * @param elementTypeName the element type name (namespace format)
   * @param propertiesJson element properties JSON
   */
  public CreateElementRequestDto(String elementTypeName, String propertiesJson) {
    this.elementTypeName = elementTypeName;
    this.propertiesJson = propertiesJson;
  }

  /**
   * Creates a request DTO with parent element for nested lists.
   *
   * @param elementTypeName the element type name (namespace format)
   * @param propertiesJson element properties JSON
   * @param parentElementId the parent element UUID (optional)
   */
  public CreateElementRequestDto(
      String elementTypeName, String propertiesJson, UUID parentElementId) {
    this.elementTypeName = elementTypeName;
    this.propertiesJson = propertiesJson;
    this.parentElementId = parentElementId;
  }

  // Getters and Setters

  public String getElementTypeName() {
    return elementTypeName;
  }

  public void setElementTypeName(String elementTypeName) {
    this.elementTypeName = elementTypeName;
  }

  public String getPropertiesJson() {
    return propertiesJson;
  }

  public void setPropertiesJson(String propertiesJson) {
    this.propertiesJson = propertiesJson;
  }

  public UUID getParentElementId() {
    return parentElementId;
  }

  public void setParentElementId(UUID parentElementId) {
    this.parentElementId = parentElementId;
  }
}
