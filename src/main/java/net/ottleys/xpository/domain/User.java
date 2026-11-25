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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
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
 * User entity representing an authenticated user in the system.
 *
 * <p>Users can be members of groups and can be granted permissions directly. Supports both internal
 * authentication and external authentication systems via username mapping.
 *
 * <p>Uses dual ID pattern: internal Long ID for database operations and public UUID for API access.
 *
 * <p>Indexed by Hibernate Search for full-text search on username, email, display name, and exact
 * match on userId.
 */
@Entity
@Table(name = "users")
@Indexed
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true, updatable = false)
  @KeywordField
  private UUID userId;

  @NotBlank(message = "Username is required")
  @Size(max = 100, message = "Username cannot exceed 100 characters")
  @Column(name = "username", nullable = false, unique = true, length = 100)
  @FullTextField
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  @Size(max = 255, message = "Email cannot exceed 255 characters")
  @Column(name = "email", nullable = false, unique = true)
  @FullTextField
  private String email;

  @Size(max = 255, message = "Display name cannot exceed 255 characters")
  @Column(name = "display_name")
  @FullTextField
  private String displayName;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "account_locked", nullable = false)
  private boolean accountLocked = false;

  @Column(name = "account_expired", nullable = false)
  private boolean accountExpired = false;

  @Column(name = "credentials_expired", nullable = false)
  private boolean credentialsExpired = false;

  @Column(name = "last_login_date")
  private LocalDateTime lastLoginDate;

  @ManyToMany
  @JoinTable(
      name = "user_group_membership",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "group_id"))
  private Set<Group> groups = new HashSet<>();

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
  public User() {}

  /** Generates a new UUID for the user on creation. */
  @PrePersist
  protected void onCreate() {
    if (userId == null) {
      userId = UUID.randomUUID();
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
   * @return the user UUID
   */
  public UUID getUserId() {
    return userId;
  }

  /**
   * Sets the public UUID identifier.
   *
   * @param userId the user UUID
   */
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  /**
   * Returns the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username the username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Returns the email address.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address.
   *
   * @param email the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Returns the display name.
   *
   * @return the display name, or null if not set
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Returns the password hash.
   *
   * @return the password hash, or null if not set
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Sets the password hash.
   *
   * @param passwordHash the password hash
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Returns whether the user account is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether the user account is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns whether the user account is locked.
   *
   * @return true if locked
   */
  public boolean isAccountLocked() {
    return accountLocked;
  }

  /**
   * Sets whether the user account is locked.
   *
   * @param accountLocked true to lock
   */
  public void setAccountLocked(boolean accountLocked) {
    this.accountLocked = accountLocked;
  }

  /**
   * Returns whether the user account is expired.
   *
   * @return true if expired
   */
  public boolean isAccountExpired() {
    return accountExpired;
  }

  /**
   * Sets whether the user account is expired.
   *
   * @param accountExpired true if expired
   */
  public void setAccountExpired(boolean accountExpired) {
    this.accountExpired = accountExpired;
  }

  /**
   * Returns whether the user credentials are expired.
   *
   * @return true if expired
   */
  public boolean isCredentialsExpired() {
    return credentialsExpired;
  }

  /**
   * Sets whether the user credentials are expired.
   *
   * @param credentialsExpired true if expired
   */
  public void setCredentialsExpired(boolean credentialsExpired) {
    this.credentialsExpired = credentialsExpired;
  }

  /**
   * Returns the last login timestamp.
   *
   * @return the last login date, or null if never logged in
   */
  public LocalDateTime getLastLoginDate() {
    return lastLoginDate;
  }

  /**
   * Sets the last login timestamp.
   *
   * @param lastLoginDate the last login date
   */
  public void setLastLoginDate(LocalDateTime lastLoginDate) {
    this.lastLoginDate = lastLoginDate;
  }

  /**
   * Returns the groups this user belongs to.
   *
   * @return the set of groups
   */
  public Set<Group> getGroups() {
    return groups;
  }

  /**
   * Sets the groups this user belongs to.
   *
   * @param groups the set of groups
   */
  public void setGroups(Set<Group> groups) {
    this.groups = groups;
  }

  /**
   * Adds a group to this user.
   *
   * @param group the group to add
   */
  public void addGroup(Group group) {
    groups.add(group);
    group.getMembers().add(this);
  }

  /**
   * Removes a group from this user.
   *
   * @param group the group to remove
   */
  public void removeGroup(Group group) {
    groups.remove(group);
    group.getMembers().remove(this);
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
   * Returns the username of the user who created this user.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this user.
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
   * Returns the username of the user who last modified this user.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the username of the user who last modified this user.
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
    User user = (User) o;
    return Objects.equals(userId, user.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }

  @Override
  public String toString() {
    return "User{"
        + "id="
        + id
        + ", userId="
        + userId
        + ", username='"
        + username
        + '\''
        + ", email='"
        + email
        + '\''
        + ", enabled="
        + enabled
        + '}';
  }
}
