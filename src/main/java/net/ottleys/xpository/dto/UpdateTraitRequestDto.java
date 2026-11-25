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
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing trait.
 *
 * <p>All fields are optional; only provided fields will be updated.
 */
@Schema(description = "Request to update an existing trait")
public class UpdateTraitRequestDto {

  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  @Schema(description = "Updated description", example = "Updated classification level")
  private String description;

  @Schema(
      description = "Updated allowed values as JSON array",
      example = "[\"Public\", \"Internal\", \"Confidential\", \"Secret\"]")
  private String allowedValuesJson;

  @Schema(description = "Updated default value", example = "Internal")
  private String defaultValue;

  @Schema(description = "Updated required flag", example = "true")
  private Boolean required;

  @Schema(description = "Updated inheritable flag", example = "false")
  private Boolean inheritable;

  /** Default constructor. */
  public UpdateTraitRequestDto() {}

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the allowed values JSON.
   *
   * @return the allowed values JSON
   */
  public String getAllowedValuesJson() {
    return allowedValuesJson;
  }

  /**
   * Sets the allowed values JSON.
   *
   * @param allowedValuesJson the allowed values JSON
   */
  public void setAllowedValuesJson(String allowedValuesJson) {
    this.allowedValuesJson = allowedValuesJson;
  }

  /**
   * Returns the default value.
   *
   * @return the default value
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  /**
   * Sets the default value.
   *
   * @param defaultValue the default value
   */
  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Returns the required flag.
   *
   * @return the required flag
   */
  public Boolean getRequired() {
    return required;
  }

  /**
   * Sets the required flag.
   *
   * @param required the required flag
   */
  public void setRequired(Boolean required) {
    this.required = required;
  }

  /**
   * Returns the inheritable flag.
   *
   * @return the inheritable flag
   */
  public Boolean getInheritable() {
    return inheritable;
  }

  /**
   * Sets the inheritable flag.
   *
   * @param inheritable the inheritable flag
   */
  public void setInheritable(Boolean inheritable) {
    this.inheritable = inheritable;
  }
}
