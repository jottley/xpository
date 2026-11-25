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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Permission entity representing access control rules.
 *
 * <p>Permissions define what actions a subject (user or group) can perform on a resource (element,
 * collection, or system). Permissions can be granted directly or inherited through group membership
 * or collection hierarchy.
 *
 * <p>Permission model:
 *
 * <ul>
 *   <li><b>Subject:</b> Who is granted permission (user UUID, group UUID, or "everyone")
 *   <li><b>Resource:</b> What the permission applies to (element UUID, collection UUID, or
 *       "system")
 *   <li><b>Action:</b> What operation is allowed (READ, WRITE, DELETE, ADMIN)
 *   <li><b>Resource Type:</b> Type of resource (USER, GROUP, ELEMENT, COLLECTION, SYSTEM)
 * </ul>
 *
 * <p>Unique constraint ensures one permission per (subject, resource, action) tuple.
 */
@Entity
@Table(
    name = "permissions",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_permission_subject_resource_action",
          columnNames = {"subject_id", "resource_id", "action"})
    })
@EntityListeners(AuditingEntityListener.class)
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Subject ID is required")
  @Size(max = 100, message = "Subject ID cannot exceed 100 characters")
  @Column(name = "subject_id", nullable = false, length = 100)
  private String subjectId;

  @NotNull(message = "Subject type is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, length = 20)
  private SubjectType subjectType;

  @NotBlank(message = "Resource ID is required")
  @Size(max = 100, message = "Resource ID cannot exceed 100 characters")
  @Column(name = "resource_id", nullable = false, length = 100)
  private String resourceId;

  @NotNull(message = "Resource type is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false, length = 20)
  private ResourceType resourceType;

  @NotNull(message = "Action is required")
  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private Action action;

  @Column(name = "inherited", nullable = false)
  private boolean inherited = false;

  @CreatedDate
  @Column(name = "created_date", nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 100)
  private String createdBy;

  /** Default constructor required by JPA. */
  public Permission() {}

  /**
   * Constructor for creating a permission.
   *
   * @param subjectId the subject identifier (user UUID, group UUID, or "everyone")
   * @param subjectType the type of subject
   * @param resourceId the resource identifier (element UUID, collection UUID, or "system")
   * @param resourceType the type of resource
   * @param action the permitted action
   */
  public Permission(
      String subjectId,
      SubjectType subjectType,
      String resourceId,
      ResourceType resourceType,
      Action action) {
    this.subjectId = subjectId;
    this.subjectType = subjectType;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.action = action;
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
   * Returns the subject identifier.
   *
   * @return the subject ID (user UUID, group UUID, or "everyone")
   */
  public String getSubjectId() {
    return subjectId;
  }

  /**
   * Sets the subject identifier.
   *
   * @param subjectId the subject ID
   */
  public void setSubjectId(String subjectId) {
    this.subjectId = subjectId;
  }

  /**
   * Returns the subject type.
   *
   * @return the subject type
   */
  public SubjectType getSubjectType() {
    return subjectType;
  }

  /**
   * Sets the subject type.
   *
   * @param subjectType the subject type
   */
  public void setSubjectType(SubjectType subjectType) {
    this.subjectType = subjectType;
  }

  /**
   * Returns the resource identifier.
   *
   * @return the resource ID (element UUID, collection UUID, or "system")
   */
  public String getResourceId() {
    return resourceId;
  }

  /**
   * Sets the resource identifier.
   *
   * @param resourceId the resource ID
   */
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  /**
   * Returns the resource type.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Sets the resource type.
   *
   * @param resourceType the resource type
   */
  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Returns the permitted action.
   *
   * @return the action
   */
  public Action getAction() {
    return action;
  }

  /**
   * Sets the permitted action.
   *
   * @param action the action
   */
  public void setAction(Action action) {
    this.action = action;
  }

  /**
   * Returns whether this permission was inherited.
   *
   * @return true if inherited from a parent resource or group
   */
  public boolean isInherited() {
    return inherited;
  }

  /**
   * Sets whether this permission was inherited.
   *
   * @param inherited true if inherited
   */
  public void setInherited(boolean inherited) {
    this.inherited = inherited;
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
   * Returns the username of the user who created this permission.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this permission.
   *
   * @param createdBy the creator username
   */
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Permission that = (Permission) o;
    return Objects.equals(subjectId, that.subjectId)
        && Objects.equals(resourceId, that.resourceId)
        && action == that.action;
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectId, resourceId, action);
  }

  @Override
  public String toString() {
    return "Permission{"
        + "id="
        + id
        + ", subjectId='"
        + subjectId
        + '\''
        + ", subjectType="
        + subjectType
        + ", resourceId='"
        + resourceId
        + '\''
        + ", resourceType="
        + resourceType
        + ", action="
        + action
        + ", inherited="
        + inherited
        + '}';
  }

  /** Subject type enumeration. */
  public enum SubjectType {
    /** Permission granted to a specific user. */
    USER,
    /** Permission granted to a group (applies to all members). */
    GROUP,
    /** Permission granted to everyone (anonymous access). */
    EVERYONE
  }

  /** Resource type enumeration. */
  public enum ResourceType {
    /** Permission applies to a user resource. */
    USER,
    /** Permission applies to a group resource. */
    GROUP,
    /** Permission applies to an element. */
    ELEMENT,
    /** Permission applies to a collection (and potentially its contents). */
    COLLECTION,
    /** Permission applies to system-wide operations. */
    SYSTEM
  }

  /** Action enumeration. */
  public enum Action {
    /** Read access - view resource details. */
    READ,
    /** Write access - modify resource. */
    WRITE,
    /** Delete access - remove resource. */
    DELETE,
    /** Admin access - full control including permission management. */
    ADMIN
  }
}
