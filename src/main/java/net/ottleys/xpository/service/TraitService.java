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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.TraitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Trait business logic.
 *
 * <p>Handles CRUD operations, validation, and business rules for trait definitions. All public
 * methods are transactional to ensure data consistency. Read-only operations use readOnly=true for
 * performance optimization.
 */
@Service
@Transactional
public class TraitService {

  private final TraitRepository traitRepository;
  private final ElementTraitRepository elementTraitRepository;
  private final ObjectMapper objectMapper;

  /**
   * Constructs a TraitService with required dependencies.
   *
   * @param traitRepository repository for trait data access
   * @param elementTraitRepository repository for element-trait associations
   * @param objectMapper JSON object mapper for validation
   */
  public TraitService(
      TraitRepository traitRepository,
      ElementTraitRepository elementTraitRepository,
      ObjectMapper objectMapper) {
    this.traitRepository = traitRepository;
    this.elementTraitRepository = elementTraitRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Retrieves all traits with pagination.
   *
   * @param pageable pagination parameters
   * @return page of traits
   */
  @Transactional(readOnly = true)
  public Page<Trait> findAll(Pageable pageable) {
    return traitRepository.findAll(pageable);
  }

  /**
   * Searches for traits by name (case-insensitive partial match).
   *
   * @param name the search term
   * @return list of matching traits
   */
  @Transactional(readOnly = true)
  public List<Trait> searchByName(String name) {
    if (name == null || name.isBlank()) {
      return List.of();
    }
    return traitRepository.findByNameContainingIgnoreCase(name.trim());
  }

  /**
   * Retrieves traits by type.
   *
   * @param traitType the trait type
   * @return list of traits of the given type
   */
  @Transactional(readOnly = true)
  public List<Trait> findByType(Trait.TraitType traitType) {
    return traitRepository.findByTraitType(traitType);
  }

  /**
   * Retrieves all required traits.
   *
   * @return list of required traits
   */
  @Transactional(readOnly = true)
  public List<Trait> findRequired() {
    return traitRepository.findByRequiredTrue();
  }

  /**
   * Retrieves all inheritable traits.
   *
   * @return list of inheritable traits
   */
  @Transactional(readOnly = true)
  public List<Trait> findInheritable() {
    return traitRepository.findByInheritableTrue();
  }

  /**
   * Retrieves a trait by its public UUID.
   *
   * @param traitId the trait UUID
   * @return Optional containing the trait if found
   */
  @Transactional(readOnly = true)
  public Optional<Trait> findByTraitId(UUID traitId) {
    return traitRepository.findByTraitId(traitId);
  }

  /**
   * Retrieves a trait by name.
   *
   * @param name the trait name
   * @return Optional containing the trait if found
   */
  @Transactional(readOnly = true)
  public Optional<Trait> findByName(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return traitRepository.findByName(name.trim());
  }

  /**
   * Creates a new trait.
   *
   * <p>Validates that the trait name is unique and that the allowedValuesJson (if provided) is
   * valid JSON. The trait is saved and returned with its generated ID and UUID.
   *
   * @param trait the trait to create (without ID)
   * @return the saved trait with generated ID and UUID
   * @throws BusinessValidationException if name is not unique or allowedValuesJson is invalid
   */
  public Trait create(Trait trait) {
    // Validate name uniqueness
    if (traitRepository.existsByName(trait.getName())) {
      throw new BusinessValidationException(
          "A trait with name '" + trait.getName() + "' already exists");
    }

    // Validate allowedValuesJson if provided
    validateAllowedValuesJson(trait);

    // UUID is auto-generated via @PrePersist
    // Timestamps are auto-generated via auditing
    return traitRepository.save(trait);
  }

  /**
   * Updates an existing trait.
   *
   * <p>Updates modification timestamp automatically. Does not modify creation fields or UUID.
   * Validates that the name is unique (excluding current trait) and that allowedValuesJson is valid
   * JSON.
   *
   * @param trait the trait with updated fields
   * @return the updated trait
   * @throws BusinessValidationException if name conflicts or allowedValuesJson is invalid
   * @throws IllegalArgumentException if trait does not exist
   */
  public Trait update(Trait trait) {
    if (trait.getId() == null) {
      throw new IllegalArgumentException("Trait ID is required for update");
    }

    // Verify trait exists
    if (!traitRepository.existsById(trait.getId())) {
      throw new IllegalArgumentException("Trait with ID " + trait.getId() + " does not exist");
    }

    // Validate name uniqueness (excluding current trait)
    Optional<Trait> existingByName = traitRepository.findByName(trait.getName());
    if (existingByName.isPresent() && !existingByName.get().getId().equals(trait.getId())) {
      throw new BusinessValidationException(
          "A trait with name '" + trait.getName() + "' already exists");
    }

    // Validate allowedValuesJson if provided
    validateAllowedValuesJson(trait);

    // Modification timestamp updated via auditing
    return traitRepository.save(trait);
  }

  /**
   * Deletes a trait by UUID.
   *
   * <p>Permanently removes the trait and all associated element-trait associations. This is a
   * destructive operation that cannot be undone.
   *
   * @param traitId the public UUID of the trait to delete
   * @return true if trait was found and deleted, false if not found
   * @throws BusinessValidationException if trait is in use and cannot be safely deleted
   */
  public boolean deleteByTraitId(UUID traitId) {
    Optional<Trait> traitOpt = traitRepository.findByTraitId(traitId);
    if (traitOpt.isEmpty()) {
      return false;
    }

    Trait trait = traitOpt.get();

    // Check if trait is in use
    long usageCount = elementTraitRepository.countByTrait(trait);
    if (usageCount > 0) {
      throw new BusinessValidationException(
          "Cannot delete trait '"
              + trait.getName()
              + "' because it is used by "
              + usageCount
              + " element(s)");
    }

    traitRepository.delete(trait);
    return true;
  }

  /**
   * Force deletes a trait and all its associations.
   *
   * <p>Removes the trait and cascades deletion to all element-trait associations. Use with caution
   * as this is a destructive operation.
   *
   * @param traitId the public UUID of the trait to delete
   * @return true if trait was found and deleted, false if not found
   */
  public boolean forceDeleteByTraitId(UUID traitId) {
    Optional<Trait> traitOpt = traitRepository.findByTraitId(traitId);
    if (traitOpt.isEmpty()) {
      return false;
    }

    Trait trait = traitOpt.get();

    // Delete all element-trait associations first
    elementTraitRepository.deleteByTrait(trait);

    // Then delete the trait
    traitRepository.delete(trait);
    return true;
  }

  /**
   * Checks if a trait exists by its public UUID.
   *
   * @param traitId the public UUID
   * @return true if a trait exists with this UUID
   */
  @Transactional(readOnly = true)
  public boolean existsByTraitId(UUID traitId) {
    return traitRepository.findByTraitId(traitId).isPresent();
  }

  /**
   * Checks if a trait exists by name.
   *
   * @param name the trait name
   * @return true if a trait exists with this name
   */
  @Transactional(readOnly = true)
  public boolean existsByName(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    return traitRepository.existsByName(name.trim());
  }

  /**
   * Validates that allowedValuesJson is valid JSON if provided.
   *
   * <p>If allowedValuesJson is not null, verifies it can be parsed as a JSON array. Throws
   * exception with details if invalid.
   *
   * @param trait the trait to validate
   * @throws BusinessValidationException if allowedValuesJson is invalid
   */
  private void validateAllowedValuesJson(Trait trait) {
    String allowedValuesJson = trait.getAllowedValuesJson();

    if (allowedValuesJson == null || allowedValuesJson.isBlank()) {
      return;
    }

    try {
      JsonNode node = objectMapper.readTree(allowedValuesJson);
      if (!node.isArray()) {
        throw new BusinessValidationException(
            "allowedValuesJson must be a JSON array, e.g., [\"value1\", \"value2\"]");
      }
    } catch (JsonProcessingException e) {
      throw new BusinessValidationException(
          "allowedValuesJson is not valid JSON: " + e.getMessage());
    }
  }
}
