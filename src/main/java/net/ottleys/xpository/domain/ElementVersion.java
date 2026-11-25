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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Represents a historical version snapshot of an element.
 *
 * <p>Each time an element is updated, a new version record is created capturing the element's state
 * at that point in time. Versions follow semantic versioning with major.minor format:
 *
 * <ul>
 *   <li><strong>Major version</strong>: Incremented for breaking changes (default: 1)
 *   <li><strong>Minor version</strong>: Incremented for backward-compatible changes
 * </ul>
 *
 * <p>Example version progression: 1.0 → 1.1 → 1.2 → 2.0 → 2.1
 *
 * <p>Version snapshots are immutable once created and provide a complete audit trail of element
 * changes over time. The current version is always stored on the Element entity itself.
 */
@Entity
@Table(
    name = "element_versions",
    indexes = {
      @Index(name = "idx_element_version", columnList = "element_id, major_version, minor_version"),
      @Index(name = "idx_element_created", columnList = "element_id, created_date")
    })
public class ElementVersion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Element is required")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "element_id", nullable = false)
  private Element element;

  @NotNull(message = "Major version is required")
  @Min(value = 1, message = "Major version must be at least 1")
  @Column(name = "major_version", nullable = false)
  private Integer majorVersion;

  @NotNull(message = "Minor version is required")
  @Min(value = 0, message = "Minor version must be at least 0")
  @Column(name = "minor_version", nullable = false)
  private Integer minorVersion;

  @NotBlank(message = "Element type name is required")
  @Size(max = 255, message = "Element type name cannot exceed 255 characters")
  @Column(name = "element_type_name", nullable = false)
  private String elementTypeName;

  @NotBlank(message = "Properties JSON is required")
  @Column(name = "properties_json", columnDefinition = "TEXT", nullable = false)
  private String propertiesJson;

  @Size(max = 1000, message = "Version comment cannot exceed 1000 characters")
  @Column(name = "version_comment", length = 1000)
  private String versionComment;

  // Audit fields
  @NotNull(message = "Created date is required")
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @NotBlank(message = "createdBy is required")
  @Size(max = 255, message = "createdBy cannot exceed 255 characters")
  @Column(nullable = false, updatable = false)
  private String createdBy;

  // Constructors
  public ElementVersion() {}

  public ElementVersion(
      Element element,
      Integer majorVersion,
      Integer minorVersion,
      String elementTypeName,
      String propertiesJson,
      String createdBy) {
    this.element = element;
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.elementTypeName = elementTypeName;
    this.propertiesJson = propertiesJson;
    this.createdBy = createdBy;
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

  public String getVersionComment() {
    return versionComment;
  }

  public void setVersionComment(String versionComment) {
    this.versionComment = versionComment;
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
   * Returns the version string in major.minor format.
   *
   * @return version string (e.g., "1.0", "2.5")
   */
  public String getVersionString() {
    return majorVersion + "." + minorVersion;
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
