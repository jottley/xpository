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
package net.ottleys.xpository.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.PermissionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for PermissionSet operations.
 *
 * <p>Provides CRUD operations and custom queries for external user permission management, including
 * lookups by UUID, name, access token, and expiration queries.
 */
public interface PermissionSetRepository extends JpaRepository<PermissionSet, Long> {

  /**
   * Finds a permission set by its public UUID.
   *
   * @param permissionSetId the permission set UUID
   * @return Optional containing the permission set if found
   */
  Optional<PermissionSet> findByPermissionSetId(UUID permissionSetId);

  /**
   * Finds a permission set by name.
   *
   * @param name the permission set name
   * @return Optional containing the permission set if found
   */
  Optional<PermissionSet> findByName(String name);

  /**
   * Finds a permission set by access token.
   *
   * @param accessToken the access token
   * @return Optional containing the permission set if found
   */
  Optional<PermissionSet> findByAccessToken(String accessToken);

  /**
   * Checks if a permission set exists with the given name.
   *
   * @param name the permission set name
   * @return true if permission set exists
   */
  boolean existsByName(String name);

  /**
   * Checks if a permission set exists with the given access token.
   *
   * @param accessToken the access token
   * @return true if permission set exists
   */
  boolean existsByAccessToken(String accessToken);

  /**
   * Finds all enabled permission sets.
   *
   * @return list of enabled permission sets
   */
  List<PermissionSet> findByEnabledTrue();

  /**
   * Finds all disabled permission sets.
   *
   * @return list of disabled permission sets
   */
  List<PermissionSet> findByEnabledFalse();

  /**
   * Finds all permission sets that expire before a given date.
   *
   * @param date the expiration date threshold
   * @return list of expiring permission sets
   */
  List<PermissionSet> findByExpiresDateBefore(LocalDateTime date);

  /**
   * Finds all permission sets that expire after a given date.
   *
   * @param date the expiration date threshold
   * @return list of permission sets expiring after date
   */
  List<PermissionSet> findByExpiresDateAfter(LocalDateTime date);

  /**
   * Finds all permission sets with no expiration date.
   *
   * @return list of non-expiring permission sets
   */
  List<PermissionSet> findByExpiresDateIsNull();

  /**
   * Finds all expired permission sets.
   *
   * @return list of expired permission sets
   */
  @Query(
      "SELECT ps FROM PermissionSet ps WHERE ps.expiresDate IS NOT NULL AND ps.expiresDate < CURRENT_TIMESTAMP")
  List<PermissionSet> findExpired();

  /**
   * Finds all valid (enabled and not expired) permission sets.
   *
   * @return list of valid permission sets
   */
  @Query(
      "SELECT ps FROM PermissionSet ps WHERE ps.enabled = true AND (ps.expiresDate IS NULL OR ps.expiresDate >= CURRENT_TIMESTAMP)")
  List<PermissionSet> findValid();

  /**
   * Finds a valid permission set by access token.
   *
   * <p>Returns the permission set only if it is enabled and not expired.
   *
   * @param accessToken the access token
   * @return Optional containing the valid permission set if found
   */
  @Query(
      "SELECT ps FROM PermissionSet ps WHERE ps.accessToken = :accessToken AND ps.enabled = true AND (ps.expiresDate IS NULL OR ps.expiresDate >= CURRENT_TIMESTAMP)")
  Optional<PermissionSet> findValidByAccessToken(@Param("accessToken") String accessToken);

  /**
   * Finds permission sets by name containing the search term (case-insensitive).
   *
   * @param name the search term
   * @return list of matching permission sets
   */
  List<PermissionSet> findByNameContainingIgnoreCase(String name);

  /**
   * Counts enabled permission sets.
   *
   * @return count of enabled permission sets
   */
  long countByEnabledTrue();

  /**
   * Counts expired permission sets.
   *
   * @return count of expired permission sets
   */
  @Query(
      "SELECT COUNT(ps) FROM PermissionSet ps WHERE ps.expiresDate IS NOT NULL AND ps.expiresDate < CURRENT_TIMESTAMP")
  long countExpired();

  /**
   * Counts valid (enabled and not expired) permission sets.
   *
   * @return count of valid permission sets
   */
  @Query(
      "SELECT COUNT(ps) FROM PermissionSet ps WHERE ps.enabled = true AND (ps.expiresDate IS NULL OR ps.expiresDate >= CURRENT_TIMESTAMP)")
  long countValid();
}
