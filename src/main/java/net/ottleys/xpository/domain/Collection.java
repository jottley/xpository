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

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 * Represents a container for organizing elements with support for hierarchical nesting and access
 * control.
 *
 * <p>Collections support multiple structural types (LIST, TREE, GRAPH, STACK) and can contain both
 * elements and other collections. They provide features including type restrictions, permission
 * inheritance, soft delete with configurable retention, and full-text search via Hibernate Search.
 * Each collection has both an internal Long ID and a public UUID for secure external references.
 */
@Entity
@Table(
    name = "collections",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {"name", "parent_collection_id"},
          name = "uk_collection_name_per_parent")
    },
    indexes = {
      @Index(
          columnList = "name, parent_collection_id, deleted",
          name = "idx_collection_name_parent_deleted")
    })
@Indexed
public class Collection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @jakarta.persistence.Version
  @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private Long version = 0L;

  @KeywordField
  @Column(unique = true, nullable = false, updatable = false, length = 36)
  private UUID collectionId;

  @NotBlank(message = "Collection name is required")
  @Size(min = 1, max = 255, message = "Collection name must be between 1 and 255 characters")
  @FullTextField
  @Column(nullable = false, length = 255)
  private String name;

  @Size(max = 65535, message = "Description cannot exceed 65535 characters")
  @FullTextField
  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Collection type is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CollectionType type;

  // Type-specific configuration (JSON)
  @Size(max = 65535, message = "Configuration JSON cannot exceed 65535 characters")
  @Column(columnDefinition = "TEXT")
  private String configurationJson;

  // Allowed element types (namespaces)
  @ElementCollection
  @CollectionTable(
      name = "collection_allowed_types",
      joinColumns = @JoinColumn(name = "collection_id"))
  @Column(name = "element_type_namespace")
  private Set<String> allowedElementTypes = new HashSet<>();

  // Custom metadata (searchable)
  @Size(max = 65535, message = "Metadata JSON cannot exceed 65535 characters")
  @FullTextField
  @Column(columnDefinition = "TEXT")
  private String metadataJson;

  // Validation rules (JSON)
  @Size(max = 65535, message = "Validation rules JSON cannot exceed 65535 characters")
  @Column(columnDefinition = "TEXT")
  private String validationRulesJson;

  // Collection nesting (collections can contain collections)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_collection_id")
  @IndexedEmbedded(
      includeDepth = 1,
      includePaths = {"name", "collectionId"})
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private Collection parentCollection;

  // Permission inheritance
  @Column(nullable = false)
  private boolean inheritPermissions = true;

  // System collection flag (for root collection)
  @Column(nullable = false)
  private boolean systemCollection = false;

  // Soft delete
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
  public Collection() {}

  public Collection(String name, CollectionType type) {
    this.name = name;
    this.type = type;
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

  public UUID getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(UUID collectionId) {
    this.collectionId = collectionId;
  }

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

  public CollectionType getType() {
    return type;
  }

  public void setType(CollectionType type) {
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

  public Collection getParentCollection() {
    return parentCollection;
  }

  public void setParentCollection(Collection parentCollection) {
    this.parentCollection = parentCollection;
  }

  public boolean isInheritPermissions() {
    return inheritPermissions;
  }

  public void setInheritPermissions(boolean inheritPermissions) {
    this.inheritPermissions = inheritPermissions;
  }

  public boolean isSystemCollection() {
    return systemCollection;
  }

  public void setSystemCollection(boolean systemCollection) {
    this.systemCollection = systemCollection;
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

  /**
   * Lifecycle callback that runs before entity is persisted.
   *
   * <p>Initializes collectionId UUID if not set and sets creation/modification timestamps.
   */
  @PrePersist
  protected void onCreate() {
    if (collectionId == null) {
      collectionId = UUID.randomUUID();
    }
    createdDate = LocalDateTime.now();
    modifiedDate = LocalDateTime.now();
  }

  /**
   * Lifecycle callback that runs before entity is removed.
   *
   * <p>Prevents deletion of system collections.
   */
  @PreRemove
  protected void preventSystemCollectionDeletion() {
    if (this.systemCollection) {
      throw new IllegalStateException("Cannot delete system collection: " + this.collectionId);
    }
  }

  /**
   * Lifecycle callback that runs before entity is updated.
   *
   * <p>Prevents soft deletion of system collections.
   *
   * <p>Updates the modification timestamp to current time.
   */
  @PreUpdate
  protected void onUpdate() {
    if (this.systemCollection && this.deleted) {
      throw new IllegalStateException("Cannot soft-delete system collection: " + this.name);
    }

    modifiedDate = LocalDateTime.now();
  }

  /**
   * Marks this collection as soft deleted without removing it from the database.
   *
   * <p>Throws an exception if attempting to soft delete a system collection.
   *
   * @param deletedBy username or identifier of who performed the deletion
   */
  public void softDelete(String deletedBy) {

    // Protect system collections from deletion
    if (this.isSystemCollection()) {
      throw new IllegalStateException("Cannot delete system collection: " + this.getName());
    }

    this.deleted = true;
    this.deletedDate = LocalDateTime.now();
    this.deletedBy = deletedBy;
  }

  /**
   * Restores this soft-deleted collection.
   *
   * <p>Resets the deleted flag to false and clears deletion metadata.
   */
  public void restore() {
    this.deleted = false;
    this.deletedDate = null;
    this.deletedBy = null;
  }
}
