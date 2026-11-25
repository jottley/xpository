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
package net.ottleys.xpository.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Trait entity representing reusable metadata classifications.
 *
 * <p>Traits provide a way to add cross-cutting metadata to elements without modifying their core
 * properties. Examples include security classifications, lifecycle stages, categories, tags, or any
 * other aspect that multiple elements can share.
 *
 * <p>Each trait has:
 *
 * <ul>
 *   <li>A unique name (e.g., "Security Classification", "Lifecycle Stage")
 *   <li>A type (STRING, NUMBER, BOOLEAN, DATE) defining the value format
 *   <li>Optional allowed values for validation (e.g., ["Public", "Confidential", "Secret"])
 *   <li>Optional default value
 * </ul>
 *
 * <p>Elements are associated with traits via the ElementTrait junction entity, which stores the
 * specific value for that element.
 *
 * <p>Uses dual ID pattern: internal Long ID for database operations and public UUID for API access.
 */
@Entity
@Table(name = "traits")
@Indexed
@EntityListeners(AuditingEntityListener.class)
public class Trait {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "trait_id", nullable = false, unique = true, updatable = false)
  @KeywordField
  private UUID traitId;

  @NotBlank(message = "Trait name is required")
  @Size(max = 100, message = "Trait name cannot exceed 100 characters")
  @Column(name = "name", nullable = false, unique = true, length = 100)
  @FullTextField
  private String name;

  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  @Column(name = "description", length = 1000)
  @FullTextField
  private String description;

  @NotNull(message = "Trait type is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "trait_type", nullable = false, length = 20)
  private TraitType traitType;

  @Column(name = "allowed_values_json", columnDefinition = "TEXT")
  private String allowedValuesJson;

  @Column(name = "default_value", length = 500)
  private String defaultValue;

  @Column(name = "required", nullable = false)
  private boolean required = false;

  @Column(name = "inheritable", nullable = false)
  private boolean inheritable = false;

  @CreatedDate
  @Column(name = "created_date", nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 100)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "modified_date")
  private LocalDateTime modifiedDate;

  @LastModifiedBy
  @Column(name = "modified_by", length = 100)
  private String modifiedBy;

  /** Default constructor required by JPA. */
  public Trait() {}

  /** Generates a new UUID for the trait on creation. */
  @PrePersist
  protected void onCreate() {
    if (traitId == null) {
      traitId = UUID.randomUUID();
    }
  }

  /**
   * Returns the internal database ID.
   *
   * @return the internal ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the internal database ID.
   *
   * @param id the internal ID
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the public UUID identifier.
   *
   * @return the trait UUID
   */
  public UUID getTraitId() {
    return traitId;
  }

  /**
   * Sets the public UUID identifier.
   *
   * @param traitId the trait UUID
   */
  public void setTraitId(UUID traitId) {
    this.traitId = traitId;
  }

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
   * Returns the trait description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the trait description.
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
  public TraitType getTraitType() {
    return traitType;
  }

  /**
   * Sets the trait type.
   *
   * @param traitType the trait type
   */
  public void setTraitType(TraitType traitType) {
    this.traitType = traitType;
  }

  /**
   * Returns the allowed values as JSON.
   *
   * <p>Format: JSON array of strings, e.g., ["Public", "Confidential", "Secret"]
   *
   * @return the allowed values JSON
   */
  public String getAllowedValuesJson() {
    return allowedValuesJson;
  }

  /**
   * Sets the allowed values as JSON.
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
   * Returns whether this trait is required for elements.
   *
   * @return true if required
   */
  public boolean isRequired() {
    return required;
  }

  /**
   * Sets whether this trait is required.
   *
   * @param required true if required
   */
  public void setRequired(boolean required) {
    this.required = required;
  }

  /**
   * Returns whether this trait is inheritable.
   *
   * <p>Inheritable traits can be inherited from parent elements or collections.
   *
   * @return true if inheritable
   */
  public boolean isInheritable() {
    return inheritable;
  }

  /**
   * Sets whether this trait is inheritable.
   *
   * @param inheritable true if inheritable
   */
  public void setInheritable(boolean inheritable) {
    this.inheritable = inheritable;
  }

  /**
   * Returns the creation timestamp.
   *
   * @return the created date
   */
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param createdDate the created date
   */
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Returns the username of the user who created this trait.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this trait.
   *
   * @param createdBy the creator username
   */
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Returns the last modification timestamp.
   *
   * @return the modified date
   */
  public LocalDateTime getModifiedDate() {
    return modifiedDate;
  }

  /**
   * Sets the last modification timestamp.
   *
   * @param modifiedDate the modified date
   */
  public void setModifiedDate(LocalDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  /**
   * Returns the username of the user who last modified this trait.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the username of the user who last modified this trait.
   *
   * @param modifiedBy the modifier username
   */
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Trait trait = (Trait) o;
    return Objects.equals(traitId, trait.traitId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(traitId);
  }

  @Override
  public String toString() {
    return "Trait{"
        + "id="
        + id
        + ", traitId="
        + traitId
        + ", name='"
        + name
        + '\''
        + ", traitType="
        + traitType
        + '}';
  }

  /** Enumeration of supported trait value types. */
  public enum TraitType {
    /** String values (e.g., "Public", "Confidential"). */
    STRING,
    /** Numeric values (e.g., 42, 3.14). */
    NUMBER,
    /** Boolean values (true/false). */
    BOOLEAN,
    /** Date values (ISO-8601 format). */
    DATE
  }
}
