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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.event.AfterElementCreateEvent;
import net.ottleys.xpository.event.AfterElementDeleteEvent;
import net.ottleys.xpository.event.AfterElementUpdateEvent;
import net.ottleys.xpository.event.BeforeElementCreateEvent;
import net.ottleys.xpository.event.BeforeElementDeleteEvent;
import net.ottleys.xpository.event.BeforeElementUpdateEvent;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementSpecifications;
import net.ottleys.xpository.repository.ElementTypeRepository;

/**
 * Service layer for Element business logic.
 *
 * <p>Handles CRUD operations, validation, and business rules for elements. All public methods are
 * transactional to ensure data consistency. Read-only operations use readOnly=true for performance
 * optimization.
 */
@Service
@Transactional
public class ElementService {

  private final ElementRepository elementRepository;
  private final ElementTypeRepository elementTypeRepository;
  private final JsonSchemaValidationService schemaValidationService;
  private final ApplicationEventPublisher eventPublisher;

  private final JdbcTemplate jdbcTemplate;

  /**
   * Constructs an ElementService with required dependencies.
   *
   * @param elementRepository repository for element data access
   * @param elementTypeRepository repository for element type validation
   * @param schemaValidationService service for JSON schema validation
   * @param eventPublisher publisher for element lifecycle events
   */
  public ElementService(
      ElementRepository elementRepository,
      ElementTypeRepository elementTypeRepository,
      JsonSchemaValidationService schemaValidationService,
      ApplicationEventPublisher eventPublisher,
      JdbcTemplate jdbcTemplate) {
    this.elementRepository = elementRepository;
    this.elementTypeRepository = elementTypeRepository;
    this.schemaValidationService = schemaValidationService;
    this.eventPublisher = eventPublisher;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Retrieves all elements with pagination.
   *
   * <p>Eagerly loads element types to prevent LazyInitializationException in REST responses.
   *
   * @param pageable pagination parameters
   * @return page of elements with types loaded
   */
  @Transactional(readOnly = true)
  public Page<Element> findAll(Pageable pageable) {
    return elementRepository.findAllWithTypes(pageable);
  }

  /**
   * Retrieves elements that belong to a given collection, paginated.
   *
   * @param collectionId the public UUID of the collection
   * @param pageable pagination parameters (page, size, sort)
   * @return a page of elements belonging to the collection
   */
  @Transactional(readOnly = true)
  public Page<Element> findByCollectionId(UUID collectionId, Pageable pageable) {
    return elementRepository.findByCollectionId(collectionId, pageable);
  }

  /**
   * Searches and filters elements with dynamic criteria.
   *
   * <p>Supports filtering by element type name, created by user, and modified by user. Multiple
   * filters are combined with AND logic. Eagerly loads element types to prevent
   * LazyInitializationException in REST responses.
   *
   * @param elementTypeName filter by element type name (namespace format)
   * @param createdBy filter by creator username
   * @param modifiedBy filter by modifier username
   * @param pageable pagination parameters
   * @return page of elements matching the criteria with types loaded
   */
  @Transactional(readOnly = true)
  public Page<Element> searchElements(
      String elementTypeName, String createdBy, String modifiedBy, Pageable pageable) {

    // Build dynamic query using specifications - start with non-deleted filter
    Specification<Element> spec = Specification.where(ElementSpecifications.isNotDeleted());

    // Add element type filter if provided
    if (elementTypeName != null && !elementTypeName.isBlank()) {
      spec = spec.and(ElementSpecifications.hasElementType(elementTypeName.trim()));
    }

    // Add created by filter if provided
    if (createdBy != null && !createdBy.isBlank()) {
      spec = spec.and(ElementSpecifications.createdByUser(createdBy.trim()));
    }

    // Add modified by filter if provided
    if (modifiedBy != null && !modifiedBy.isBlank()) {
      spec = spec.and(ElementSpecifications.modifiedByUser(modifiedBy.trim()));
    }

    // Eagerly fetch element type to prevent lazy loading issues
    spec = spec.and(ElementSpecifications.fetchType());

    return elementRepository.findAll(spec, pageable);
  }

  /**
   * Retrieves an element by its public UUID.
   *
   * <p>Eagerly loads element type to prevent LazyInitializationException in REST responses.
   *
   * @param elementId the public UUID
   * @return Optional containing the element if found
   */
  @Transactional(readOnly = true)
  public Optional<Element> findByElementId(UUID elementId) {
    return elementRepository.findByElementIdWithType(elementId);
  }

  /**
   * Creates a new element.
   *
   * <p>Generates a new UUID and sets creation timestamps. Validates that the element type exists
   * and validates the propertiesJson against the element type's schema (if defined). The element is
   * saved and returned with its generated ID.
   *
   * @param element the element to create (without ID)
   * @return the saved element with generated ID and UUID
   * @throws IllegalArgumentException if element type does not exist or schema validation fails
   */
  public Element create(Element element) {
    // Get current username
    String username = getCurrentUsername();

    // Publish before-create event
    eventPublisher.publishEvent(new BeforeElementCreateEvent(this, element, username));

    // Validate element type exists
    validateElementType(element.getType());

    // Validate propertiesJson against schema if schema is defined
    validatePropertiesAgainstSchema(element);

    // UUID is auto-generated via @PrePersist
    // Timestamps are auto-generated via @PrePersist
    Element savedElement = elementRepository.save(element);

    // Publish after-create event
    eventPublisher.publishEvent(new AfterElementCreateEvent(this, savedElement, username));

    return savedElement;
  }

  /**
   * Updates an existing element with a minor version increment (default).
   *
   * <p>Updates modification timestamp automatically. Does not modify creation fields or UUID.
   * Validates element type if changed and validates the propertiesJson against the schema.
   *
   * @param element the element with updated fields
   * @return the updated element
   * @throws IllegalArgumentException if element type does not exist or schema validation fails
   */
  public Element update(Element element) {
    return update(element, false);
  }

  /**
   * Updates an existing element, automatically incrementing its semantic version.
   *
   * <p>Updates modification timestamp automatically. Does not modify creation fields or UUID.
   * Validates element type if changed and validates the propertiesJson against the schema.
   *
   * <p>If isMajorVersion is true, majorVersion is incremented by 1 and minorVersion is reset to 0.
   * If isMajorVersion is false, minorVersion is incremented by 1. A snapshot of the pre-update
   * state is captured and published in an event.
   *
   * @param element the element with updated fields
   * @param isMajorVersion true to increment major version (breaking change), false for minor
   *     version
   * @return the updated element
   * @throws IllegalArgumentException if element type does not exist or schema validation fails
   */
  public Element update(Element element, boolean isMajorVersion) {
    // Get current username
    String username = getCurrentUsername();

    // Try to load old element state for comparison (may not exist in some test scenarios)
    // Create a detached copy to prevent JPA from updating it when we save the new element
    Element oldElementSnapshot = null;
    if (element.getId() != null) {
      Element oldElement = elementRepository.findById(element.getId()).orElse(null);
      if (oldElement != null) {
        oldElementSnapshot = cloneElementState(oldElement);
      }
    }

    // Publish before-update event (oldElementSnapshot may be null)
    if (oldElementSnapshot != null) {
      eventPublisher.publishEvent(
          new BeforeElementUpdateEvent(this, oldElementSnapshot, element, username));
    }

    // Validate element type exists if specified
    validateElementType(element.getType());

    // Validate propertiesJson against schema if schema is defined
    validatePropertiesAgainstSchema(element);

    // Increment semantic version
    int currentMajor = element.getMajorVersion() != null ? element.getMajorVersion() : 1;
    int currentMinor = element.getMinorVersion() != null ? element.getMinorVersion() : 0;
    if (isMajorVersion) {
      element.setMajorVersion(currentMajor + 1);
      element.setMinorVersion(0);
    } else {
      element.setMajorVersion(currentMajor);
      element.setMinorVersion(currentMinor + 1);
    }

    // Modification timestamp updated via @PreUpdate
    Element savedElement = elementRepository.save(element);

    // Publish after-update event (oldElementSnapshot may be null)
    if (oldElementSnapshot != null) {
      eventPublisher.publishEvent(
          new AfterElementUpdateEvent(this, oldElementSnapshot, savedElement, username));
    }

    return savedElement;
  }

  /**
   * Soft deletes an element by UUID.
   *
   * <p>Marks the element as deleted without removing it from the database. Sets deleted flag and
   * deletion timestamp. Uses default transaction isolation to prevent race conditions - if the
   * element is already deleted by another transaction, this method will return false.
   *
   * @param elementId the public UUID of the element to delete
   * @param deletedBy username of user performing deletion
   * @return true if element was found and deleted, false if not found or already deleted
   */
  public boolean deleteByElementId(UUID elementId, String deletedBy) {
    // findByElementId filters deleted=false, so already-deleted elements return empty
    // This prevents race conditions where two concurrent deletes both succeed
    Optional<Element> elementOpt = elementRepository.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return false;
    }

    Element element = elementOpt.get();

    // Publish before-delete event
    eventPublisher.publishEvent(new BeforeElementDeleteEvent(this, element, deletedBy));

    element.softDelete(deletedBy);
    elementRepository.save(element);

    // Publish after-delete event
    eventPublisher.publishEvent(new AfterElementDeleteEvent(this, element, deletedBy));

    return true;
  }

  /**
   * Checks if an element exists by its public UUID.
   *
   * @param elementId the public UUID
   * @return true if an element exists with this UUID
   */
  @Transactional(readOnly = true)
  public boolean existsByElementId(UUID elementId) {
    return elementRepository.existsByElementId(elementId);
  }

  /**
   * Validates propertiesJson against the element type's JSON schema.
   *
   * <p>If the element type has a schemaJson defined, validates the element's propertiesJson against
   * it. Returns silently if no schema is defined or if validation passes. Throws exception with
   * detailed error messages if validation fails.
   *
   * @param element the element to validate
   * @throws IllegalArgumentException if schema validation fails
   */
  private void validatePropertiesAgainstSchema(Element element) {
    if (element.getType() == null || element.getType().getSchemaJson() == null) {
      // No schema to validate against
      return;
    }

    String schemaJson = element.getType().getSchemaJson();
    String propertiesJson = element.getPropertiesJson();

    List<String> validationErrors = schemaValidationService.validate(propertiesJson, schemaJson);

    if (!validationErrors.isEmpty()) {
      String errorMessage =
          String.format(
              "Element properties do not match the schema for type '%s'. Errors: %s",
              element.getType().getName(), String.join("; ", validationErrors));
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Validates that an element type exists.
   *
   * <p>Checks that the element type reference exists in the database. This prevents invalid
   * references where an element claims to be of a type that doesn't exist.
   *
   * @param elementType the element type to validate (must not be null)
   * @throws IllegalArgumentException if element type does not exist in the database
   */
  protected void validateElementType(ElementType elementType) {
    if (elementType == null) {
      throw new IllegalArgumentException("Element type is required");
    }

    if (elementType.getId() == null) {
      throw new IllegalArgumentException("Element type must be persisted before use");
    }

    Optional<ElementType> existingType = elementTypeRepository.findByName(elementType.getName());
    if (existingType.isEmpty()) {
      throw new IllegalArgumentException(
          "Element type with name " + elementType.getName() + " does not exist");
    }
  }

  /**
   * Checks if an element type exists in the database.
   *
   * @param elementType the element type name to check
   * @return true if the element type exists, false otherwise
   */
  public boolean isValidElementType(String elementType) {
    if (elementType == null || elementType.isEmpty()) {
      return false;
    }

    return elementTypeRepository.findByName(elementType).isPresent();
  }

  /**
   * Creates a detached copy of an element's state for event publishing.
   *
   * <p>This copy preserves the element's state at a point in time and prevents JPA from updating it
   * when the original element is saved. The copy is not managed by the persistence context.
   *
   * @param element the element to clone
   * @return a detached copy of the element
   */
  private Element cloneElementState(Element element) {
    Element clone = new Element();
    clone.setId(element.getId());
    clone.setVersion(element.getVersion());
    clone.setElementId(element.getElementId());
    clone.setType(element.getType());
    clone.setPropertiesJson(element.getPropertiesJson());
    clone.setMajorVersion(element.getMajorVersion());
    clone.setMinorVersion(element.getMinorVersion());
    clone.setCreatedDate(element.getCreatedDate());
    clone.setCreatedBy(element.getCreatedBy());
    clone.setModifiedDate(element.getModifiedDate());
    clone.setModifiedBy(element.getModifiedBy());
    return clone;
  }

  /**
   * Gets the current authenticated username.
   *
   * <p>Retrieves the username from Spring Security context. Returns "system" if no authentication
   * is present or if the user is anonymous.
   *
   * @return the current username or "system"
   */
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "system";
    }
    String username = authentication.getName();
    return username != null && !username.equals("anonymousUser") ? username : "system";
  }
}
