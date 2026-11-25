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
import net.ottleys.xpository.domain.Trait;

/**
 * Response DTO for trait data.
 *
 * <p>Exposes only public UUID, not internal database ID.
 */
@Schema(description = "Trait definition response")
public class TraitResponseDto {

  @Schema(description = "Public UUID identifier", example = "123e4567-e89b-12d3-a456-426614174000")
  private UUID traitId;

  @Schema(description = "Trait name", example = "Security Classification")
  private String name;

  @Schema(description = "Trait description", example = "Classification level for security")
  private String description;

  @Schema(description = "Trait type", example = "STRING")
  private Trait.TraitType traitType;

  @Schema(
      description = "Allowed values as JSON array",
      example = "[\"Public\", \"Confidential\", \"Secret\"]")
  private String allowedValuesJson;

  @Schema(description = "Default value", example = "Public")
  private String defaultValue;

  @Schema(description = "Whether this trait is required", example = "false")
  private boolean required;

  @Schema(description = "Whether this trait is inheritable", example = "true")
  private boolean inheritable;

  @Schema(description = "Creation timestamp", example = "2026-07-03T10:15:30")
  private LocalDateTime createdDate;

  @Schema(description = "Username who created this trait", example = "admin")
  private String createdBy;

  @Schema(description = "Last modification timestamp", example = "2026-07-03T14:20:45")
  private LocalDateTime modifiedDate;

  @Schema(description = "Username who last modified this trait", example = "admin")
  private String modifiedBy;

  /** Default constructor. */
  public TraitResponseDto() {}

  /**
   * Returns the trait ID.
   *
   * @return the trait ID
   */
  public UUID getTraitId() {
    return traitId;
  }

  /**
   * Sets the trait ID.
   *
   * @param traitId the trait ID
   */
  public void setTraitId(UUID traitId) {
    this.traitId = traitId;
  }

  /**
   * Returns the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
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

  /**
   * Returns the creation date.
   *
   * @return the creation date
   */
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  /**
   * Sets the creation date.
   *
   * @param createdDate the creation date
   */
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Returns the creator username.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the creator username.
   *
   * @param createdBy the creator username
   */
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Returns the modification date.
   *
   * @return the modification date
   */
  public LocalDateTime getModifiedDate() {
    return modifiedDate;
  }

  /**
   * Sets the modification date.
   *
   * @param modifiedDate the modification date
   */
  public void setModifiedDate(LocalDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  /**
   * Returns the modifier username.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the modifier username.
   *
   * @param modifiedBy the modifier username
   */
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }
}
