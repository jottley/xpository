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
import net.ottleys.xpository.domain.Trait;

/**
 * Request DTO for creating a new trait.
 *
 * <p>Validates input data before creating a trait definition.
 */
@Schema(description = "Request to create a new trait")
public class CreateTraitRequestDto {

  @NotBlank(message = "Trait name is required")
  @Size(max = 100, message = "Trait name cannot exceed 100 characters")
  @Schema(description = "Unique name for the trait", example = "Security Classification")
  private String name;

  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  @Schema(
      description = "Description of the trait's purpose",
      example = "Classification level for security purposes")
  private String description;

  @NotNull(message = "Trait type is required")
  @Schema(description = "Type of values this trait accepts", example = "STRING")
  private Trait.TraitType traitType;

  @Schema(
      description = "JSON array of allowed values for validation",
      example = "[\"Public\", \"Confidential\", \"Secret\"]")
  private String allowedValuesJson;

  @Schema(description = "Default value for new elements", example = "Public")
  private String defaultValue;

  @Schema(description = "Whether this trait is required for all elements", example = "false")
  private boolean required = false;

  @Schema(
      description = "Whether this trait can be inherited from parent elements",
      example = "true")
  private boolean inheritable = false;

  /** Default constructor. */
  public CreateTraitRequestDto() {}

  /**
   * Returns the trait name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the trait name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

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
   * Returns the trait type.
   *
   * @return the trait type
   */
  public Trait.TraitType getTraitType() {
    return traitType;
  }

  /**
   * Sets the trait type.
   *
   * @param traitType the trait type
   */
  public void setTraitType(Trait.TraitType traitType) {
    this.traitType = traitType;
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
   * Returns whether the trait is required.
   *
   * @return true if required
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Sets whether the trait is required.
   *
   * @param required true if required
   */
  public void setRequired(boolean required) {
    this.required = required;
  }

  /**
   * Returns whether the trait is inheritable.
   *
   * @return true if inheritable
   */
  public boolean isInheritable() {
    return inheritable;
  }

  /**
   * Sets whether the trait is inheritable.
   *
   * @param inheritable true if inheritable
   */
  public void setInheritable(boolean inheritable) {
    this.inheritable = inheritable;
  }
}
