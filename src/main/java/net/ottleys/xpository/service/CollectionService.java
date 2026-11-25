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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementCollectionMembership;
import net.ottleys.xpository.event.AfterElementDeleteEvent;
import net.ottleys.xpository.event.BeforeElementDeleteEvent;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.CollectionSpecifications;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;

/**
 * Service layer for Collection business logic.
 *
 * <p>Handles CRUD operations, validation, and business rules for collections. All public methods
 * are transactional to ensure data consistency. Read-only operations use readOnly=true for
 * performance optimization.
 */
@Service
@Transactional
public class CollectionService {

  private final CollectionRepository collectionRepository;
  private final ElementTypeRepository elementTypeRepository;
  private final ElementCollectionMembershipRepository membershipRepository;
  private final ElementRepository elementRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructs a CollectionService with required dependencies.
   *
   * @param collectionRepository repository for collection data access
   * @param elementTypeRepository repository for element type validation
   * @param membershipRepository repository for collection membership operations
   * @param elementRepository repository for element persistence operations
   * @param eventPublisher publisher for domain lifecycle events
   */
  public CollectionService(
      CollectionRepository collectionRepository,
      ElementTypeRepository elementTypeRepository,
      ElementCollectionMembershipRepository membershipRepository,
      ElementRepository elementRepository,
      ApplicationEventPublisher eventPublisher) {
    this.collectionRepository = collectionRepository;
    this.elementTypeRepository = elementTypeRepository;
    this.membershipRepository = membershipRepository;
    this.elementRepository = elementRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Retrieves all non-deleted collections with pagination.
   *
   * <p>Eagerly loads allowed element types to prevent LazyInitializationException in REST
   * responses.
   *
   * @param pageable pagination parameters
   * @return page of collections with allowed types loaded
   */
  @Transactional(readOnly = true)
  public Page<Collection> findAll(Pageable pageable) {
    return collectionRepository.findAllWithTypes(pageable);
  }

  /**
   * Retrieves a collection by its UUID, including allowed element types.
   * @param collectionId
   * @return
   */
  @Transactional(readOnly = true)
  public Collection findById(UUID collectionId) {
    return collectionRepository.findByCollectionIdWithTypes(collectionId).orElse(null);
  }

  /**
   * Searches and filters collections with dynamic criteria.
   *
   * <p>Supports filtering by parent collection, type, system flag, and name search (partial match).
   * Multiple filters are combined with AND logic. Eagerly loads allowed element types to prevent
   * LazyInitializationException in REST responses.
   *
   * @param parentCollectionId filter by parent UUID (null for top-level, omit to ignore parent)
   * @param includeParentFilter whether to apply parent filter (true if parentCollectionId was
   *     explicitly provided, even if null)
   * @param type filter by collection type (LIST, TREE, GRAPH, STACK)
   * @param systemCollection filter by system collection flag
   * @param nameQuery search by name (case-insensitive partial match)
   * @param pageable pagination parameters
   * @return page of collections matching the criteria with types loaded
   */
  @Transactional(readOnly = true)
  public Page<Collection> searchCollections(
      UUID parentCollectionId,
      boolean includeParentFilter,
      CollectionType type,
      Boolean systemCollection,
      String nameQuery,
      Pageable pageable) {

    // Build dynamic query using specifications
    Specification<Collection> spec = Specification.where(CollectionSpecifications.isNotDeleted());

    // Add parent filter if explicitly requested
    if (includeParentFilter) {
      spec = spec.and(CollectionSpecifications.hasParent(parentCollectionId));
    }

    // Add type filter if provided
    if (type != null) {
      spec = spec.and(CollectionSpecifications.hasType(type));
    }

    // Add system collection filter if provided
    if (systemCollection != null) {
      spec = spec.and(CollectionSpecifications.isSystemCollection(systemCollection));
    }

    // Add name search if provided and not blank
    if (nameQuery != null && !nameQuery.isBlank()) {
      spec = spec.and(CollectionSpecifications.nameContains(nameQuery.trim()));
    }

    // Eagerly fetch element types to prevent lazy loading issues
    spec = spec.and(CollectionSpecifications.fetchElementTypes());

    return collectionRepository.findAll(spec, pageable);
  }

  /**
   * Retrieves a collection by its public UUID.
   *
   * <p>Eagerly loads allowed element types to prevent LazyInitializationException in REST
   * responses.
   *
   * @param collectionId the public UUID
   * @return Optional containing the collection if found and not deleted
   */
  @Transactional(readOnly = true)
  public Optional<Collection> findByCollectionId(UUID collectionId) {
    return collectionRepository.findByCollectionIdWithTypes(collectionId);
  }

  /**
   * Creates a new collection.
   *
   * <p>Generates a new UUID and sets creation timestamps. The collection is saved and returned with
   * its generated ID.
   *
   * <p>Validates that the parent collection exists if one is specified. Validates that all allowed
   * element types exist in the database.
   *
   * @param collection the collection to create (without ID)
   * @return the saved collection with generated ID and UUID
   * @throws IllegalArgumentException if parent collection does not exist or if any allowed element
   *     types do not exist
   */
  public Collection create(Collection collection) {
    // Validate parent collection exists if specified
    validateParentCollection(collection.getParentCollection());

    // Validate all allowed element types exist
    validateAllowedElementTypes(collection.getAllowedElementTypes());

    // UUID is auto-generated via @PrePersist
    // Timestamps are auto-generated via @PrePersist
    return collectionRepository.save(collection);
  }

  /**
   * Updates an existing collection.
   *
   * <p>Updates modification timestamp automatically. Does not modify creation fields or UUID.
   * Validates parent collection reference if changed to prevent orphan references and circular
   * hierarchies. Validates that all allowed element types exist in the database.
   *
   * @param collection the collection with updated fields
   * @return the updated collection
   * @throws IllegalArgumentException if parent collection is invalid, would create circular
   *     reference, or if any allowed element types do not exist
   */
  public Collection update(Collection collection) {
    // Validate parent collection exists if specified
    validateParentCollection(collection.getParentCollection());

    // Validate no circular reference if parent is being changed
    validateNoCircularReference(collection, collection.getParentCollection());

    // Validate all allowed element types exist
    validateAllowedElementTypes(collection.getAllowedElementTypes());

    // Modification timestamp updated via @PreUpdate
    return collectionRepository.save(collection);
  }

  /**
   * Soft deletes a collection by UUID.
   *
   * <p>Marks the collection as deleted without removing it from the database. Sets deleted flag and
   * deletion timestamp. Uses default transaction isolation to prevent race conditions - if the
   * collection is already deleted by another transaction, this method will return false.
   *
   * <p>System collections (systemCollection=true) cannot be deleted and will throw an exception.
   *
   * @param collectionId the public UUID of the collection to delete
   * @param deletedBy username of user performing deletion
   * @return true if collection was found and deleted, false if not found or already deleted
   * @throws IllegalStateException if attempting to delete a system collection
   */
  public boolean deleteByCollectionId(UUID collectionId, String deletedBy) {
    // findByCollectionId filters deleted=false, so already-deleted collections return empty
    // This prevents race conditions where two concurrent deletes both succeed
    Optional<Collection> collectionOpt = collectionRepository.findByCollectionId(collectionId);
    if (collectionOpt.isEmpty()) {
      return false;
    }

    Collection collection = collectionOpt.get();

    // Protect system collections from deletion
    if (collection.isSystemCollection()) {
      throw new IllegalStateException("Cannot delete system collection: " + collection.getName());
    }

    collection.softDelete(deletedBy);
    collectionRepository.save(collection);

    // 1. Cascade soft-delete active child collections recursively
    List<Collection> activeChildren = collectionRepository.findByParentCollectionActive(collection);
    for (Collection child : activeChildren) {
      deleteByCollectionId(child.getCollectionId(), deletedBy);
    }

    // 2. Cascade soft-delete elements that have no other active collection
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    for (ElementCollectionMembership m : memberships) {
      Element element = m.getElement();
      if (!element.isDeleted()) {
        long remainingActive =
            membershipRepository.countActiveMembershipsByElementAndCollectionNot(
                element, collection);
        if (remainingActive == 0) {
          eventPublisher.publishEvent(new BeforeElementDeleteEvent(this, element, deletedBy));
          element.softDelete(deletedBy);
          elementRepository.save(element);
          eventPublisher.publishEvent(new AfterElementDeleteEvent(this, element, deletedBy));
        }
      }
    }

    return true;
  }

  /**
   * Restores a soft-deleted collection and cascade-restores its elements and child collections.
   *
   * @param collectionId the public UUID of the collection to restore
   * @return true if collection was found and restored, false if not found or not deleted
   */
  public boolean restoreCollection(UUID collectionId) {
    Optional<Collection> collectionOpt =
        collectionRepository.findByCollectionIdIncludingDeleted(collectionId);
    if (collectionOpt.isEmpty() || !collectionOpt.get().isDeleted()) {
      return false;
    }

    Collection collection = collectionOpt.get();
    collection.restore();
    collectionRepository.save(collection);

    // 1. Restore child elements that belong to this collection and were soft-deleted
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    for (ElementCollectionMembership m : memberships) {
      Element element = m.getElement();
      if (element.isDeleted()) {
        element.restore();
        elementRepository.save(element);
      }
    }

    // 2. Cascade restore child collections that were deleted
    List<Collection> deletedChildren =
        collectionRepository.findByParentCollectionDeleted(collection);
    for (Collection child : deletedChildren) {
      restoreCollection(child.getCollectionId());
    }

    return true;
  }

  /**
   * Public API used by tests/controllers to delete a collection.
   *
   * <p>This method simply delegates to {@link #deleteByCollectionId(UUID, String)} so that the
   * existing system‑collection guard and soft‑delete logic are reused.
   *
   * @param collectionId the public UUID of the collection to delete
   * @param deletedBy username performing the deletion
   * @return {@code true} if the collection was successfully soft‑deleted
   * @throws IllegalStateException if the collection is a system collection
   */
  public boolean deleteCollection(UUID collectionId, String deletedBy) {
    return deleteByCollectionId(collectionId, deletedBy);
  }

  /**
   * Checks if a collection exists by its public UUID.
   *
   * @param collectionId the public UUID
   * @return true if a non-deleted collection exists with this UUID
   */
  @Transactional(readOnly = true)
  public boolean existsByCollectionId(UUID collectionId) {
    return collectionRepository.findByCollectionId(collectionId).isPresent();
  }

  /**
   * Finds all child collections of a parent collection.
   *
   * @param parentId the public UUID of the parent collection
   * @param pageable pagination parameters
   * @return page of child collections
   */
  @Transactional(readOnly = true)
  public Page<Collection> findByParentCollectionId(UUID parentId, Pageable pageable) {
    return collectionRepository.findByParentCollectionCollectionId(parentId, pageable);
  }

  /**
   * Finds all system collections.
   *
   * <p>System collections are created automatically by the application and cannot be deleted via
   * the API.
   *
   * @return list of system collections
   */
  @Transactional(readOnly = true)
  public java.util.List<Collection> findSystemCollections() {
    return collectionRepository.findBySystemCollectionTrue();
  }

  /**
   * Validates that a parent collection exists if specified.
   *
   * <p>Checks that the parent collection reference exists in the database and is not soft-deleted.
   * This prevents orphan references where a collection points to a non-existent parent.
   *
   * @param parentCollection the parent collection to validate (null is allowed for top-level
   *     collections)
   * @throws IllegalArgumentException if parent collection is specified but does not exist or is
   *     deleted
   */
  private void validateParentCollection(Collection parentCollection) {
    if (parentCollection == null) {
      return; // Top-level collections are allowed
    }

    // Parent must exist and not be deleted
    if (parentCollection.getId() == null) {
      throw new IllegalArgumentException("Parent collection must be persisted before use");
    }

    Optional<Collection> existingParent =
        collectionRepository.findByCollectionId(parentCollection.getCollectionId());
    if (existingParent.isEmpty()) {
      throw new IllegalArgumentException(
          "Parent collection with UUID "
              + parentCollection.getCollectionId()
              + " does not exist or is deleted");
    }
  }

  /**
   * Validates that updating a collection's parent does not create a circular reference.
   *
   * <p>Prevents cycles in the collection hierarchy where a collection becomes its own ancestor. For
   * example, if A is parent of B and B is parent of C, then C cannot become parent of A or B.
   *
   * @param collection the collection being updated
   * @param newParent the new parent collection (null is allowed for top-level)
   * @throws IllegalArgumentException if the new parent would create a circular reference
   */
  private void validateNoCircularReference(Collection collection, Collection newParent) {
    if (newParent == null) {
      return; // Moving to top-level - no circular reference possible
    }

    // Cannot be its own parent
    if (collection.getCollectionId().equals(newParent.getCollectionId())) {
      throw new IllegalArgumentException("A collection cannot be its own parent");
    }

    // Check if newParent is a descendant of collection (would create cycle)
    Collection current = newParent;
    while (current.getParentCollection() != null) {
      if (collection.getCollectionId().equals(current.getParentCollection().getCollectionId())) {
        throw new IllegalArgumentException(
            "Cannot set parent: would create circular reference. Collection "
                + newParent.getCollectionId()
                + " is a descendant of "
                + collection.getCollectionId());
      }
      // Load parent to continue traversal
      UUID parentId = current.getParentCollection().getCollectionId();
      Optional<Collection> parentOpt = collectionRepository.findByCollectionId(parentId);
      if (parentOpt.isEmpty()) {
        break; // Parent doesn't exist - will be caught by validateParentCollection
      }
      current = parentOpt.get();
    }
  }

  /**
   * Validates that all element types in the allowed list exist in the database.
   *
   * <p>Checks that each element type name in the collection's allowedElementTypes set corresponds
   * to an existing ElementType entity. This prevents invalid references where a collection claims
   * to support element types that don't exist.
   *
   * @param allowedElementTypes the set of element type names to validate (empty or null is allowed)
   * @throws IllegalArgumentException if any element type does not exist in the database
   */
  private void validateAllowedElementTypes(Set<String> allowedElementTypes) {
    if (allowedElementTypes == null || allowedElementTypes.isEmpty()) {
      return; // No element type restrictions - allows all types
    }

    // Find which element types don't exist
    List<String> invalidTypes =
        allowedElementTypes.stream()
            .filter(typeName -> !elementTypeRepository.existsByName(typeName))
            .collect(Collectors.toList());

    if (!invalidTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "The following element types do not exist: " + String.join(", ", invalidTypes));
    }
  }

  /**
   * Gets the collection hierarchy for permission inheritance.
   *
   * <p>Returns a list of collection UUIDs starting from the given collection and walking up the
   * parent chain to the root. Used for permission inheritance checks. If the collection has
   * inheritPermissions=false, only returns the collection itself.
   *
   * @param collectionId the starting collection UUID
   * @return list of collection UUIDs (current → parent → grandparent → ... → root)
   */
  @Transactional(readOnly = true)
  public List<String> getCollectionHierarchyForPermissions(UUID collectionId) {
    Collection collection =
        collectionRepository
            .findByCollectionId(collectionId)
            .orElseThrow(
                () -> new IllegalArgumentException("Collection not found: " + collectionId));

    List<String> hierarchy = new java.util.ArrayList<>();
    hierarchy.add(collection.getCollectionId().toString());

    // If collection doesn't inherit permissions, only return itself
    if (!collection.isInheritPermissions()) {
      return hierarchy;
    }

    // Walk up the parent chain
    Collection current = collection;
    int maxDepth = 100; // Prevent infinite loops
    int depth = 0;

    while (current.getParentCollection() != null && depth < maxDepth) {
      current = current.getParentCollection();
      hierarchy.add(current.getCollectionId().toString());

      // If we hit a collection that doesn't inherit, stop here
      if (!current.isInheritPermissions()) {
        break;
      }

      depth++;
    }

    return hierarchy;
  }
}
