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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Junction table managing the many-to-many relationship between elements and collections.
 *
 * <p>Stores structural metadata for collection membership including parent-child relationships,
 * order positions for LIST collections, depth tracking, and materialized paths for TREE
 * collections. Each membership can have custom JSON metadata and tracks audit information for when
 * the element was added to the collection.
 */
@Entity
@Table(
    name = "element_collection_membership",
    uniqueConstraints = {
      @UniqueConstraint(
          columnNames = {"element_id", "collection_id"},
          name = "uk_element_collection")
    },
    indexes = {
      @Index(name = "idx_collection_parent", columnList = "collection_id, parent_element_id"),
      @Index(name = "idx_collection_order", columnList = "collection_id, order_position"),
      @Index(name = "idx_materialized_path", columnList = "materialized_path")
    })
public class ElementCollectionMembership {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Element is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "element_id", nullable = false)
  private Element element;

  @NotNull(message = "Collection is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "collection_id", nullable = false)
  private Collection collection;

  // Tree/List structure - parent element within collection
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_element_id")
  private Element parentElement;

  // Order position for LIST collections
  @Min(value = 0, message = "Order position must be non-negative")
  @Column(name = "order_position")
  private Integer orderPosition;

  // Nesting depth for LIST collections
  @Min(value = 0, message = "Depth must be non-negative")
  @Column(name = "depth")
  private Integer depth;

  // Materialized path for TREE collections (e.g., "/1/5/12")
  @Size(max = 1000, message = "Materialized path cannot exceed 1000 characters")
  @Column(name = "materialized_path", length = 1000)
  private String materializedPath;

  // Custom metadata for this membership (JSON)
  @Size(max = 65535, message = "Membership metadata cannot exceed 65535 characters")
  @Column(columnDefinition = "TEXT")
  private String membershipMetadata;

  // Audit
  @Column(nullable = false, updatable = false)
  private LocalDateTime addedDate;

  @NotBlank(message = "addedBy is required")
  @Size(max = 255, message = "addedBy cannot exceed 255 characters")
  @Column(nullable = false, updatable = false)
  private String addedBy;

  // Constructors
  public ElementCollectionMembership() {}

  public ElementCollectionMembership(Element element, Collection collection) {
    this.element = element;
    this.collection = collection;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Element getElement() {
    return element;
  }

  public void setElement(Element element) {
    this.element = element;
  }

  public Collection getCollection() {
    return collection;
  }

  public void setCollection(Collection collection) {
    this.collection = collection;
  }

  public Element getParentElement() {
    return parentElement;
  }

  public void setParentElement(Element parentElement) {
    this.parentElement = parentElement;
  }

  public Integer getOrderPosition() {
    return orderPosition;
  }

  public void setOrderPosition(Integer orderPosition) {
    this.orderPosition = orderPosition;
  }

  public Integer getDepth() {
    return depth;
  }

  public void setDepth(Integer depth) {
    this.depth = depth;
  }

  public String getMaterializedPath() {
    return materializedPath;
  }

  public void setMaterializedPath(String materializedPath) {
    this.materializedPath = materializedPath;
  }

  public String getMembershipMetadata() {
    return membershipMetadata;
  }

  public void setMembershipMetadata(String membershipMetadata) {
    this.membershipMetadata = membershipMetadata;
  }

  public LocalDateTime getAddedDate() {
    return addedDate;
  }

  public void setAddedDate(LocalDateTime addedDate) {
    this.addedDate = addedDate;
  }

  public String getAddedBy() {
    return addedBy;
  }

  public void setAddedBy(String addedBy) {
    this.addedBy = addedBy;
  }

  /**
   * Lifecycle callback that runs before entity is persisted.
   *
   * <p>Sets the addedDate timestamp to current time.
   */
  @PrePersist
  protected void onCreate() {
    addedDate = LocalDateTime.now();
  }
}
