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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * Represents a metadata item with custom properties defined by its ElementType.
 *
 * <p>Elements store flexible JSON-based properties validated against their type's schema. Each
 * element has both an internal Long ID and a public UUID for secure external references. Elements
 * are fully indexed for search via Hibernate Search and track creation/modification audit
 * information.
 */
@Entity
@Table(
    name = "elements",
    indexes = {
      @Index(name = "idx_element_deleted", columnList = "deleted"),
      @Index(name = "idx_element_uuid_deleted", columnList = "elementId, deleted")
    })
@Indexed
public class Element {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @jakarta.persistence.Version
  @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private Long version = 0L;

  @KeywordField
  @Column(unique = true, nullable = false, updatable = false, length = 36)
  private UUID elementId;

  @Size(max = 65535, message = "Properties JSON cannot exceed 65535 characters")
  @FullTextField(analyzer = "default")
  @Column(columnDefinition = "TEXT")
  private String propertiesJson;

  @NotNull(message = "Element type is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "element_type_id")
  @IndexedEmbedded
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private ElementType type;

  @OneToMany(mappedBy = "element", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ElementCollectionMembership> memberships = new HashSet<>();

  // Version tracking
  @NotNull(message = "Major version is required")
  @Min(value = 1, message = "Major version must be at least 1")
  @Column(name = "major_version", nullable = false)
  private Integer majorVersion = 1;

  @NotNull(message = "Minor version is required")
  @Min(value = 0, message = "Minor version must be at least 0")
  @Column(name = "minor_version", nullable = false)
  private Integer minorVersion = 0;

  // Soft delete support
  @GenericField
  @Column(nullable = false)
  private boolean deleted = false;

  private LocalDateTime deletedDate;

  @Size(max = 255, message = "deletedBy cannot exceed 255 characters")
  private String deletedBy;

  // Audit fields
  @NotBlank(message = "createdBy is required")
  @Size(max = 255, message = "createdBy cannot exceed 255 characters")
  @KeywordField
  @Column(nullable = false, updatable = false)
  private String createdBy;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @NotBlank(message = "modifiedBy is required")
  @Size(max = 255, message = "modifiedBy cannot exceed 255 characters")
  @KeywordField
  @Column(nullable = false)
  private String modifiedBy;

  @Column(nullable = false)
  private LocalDateTime modifiedDate;

  // Constructors
  public Element() {
    // Default constructor for JPA
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public UUID getElementId() {
    return elementId;
  }

  public void setElementId(UUID elementId) {
    this.elementId = elementId;
  }

  public String getPropertiesJson() {
    return propertiesJson;
  }

  public void setPropertiesJson(String propertiesJson) {
    this.propertiesJson = propertiesJson;
  }

  public ElementType getType() {
    return type;
  }

  public void setType(ElementType type) {
    this.type = type;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public LocalDateTime getModifiedDate() {
    return modifiedDate;
  }

  public void setModifiedDate(LocalDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  public Integer getMajorVersion() {
    return majorVersion;
  }

  public void setMajorVersion(Integer majorVersion) {
    this.majorVersion = majorVersion;
  }

  public Integer getMinorVersion() {
    return minorVersion;
  }

  public void setMinorVersion(Integer minorVersion) {
    this.minorVersion = minorVersion;
  }

  /**
   * Returns the current version string in major.minor format.
   *
   * @return version string (e.g., "1.0", "2.5")
   */
  public String getVersionString() {
    return majorVersion + "." + minorVersion;
  }

  public Set<ElementCollectionMembership> getMemberships() {
    return memberships;
  }

  public void setMemberships(Set<ElementCollectionMembership> memberships) {
    this.memberships = memberships;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public LocalDateTime getDeletedDate() {
    return deletedDate;
  }

  public void setDeletedDate(LocalDateTime deletedDate) {
    this.deletedDate = deletedDate;
  }

  public String getDeletedBy() {
    return deletedBy;
  }

  public void setDeletedBy(String deletedBy) {
    this.deletedBy = deletedBy;
  }

  /**
   * Marks this element as soft-deleted.
   *
   * <p>Sets the deleted flag to true and records the deletion timestamp and user. The element
   * remains in the database but will be excluded from normal queries.
   *
   * @param deletedBy username of the user performing the deletion
   */
  public void softDelete(String deletedBy) {
    this.deleted = true;
    this.deletedDate = LocalDateTime.now();
    this.deletedBy = deletedBy;
  }

  /**
   * Restores this soft-deleted element.
   *
   * <p>Resets the deleted flag to false and clears deletion metadata.
   */
  public void restore() {
    this.deleted = false;
    this.deletedDate = null;
    this.deletedBy = null;
  }

  /**
   * Lifecycle callback that runs before entity is persisted.
   *
   * <p>Initializes elementId UUID if not set and sets creation/modification timestamps.
   */
  @PrePersist
  protected void onCreate() {
    if (elementId == null) {
      elementId = UUID.randomUUID();
    }
    createdDate = LocalDateTime.now();
    modifiedDate = LocalDateTime.now();
  }

  /**
   * Lifecycle callback that runs before entity is updated.
   *
   * <p>Updates the modification timestamp to current time.
   */
  @PreUpdate
  protected void onUpdate() {
    modifiedDate = LocalDateTime.now();
  }
}
