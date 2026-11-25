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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
 * PermissionSet entity for managing external user access.
 *
 * <p>Permission sets are pre-configured bundles of permissions that can be granted to external
 * users via API tokens or access keys. This allows controlled access to specific resources without
 * creating full user accounts.
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>API integrations with limited scope
 *   <li>Temporary access for external contractors
 *   <li>Public read-only access to specific collections
 *   <li>Service-to-service authentication
 * </ul>
 *
 * <p>Each permission set has a unique access token that external users can present for
 * authentication. The permissions JSON defines the allowed operations.
 *
 * <p>Uses dual ID pattern: internal Long ID for database operations and public UUID for API access.
 *
 * <p>Indexed by Hibernate Search for full-text search on name, description, and exact match on
 * permissionSetId.
 */
@Entity
@Table(name = "permission_sets")
@Indexed
@EntityListeners(AuditingEntityListener.class)
public class PermissionSet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "permission_set_id", nullable = false, unique = true, updatable = false)
  @KeywordField
  private UUID permissionSetId;

  @NotBlank(message = "Permission set name is required")
  @Size(max = 100, message = "Permission set name cannot exceed 100 characters")
  @Column(name = "name", nullable = false, unique = true, length = 100)
  @FullTextField
  private String name;

  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  @Column(name = "description", length = 1000)
  @FullTextField
  private String description;

  @NotBlank(message = "Permissions JSON is required")
  @Column(name = "permissions_json", nullable = false, columnDefinition = "TEXT")
  private String permissionsJson;

  @Column(name = "access_token", unique = true, length = 255)
  private String accessToken;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "expires_date")
  private LocalDateTime expiresDate;

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
  public PermissionSet() {}

  /** Generates a new UUID for the permission set on creation. */
  @PrePersist
  protected void onCreate() {
    if (permissionSetId == null) {
      permissionSetId = UUID.randomUUID();
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
   * @return the permission set UUID
   */
  public UUID getPermissionSetId() {
    return permissionSetId;
  }

  /**
   * Sets the public UUID identifier.
   *
   * @param permissionSetId the permission set UUID
   */
  public void setPermissionSetId(UUID permissionSetId) {
    this.permissionSetId = permissionSetId;
  }

  /**
   * Returns the permission set name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the permission set name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the permissions JSON.
   *
   * <p>JSON format:
   *
   * <pre>
   * {
   *   "permissions": [
   *     {"resource": "collection-uuid", "action": "READ"},
   *     {"resource": "element-uuid", "action": "WRITE"}
   *   ]
   * }
   * </pre>
   *
   * @return the permissions JSON
   */
  public String getPermissionsJson() {
    return permissionsJson;
  }

  /**
   * Sets the permissions JSON.
   *
   * @param permissionsJson the permissions JSON
   */
  public void setPermissionsJson(String permissionsJson) {
    this.permissionsJson = permissionsJson;
  }

  /**
   * Returns the access token.
   *
   * <p>This token is used by external users to authenticate and access resources according to the
   * permissions defined in this set.
   *
   * @return the access token, or null if not generated
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Sets the access token.
   *
   * @param accessToken the access token
   */
  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  /**
   * Returns whether this permission set is enabled.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether this permission set is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns the expiration timestamp.
   *
   * @return the expiration date, or null if no expiration
   */
  public LocalDateTime getExpiresDate() {
    return expiresDate;
  }

  /**
   * Sets the expiration timestamp.
   *
   * @param expiresDate the expiration date
   */
  public void setExpiresDate(LocalDateTime expiresDate) {
    this.expiresDate = expiresDate;
  }

  /**
   * Returns whether this permission set has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return expiresDate != null && LocalDateTime.now().isAfter(expiresDate);
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
   * Returns the username of the user who created this permission set.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this permission set.
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
   * Returns the username of the user who last modified this permission set.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the username of the user who last modified this permission set.
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
    PermissionSet that = (PermissionSet) o;
    return Objects.equals(permissionSetId, that.permissionSetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionSetId);
  }

  @Override
  public String toString() {
    return "PermissionSet{"
        + "id="
        + id
        + ", permissionSetId="
        + permissionSetId
        + ", name='"
        + name
        + '\''
        + ", enabled="
        + enabled
        + ", expired="
        + isExpired()
        + '}';
  }
}
