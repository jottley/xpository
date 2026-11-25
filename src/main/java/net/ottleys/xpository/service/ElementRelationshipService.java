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

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementRelationship;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRelationshipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import org.springframework.stereotype.Service;

/**
 * Service for managing element relationships in GRAPH collections.
 *
 * <p>Handles creation, retrieval, and deletion of directed relationships between elements. Enforces
 * GRAPH collection type validation and prevents circular references (element cannot relate to
 * itself). Relationships are directed (source → target) with types like "depends-on", "references",
 * etc.
 */
@Service
public class ElementRelationshipService {

  private final ElementRelationshipRepository relationshipRepository;
  private final ElementRepository elementRepository;
  private final CollectionRepository collectionRepository;
  private final ElementCollectionMembershipRepository membershipRepository;

  public ElementRelationshipService(
      ElementRelationshipRepository relationshipRepository,
      ElementRepository elementRepository,
      CollectionRepository collectionRepository,
      ElementCollectionMembershipRepository membershipRepository) {
    this.relationshipRepository = relationshipRepository;
    this.elementRepository = elementRepository;
    this.collectionRepository = collectionRepository;
    this.membershipRepository = membershipRepository;
  }

  /**
   * Creates a relationship between two elements in a GRAPH collection.
   *
   * @param collectionId the GRAPH collection UUID
   * @param sourceElementId the source element UUID
   * @param targetElementId the target element UUID
   * @param relationshipType the relationship type (e.g., "depends-on")
   * @param relationshipMetadata optional JSON metadata for the relationship
   * @param createdBy the user creating the relationship
   * @return the created relationship
   * @throws BusinessValidationException if collection is not GRAPH type, elements don't exist, or
   *     relationship already exists
   */
  @Transactional
  public ElementRelationship createRelationship(
      UUID collectionId,
      UUID sourceElementId,
      UUID targetElementId,
      String relationshipType,
      String relationshipMetadata,
      String createdBy) {

    // Validate collection exists and is GRAPH type
    Collection collection =
        collectionRepository
            .findByCollectionId(collectionId)
            .orElseThrow(
                () -> new BusinessValidationException("Collection not found: " + collectionId));

    if (!CollectionType.GRAPH.equals(collection.getType())) {
      throw new BusinessValidationException(
          "Collection is not a GRAPH type. Relationships are only supported in GRAPH collections.");
    }

    // Validate elements exist
    Element sourceElement =
        elementRepository
            .findByElementId(sourceElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Source element not found: " + sourceElementId));

    Element targetElement =
        elementRepository
            .findByElementId(targetElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Target element not found: " + targetElementId));

    // Prevent self-referencing relationships
    if (sourceElementId.equals(targetElementId)) {
      throw new BusinessValidationException("Element cannot have a relationship with itself");
    }

    // Validate both elements are members of the collection
    if (!membershipRepository.existsByElementAndCollection(sourceElement, collection)) {
      throw new BusinessValidationException(
          "Source element is not a member of collection: " + collectionId);
    }

    if (!membershipRepository.existsByElementAndCollection(targetElement, collection)) {
      throw new BusinessValidationException(
          "Target element is not a member of collection: " + collectionId);
    }

    // Check if relationship already exists
    if (relationshipRepository
        .existsBySourceElementAndTargetElementAndRelationshipTypeAndCollection(
            sourceElement, targetElement, relationshipType, collection)) {
      throw new BusinessValidationException(
          "Relationship already exists between these elements with type: " + relationshipType);
    }

    // Create the relationship
    ElementRelationship relationship =
        new ElementRelationship(collection, sourceElement, targetElement, relationshipType);
    relationship.setRelationshipMetadata(relationshipMetadata);
    relationship.setCreatedBy(createdBy);

    return relationshipRepository.save(relationship);
  }

  /**
   * Gets all outgoing relationships from an element in a collection.
   *
   * @param collectionId the GRAPH collection UUID
   * @param sourceElementId the source element UUID
   * @return list of outgoing relationships
   * @throws BusinessValidationException if collection is not GRAPH type
   */
  public List<ElementRelationship> getOutgoingRelationships(
      UUID collectionId, UUID sourceElementId) {

    Collection collection = validateGraphCollection(collectionId);
    Element sourceElement = getElement(sourceElementId);

    return relationshipRepository.findBySourceElementAndCollection(sourceElement, collection);
  }

  /**
   * Gets all incoming relationships to an element in a collection.
   *
   * @param collectionId the GRAPH collection UUID
   * @param targetElementId the target element UUID
   * @return list of incoming relationships
   * @throws BusinessValidationException if collection is not GRAPH type
   */
  public List<ElementRelationship> getIncomingRelationships(
      UUID collectionId, UUID targetElementId) {

    Collection collection = validateGraphCollection(collectionId);
    Element targetElement = getElement(targetElementId);

    return relationshipRepository.findByTargetElementAndCollection(targetElement, collection);
  }

  /**
   * Gets all relationships of a specific type in a collection.
   *
   * @param collectionId the GRAPH collection UUID
   * @param relationshipType the relationship type
   * @return list of relationships with the specified type
   * @throws BusinessValidationException if collection is not GRAPH type
   */
  public List<ElementRelationship> getRelationshipsByType(
      UUID collectionId, String relationshipType) {

    Collection collection = validateGraphCollection(collectionId);

    return relationshipRepository.findByRelationshipTypeAndCollection(relationshipType, collection);
  }

  /**
   * Deletes a specific relationship between two elements.
   *
   * @param collectionId the GRAPH collection UUID
   * @param sourceElementId the source element UUID
   * @param targetElementId the target element UUID
   * @param relationshipType the relationship type
   * @throws BusinessValidationException if collection is not GRAPH type or relationship doesn't
   *     exist
   */
  @Transactional
  public void deleteRelationship(
      UUID collectionId, UUID sourceElementId, UUID targetElementId, String relationshipType) {

    Collection collection = validateGraphCollection(collectionId);
    Element sourceElement = getElement(sourceElementId);
    Element targetElement = getElement(targetElementId);

    // Verify relationship exists
    if (!relationshipRepository
        .existsBySourceElementAndTargetElementAndRelationshipTypeAndCollection(
            sourceElement, targetElement, relationshipType, collection)) {
      throw new BusinessValidationException("Relationship not found");
    }

    relationshipRepository.deleteBySourceElementAndTargetElementAndRelationshipTypeAndCollection(
        sourceElement, targetElement, relationshipType, collection);
  }

  /**
   * Deletes all relationships involving an element (as source or target).
   *
   * <p>Called internally when removing an element from a GRAPH collection.
   *
   * @param element the element
   * @param collection the GRAPH collection
   */
  @Transactional
  public void deleteAllRelationshipsForElement(Element element, Collection collection) {
    relationshipRepository.deleteByElementAndCollection(element, collection);
  }

  /**
   * Validates that a collection exists and is a GRAPH type.
   *
   * @param collectionId the collection UUID
   * @return the validated collection
   * @throws BusinessValidationException if collection doesn't exist or is not GRAPH type
   */
  private Collection validateGraphCollection(UUID collectionId) {
    Collection collection =
        collectionRepository
            .findByCollectionId(collectionId)
            .orElseThrow(
                () -> new BusinessValidationException("Collection not found: " + collectionId));

    if (!CollectionType.GRAPH.equals(collection.getType())) {
      throw new BusinessValidationException(
          "Collection is not a GRAPH type. Relationships are only supported in GRAPH collections.");
    }

    return collection;
  }

  /**
   * Gets an element by UUID.
   *
   * @param elementId the element UUID
   * @return the element
   * @throws BusinessValidationException if element doesn't exist
   */
  private Element getElement(UUID elementId) {
    return elementRepository
        .findByElementId(elementId)
        .orElseThrow(() -> new BusinessValidationException("Element not found: " + elementId));
  }
}
