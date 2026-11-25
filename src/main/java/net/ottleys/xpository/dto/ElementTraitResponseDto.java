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
 * Response DTO for element-trait associations.
 *
 * <p>Includes both the trait definition and the specific value for this element.
 */
@Schema(description = "Element-trait association with value")
public class ElementTraitResponseDto {

  @Schema(description = "Element UUID", example = "123e4567-e89b-12d3-a456-426614174000")
  private UUID elementId;

  @Schema(description = "Trait UUID", example = "123e4567-e89b-12d3-a456-426614174001")
  private UUID traitId;

  @Schema(description = "Trait name", example = "Security Classification")
  private String traitName;

  @Schema(description = "Trait value for this element", example = "Confidential")
  private String value;

  @Schema(description = "Whether this value was inherited", example = "false")
  private boolean inherited;

  @Schema(description = "Element ID from which this value was inherited", example = "123")
  private Long inheritedFromElementId;

  @Schema(description = "Creation timestamp", example = "2026-07-03T10:15:30")
  private LocalDateTime createdDate;

  @Schema(description = "Username who assigned this trait", example = "admin")
  private String createdBy;

  @Schema(description = "Last modification timestamp", example = "2026-07-03T14:20:45")
  private LocalDateTime modifiedDate;

  @Schema(description = "Username who last modified this value", example = "admin")
  private String modifiedBy;

  /** Default constructor. */
  public ElementTraitResponseDto() {}

  /**
   * Returns the element ID.
   *
   * @return the element ID
   */
  public UUID getElementId() {
    return elementId;
  }

  /**
   * Sets the element ID.
   *
   * @param elementId the element ID
   */
  public void setElementId(UUID elementId) {
    this.elementId = elementId;
  }

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
   * Returns the trait name.
   *
   * @return the trait name
   */
  public String getTraitName() {
    return traitName;
  }

  /**
   * Sets the trait name.
   *
   * @param traitName the trait name
   */
  public void setTraitName(String traitName) {
    this.traitName = traitName;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns whether the value is inherited.
   *
   * @return true if inherited
   */
  public boolean isInherited() {
    return inherited;
  }

  /**
   * Sets whether the value is inherited.
   *
   * @param inherited true if inherited
   */
  public void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  /**
   * Returns the inherited-from element ID.
   *
   * @return the inherited-from element ID
   */
  public Long getInheritedFromElementId() {
    return inheritedFromElementId;
  }

  /**
   * Sets the inherited-from element ID.
   *
   * @param inheritedFromElementId the inherited-from element ID
   */
  public void setInheritedFromElementId(Long inheritedFromElementId) {
    this.inheritedFromElementId = inheritedFromElementId;
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
