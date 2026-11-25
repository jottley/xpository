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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
 * Group entity representing a collection of users for permission management.
 *
 * <p>Groups can contain multiple users and can be granted permissions. Groups can also inherit
 * permissions from parent groups, allowing hierarchical permission structures.
 *
 * <p>The "everyone" group is a special read-only group that includes all users automatically.
 *
 * <p>Uses dual ID pattern: internal Long ID for database operations and public UUID for API access.
 *
 * <p>Indexed by Hibernate Search for full-text search on name, description, and exact match on
 * groupId.
 */
@Entity
@Table(name = "groups")
@Indexed
@EntityListeners(AuditingEntityListener.class)
public class Group {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "group_id", nullable = false, unique = true, updatable = false)
  @KeywordField
  private UUID groupId;

  @NotBlank(message = "Group name is required")
  @Size(max = 100, message = "Group name cannot exceed 100 characters")
  @Column(name = "name", nullable = false, unique = true, length = 100)
  @FullTextField
  private String name;

  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  @Column(name = "description", length = 1000)
  @FullTextField
  private String description;

  @Column(name = "system_group", nullable = false)
  private boolean systemGroup = false;

  @Column(name = "inherit_permissions", nullable = false)
  private boolean inheritPermissions = true;

  @ManyToOne private Group parentGroup;

  @ManyToMany(mappedBy = "groups")
  private Set<User> members = new HashSet<>();

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
  public Group() {}

  /** Generates a new UUID for the group on creation. */
  @PrePersist
  protected void onCreate() {
    if (groupId == null) {
      groupId = UUID.randomUUID();
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
   * @return the group UUID
   */
  public UUID getGroupId() {
    return groupId;
  }

  /**
   * Sets the public UUID identifier.
   *
   * @param groupId the group UUID
   */
  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  /**
   * Returns the group name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the group name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the group description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the group description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns whether this is a system-managed group.
   *
   * <p>System groups (e.g., "everyone") cannot be deleted or have certain properties modified.
   *
   * @return true if system group
   */
  public boolean isSystemGroup() {
    return systemGroup;
  }

  /**
   * Sets whether this is a system-managed group.
   *
   * @param systemGroup true for system groups
   */
  public void setSystemGroup(boolean systemGroup) {
    this.systemGroup = systemGroup;
  }

  /**
   * Returns whether this group inherits permissions from its parent.
   *
   * @return true if permissions are inherited
   */
  public boolean isInheritPermissions() {
    return inheritPermissions;
  }

  /**
   * Sets whether this group inherits permissions from its parent.
   *
   * @param inheritPermissions true to inherit permissions
   */
  public void setInheritPermissions(boolean inheritPermissions) {
    this.inheritPermissions = inheritPermissions;
  }

  /**
   * Returns the parent group.
   *
   * @return the parent group, or null if this is a top-level group
   */
  public Group getParentGroup() {
    return parentGroup;
  }

  /**
   * Sets the parent group.
   *
   * @param parentGroup the parent group
   */
  public void setParentGroup(Group parentGroup) {
    this.parentGroup = parentGroup;
  }

  /**
   * Returns the users who are members of this group.
   *
   * @return the set of member users
   */
  public Set<User> getMembers() {
    return members;
  }

  /**
   * Sets the users who are members of this group.
   *
   * @param members the set of member users
   */
  public void setMembers(Set<User> members) {
    this.members = members;
  }

  /**
   * Adds a user to this group.
   *
   * @param user the user to add
   */
  public void addMember(User user) {
    members.add(user);
    user.getGroups().add(this);
  }

  /**
   * Removes a user from this group.
   *
   * @param user the user to remove
   */
  public void removeMember(User user) {
    members.remove(user);
    user.getGroups().remove(this);
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
   * Returns the username of the user who created this group.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this group.
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
   * Returns the username of the user who last modified this group.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the username of the user who last modified this group.
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
    Group group = (Group) o;
    return Objects.equals(groupId, group.groupId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId);
  }

  @Override
  public String toString() {
    return "Group{"
        + "id="
        + id
        + ", groupId="
        + groupId
        + ", name='"
        + name
        + '\''
        + ", systemGroup="
        + systemGroup
        + '}';
  }
}
