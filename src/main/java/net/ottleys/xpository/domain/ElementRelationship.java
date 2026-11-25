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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Represents a directed relationship between two elements in a GRAPH collection.
 *
 * <p>Relationships are directional edges in the graph, connecting a source element to a target
 * element. Each relationship has a type (e.g., "depends-on", "references", "relates-to") and
 * optional JSON metadata. Multiple relationships of different types can exist between the same two
 * elements.
 *
 * <p>Example: Element A "depends-on" Element B, Element A "references" Element C
 */
@Entity
@Table(
    name = "element_relationships",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {
            "collection_id",
            "source_element_id",
            "target_element_id",
            "relationship_type"
          },
          name = "uk_relationship")
    },
    indexes = {
      @Index(name = "idx_source_element", columnList = "source_element_id, collection_id"),
      @Index(name = "idx_target_element", columnList = "target_element_id, collection_id"),
      @Index(name = "idx_relationship_type", columnList = "relationship_type, collection_id")
    })
public class ElementRelationship {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Collection is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "collection_id", nullable = false)
  private Collection collection;

  @NotNull(message = "Source element is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_element_id", nullable = false)
  private Element sourceElement;

  @NotNull(message = "Target element is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_element_id", nullable = false)
  private Element targetElement;

  @NotBlank(message = "Relationship type is required")
  @Size(max = 100, message = "Relationship type cannot exceed 100 characters")
  @Column(name = "relationship_type", nullable = false, length = 100)
  private String relationshipType;

  // Optional metadata for this relationship (JSON)
  @Size(max = 65535, message = "Relationship metadata cannot exceed 65535 characters")
  @Column(name = "relationship_metadata", columnDefinition = "TEXT")
  private String relationshipMetadata;

  // Audit
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @NotBlank(message = "createdBy is required")
  @Size(max = 255, message = "createdBy cannot exceed 255 characters")
  @Column(nullable = false, updatable = false)
  private String createdBy;

  // Constructors
  public ElementRelationship() {}

  public ElementRelationship(
      Collection collection,
      Element sourceElement,
      Element targetElement,
      String relationshipType) {
    this.collection = collection;
    this.sourceElement = sourceElement;
    this.targetElement = targetElement;
    this.relationshipType = relationshipType;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Collection getCollection() {
    return collection;
  }

  public void setCollection(Collection collection) {
    this.collection = collection;
  }

  public Element getSourceElement() {
    return sourceElement;
  }

  public void setSourceElement(Element sourceElement) {
    this.sourceElement = sourceElement;
  }

  public Element getTargetElement() {
    return targetElement;
  }

  public void setTargetElement(Element targetElement) {
    this.targetElement = targetElement;
  }

  public String getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(String relationshipType) {
    this.relationshipType = relationshipType;
  }

  public String getRelationshipMetadata() {
    return relationshipMetadata;
  }

  public void setRelationshipMetadata(String relationshipMetadata) {
    this.relationshipMetadata = relationshipMetadata;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Lifecycle callback that runs before entity is persisted.
   *
   * <p>Sets the createdDate timestamp to current time.
   */
  @PrePersist
  protected void onCreate() {
    createdDate = LocalDateTime.now();
  }
}
