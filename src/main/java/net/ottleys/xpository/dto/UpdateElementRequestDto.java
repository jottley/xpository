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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.ottleys.xpository.validation.ValidJson;

/**
 * Data Transfer Object for updating an existing Element via REST API.
 *
 * <p>All fields are optional to support partial updates (PATCH semantics). Only non-null fields
 * will be updated. Does not allow updating immutable fields like elementId, createdBy, or
 * createdDate. The modifiedBy field will be set from the authenticated user context.
 */
@Schema(
    description =
        "Request body for partially updating an element (PATCH). All fields are optional except version.")
public class UpdateElementRequestDto {

  @Schema(
      description =
          "Version number for optimistic locking - must match current version to prevent lost updates",
      example = "0",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Version is required for optimistic locking")
  private Long version;

  @Schema(description = "Element type name (namespace format)", example = "xpository:core:standard")
  @Size(max = 255, message = "Element type name cannot exceed 255 characters")
  private String elementTypeName;

  @Schema(
      description = "Element properties as a JSON string (not a JSON object)",
      example =
          "{\\\"title\\\": \\\"Updated Document\\\", \\\"description\\\": \\\"Updated description\\\"}")
  @Size(max = 65535, message = "Properties JSON cannot exceed 65535 characters")
  @ValidJson(message = "Properties must be valid JSON")
  private String propertiesJson;

  @Schema(
      description =
          "Flag indicating if this update is a breaking/major change. If true, major version is incremented and minor version resets to 0. If false or omitted, minor version is incremented.",
      example = "false")
  private Boolean majorVersion = false;

  // Constructors
  public UpdateElementRequestDto() {}

  /**
   * Creates a request DTO with common fields.
   *
   * @param version the version number for optimistic locking (required)
   * @param elementTypeName the element type name (optional)
   * @param propertiesJson element properties JSON (optional)
   */
  public UpdateElementRequestDto(Long version, String elementTypeName, String propertiesJson) {
    this(version, elementTypeName, propertiesJson, false);
  }

  /**
   * Creates a request DTO with all fields including majorVersion flag.
   *
   * @param version the version number for optimistic locking (required)
   * @param elementTypeName the element type name (optional)
   * @param propertiesJson element properties JSON (optional)
   * @param majorVersion whether this is a major version bump (optional)
   */
  public UpdateElementRequestDto(
      Long version, String elementTypeName, String propertiesJson, Boolean majorVersion) {
    this.version = version;
    this.elementTypeName = elementTypeName;
    this.propertiesJson = propertiesJson;
    this.majorVersion = majorVersion;
  }

  // Getters and Setters

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

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

  public Boolean getMajorVersion() {
    return majorVersion;
  }

  public void setMajorVersion(Boolean majorVersion) {
    this.majorVersion = majorVersion;
  }

  public boolean isMajorVersion() {
    return Boolean.TRUE.equals(majorVersion);
  }
}
