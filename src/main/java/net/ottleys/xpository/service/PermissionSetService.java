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
package net.ottleys.xpository.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.PermissionSet;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.PermissionSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for PermissionSet management operations.
 *
 * <p>Provides business logic for managing external user access through permission sets. Supports
 * CRUD operations, access token generation, expiration management, and validation. All operations
 * are transactional and include validation.
 */
@Service
@Transactional
public class PermissionSetService {

  private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final PermissionSetRepository permissionSetRepository;

  /**
   * Constructs a PermissionSetService with required dependencies.
   *
   * @param permissionSetRepository repository for permission set data access
   */
  public PermissionSetService(PermissionSetRepository permissionSetRepository) {
    this.permissionSetRepository = permissionSetRepository;
  }

  /**
   * Creates a new permission set with an automatically generated access token.
   *
   * @param permissionSet the permission set to create
   * @return the created permission set with access token
   * @throws BusinessValidationException if name already exists
   */
  public PermissionSet createPermissionSet(PermissionSet permissionSet) {
    if (permissionSetRepository.existsByName(permissionSet.getName())) {
      throw new BusinessValidationException(
          "Permission set name already exists: " + permissionSet.getName());
    }

    // Generate unique access token
    String accessToken = generateAccessToken();
    while (permissionSetRepository.existsByAccessToken(accessToken)) {
      accessToken = generateAccessToken();
    }

    permissionSet.setAccessToken(accessToken);
    return permissionSetRepository.save(permissionSet);
  }

  /**
   * Updates an existing permission set.
   *
   * @param permissionSetId the permission set UUID
   * @param updatedPermissionSet the updated permission set data
   * @return the updated permission set
   * @throws BusinessValidationException if permission set not found or validation fails
   */
  public PermissionSet updatePermissionSet(
      UUID permissionSetId, PermissionSet updatedPermissionSet) {
    PermissionSet existingPermissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    // Check name uniqueness if changed
    if (!existingPermissionSet.getName().equals(updatedPermissionSet.getName())
        && permissionSetRepository.existsByName(updatedPermissionSet.getName())) {
      throw new BusinessValidationException(
          "Permission set name already exists: " + updatedPermissionSet.getName());
    }

    existingPermissionSet.setName(updatedPermissionSet.getName());
    existingPermissionSet.setDescription(updatedPermissionSet.getDescription());
    existingPermissionSet.setPermissionsJson(updatedPermissionSet.getPermissionsJson());
    existingPermissionSet.setEnabled(updatedPermissionSet.isEnabled());
    existingPermissionSet.setExpiresDate(updatedPermissionSet.getExpiresDate());

    return permissionSetRepository.save(existingPermissionSet);
  }

  /**
   * Deletes a permission set by UUID.
   *
   * @param permissionSetId the permission set UUID
   * @throws BusinessValidationException if permission set not found
   */
  public void deletePermissionSet(UUID permissionSetId) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    permissionSetRepository.delete(permissionSet);
  }

  /**
   * Finds a permission set by UUID.
   *
   * @param permissionSetId the permission set UUID
   * @return Optional containing the permission set if found
   */
  @Transactional(readOnly = true)
  public Optional<PermissionSet> findByPermissionSetId(UUID permissionSetId) {
    return permissionSetRepository.findByPermissionSetId(permissionSetId);
  }

  /**
   * Finds a permission set by name.
   *
   * @param name the permission set name
   * @return Optional containing the permission set if found
   */
  @Transactional(readOnly = true)
  public Optional<PermissionSet> findByName(String name) {
    return permissionSetRepository.findByName(name);
  }

  /**
   * Finds a permission set by access token.
   *
   * @param accessToken the access token
   * @return Optional containing the permission set if found
   */
  @Transactional(readOnly = true)
  public Optional<PermissionSet> findByAccessToken(String accessToken) {
    return permissionSetRepository.findByAccessToken(accessToken);
  }

  /**
   * Finds a valid permission set by access token.
   *
   * <p>Returns the permission set only if it is enabled and not expired.
   *
   * @param accessToken the access token
   * @return Optional containing the valid permission set if found
   */
  @Transactional(readOnly = true)
  public Optional<PermissionSet> findValidByAccessToken(String accessToken) {
    return permissionSetRepository.findValidByAccessToken(accessToken);
  }

  /**
   * Lists all permission sets.
   *
   * @return list of all permission sets
   */
  @Transactional(readOnly = true)
  public List<PermissionSet> findAll() {
    return permissionSetRepository.findAll();
  }

  /**
   * Lists all enabled permission sets.
   *
   * @return list of enabled permission sets
   */
  @Transactional(readOnly = true)
  public List<PermissionSet> findEnabled() {
    return permissionSetRepository.findByEnabledTrue();
  }

  /**
   * Lists all valid (enabled and not expired) permission sets.
   *
   * @return list of valid permission sets
   */
  @Transactional(readOnly = true)
  public List<PermissionSet> findValid() {
    return permissionSetRepository.findValid();
  }

  /**
   * Lists all expired permission sets.
   *
   * @return list of expired permission sets
   */
  @Transactional(readOnly = true)
  public List<PermissionSet> findExpired() {
    return permissionSetRepository.findExpired();
  }

  /**
   * Enables a permission set.
   *
   * @param permissionSetId the permission set UUID
   * @throws BusinessValidationException if permission set not found
   */
  public void enable(UUID permissionSetId) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    permissionSet.setEnabled(true);
    permissionSetRepository.save(permissionSet);
  }

  /**
   * Disables a permission set.
   *
   * @param permissionSetId the permission set UUID
   * @throws BusinessValidationException if permission set not found
   */
  public void disable(UUID permissionSetId) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    permissionSet.setEnabled(false);
    permissionSetRepository.save(permissionSet);
  }

  /**
   * Regenerates the access token for a permission set.
   *
   * @param permissionSetId the permission set UUID
   * @return the updated permission set with new access token
   * @throws BusinessValidationException if permission set not found
   */
  public PermissionSet regenerateAccessToken(UUID permissionSetId) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    String newAccessToken = generateAccessToken();
    while (permissionSetRepository.existsByAccessToken(newAccessToken)) {
      newAccessToken = generateAccessToken();
    }

    permissionSet.setAccessToken(newAccessToken);
    return permissionSetRepository.save(permissionSet);
  }

  /**
   * Sets the expiration date for a permission set.
   *
   * @param permissionSetId the permission set UUID
   * @param expiresDate the expiration date
   * @throws BusinessValidationException if permission set not found
   */
  public void setExpiration(UUID permissionSetId, LocalDateTime expiresDate) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    permissionSet.setExpiresDate(expiresDate);
    permissionSetRepository.save(permissionSet);
  }

  /**
   * Removes expiration from a permission set (makes it permanent).
   *
   * @param permissionSetId the permission set UUID
   * @throws BusinessValidationException if permission set not found
   */
  public void removeExpiration(UUID permissionSetId) {
    PermissionSet permissionSet =
        permissionSetRepository
            .findByPermissionSetId(permissionSetId)
            .orElseThrow(() ->
                new BusinessValidationException("Permission set not found: " + permissionSetId));

    permissionSet.setExpiresDate(null);
    permissionSetRepository.save(permissionSet);
  }

  /**
   * Searches permission sets by name.
   *
   * @param searchTerm the search term
   * @return list of matching permission sets
   */
  @Transactional(readOnly = true)
  public List<PermissionSet> searchPermissionSets(String searchTerm) {
    return permissionSetRepository.findByNameContainingIgnoreCase(searchTerm);
  }

  /**
   * Counts enabled permission sets.
   *
   * @return count of enabled permission sets
   */
  @Transactional(readOnly = true)
  public long countEnabled() {
    return permissionSetRepository.countByEnabledTrue();
  }

  /**
   * Counts valid (enabled and not expired) permission sets.
   *
   * @return count of valid permission sets
   */
  @Transactional(readOnly = true)
  public long countValid() {
    return permissionSetRepository.countValid();
  }

  /**
   * Counts expired permission sets.
   *
   * @return count of expired permission sets
   */
  @Transactional(readOnly = true)
  public long countExpired() {
    return permissionSetRepository.countExpired();
  }

  /**
   * Generates a cryptographically secure random access token.
   *
   * @return Base64-encoded access token
   */
  private String generateAccessToken() {
    byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }
}
