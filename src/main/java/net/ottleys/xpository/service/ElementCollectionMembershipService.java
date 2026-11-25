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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementCollectionMembership;
import net.ottleys.xpository.event.AfterElementDeleteEvent;
import net.ottleys.xpository.event.BeforeElementDeleteEvent;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.security.SecurityUtils;

/**
 * Service layer for ElementCollectionMembership business logic.
 *
 * <p>Manages the many-to-many relationship between elements and collections. Handles adding
 * elements to collections, removing them, and querying membership relationships. All public methods
 * are transactional to ensure data consistency.
 */
@Service
@Transactional
public class ElementCollectionMembershipService {

  private final ElementCollectionMembershipRepository membershipRepository;
  private final ElementRepository elementRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final ElementRelationshipService relationshipService;

  /**
   * Constructs an ElementCollectionMembershipService with required dependencies.
   *
   * @param membershipRepository repository for membership data access
   * @param elementRepository repository for element data access
   * @param eventPublisher publisher for element lifecycle events
   */
  public ElementCollectionMembershipService(
      ElementCollectionMembershipRepository membershipRepository,
      ElementRepository elementRepository,
      ApplicationEventPublisher eventPublisher,
      @Lazy ElementRelationshipService relationshipService) {
    this.membershipRepository = membershipRepository;
    this.elementRepository = elementRepository;
    this.eventPublisher = eventPublisher;
    this.relationshipService = relationshipService;
  }

  /**
   * Adds an element to a collection.
   *
   * <p>Creates a new membership record linking the element to the collection. If the element is
   * already in the collection, this method does nothing (idempotent). For LIST collections, assigns
   * the next available order position and calculates depth based on parent element.
   *
   * @param element the element to add
   * @param collection the collection to add it to
   * @param addedBy username of user performing the operation
   * @return the created membership, or existing membership if already present
   */
  public ElementCollectionMembership addElementToCollection(
      Element element, Collection collection, String addedBy) {
    return addElementToCollection(element, collection, addedBy, null);
  }

  /**
   * Adds an element to a collection with optional parent element.
   *
   * <p>Creates a new membership record linking the element to the collection. If the element is
   * already in the collection, this method does nothing (idempotent). For LIST collections, assigns
   * the next available order position and calculates depth based on parent element.
   *
   * @param element the element to add
   * @param collection the collection to add it to
   * @param addedBy username of user performing the operation
   * @param parentElement optional parent element for nested lists (null for root level)
   * @return the created membership, or existing membership if already present
   * @throws IllegalArgumentException if parent element is not in the same collection
   */
  public ElementCollectionMembership addElementToCollection(
      Element element, Collection collection, String addedBy, Element parentElement) {
    // Check if membership already exists
    if (membershipRepository.existsByElementAndCollection(element, collection)) {
      return membershipRepository
          .findByElementAndCollection(element, collection)
          .orElseThrow(() ->
              new IllegalStateException(
                  "Membership exists but could not be retrieved")); // Should never happen
    }

    // Validate parent element if provided
    if (parentElement != null) {
      if (!membershipRepository.existsByElementAndCollection(parentElement, collection)) {
        throw new IllegalArgumentException(
            "Parent element must be in the same collection as the child element");
      }
    }

    // Create new membership
    ElementCollectionMembership membership = new ElementCollectionMembership();
    membership.setElement(element);
    membership.setCollection(collection);
    membership.setAddedBy(addedBy);
    membership.setParentElement(parentElement);

    // Set collection-type-specific fields
    if (CollectionType.LIST.equals(collection.getType())) {
      // Assign next order position
      membership.setOrderPosition(getNextOrderPosition(collection));
      // Calculate depth based on parent
      membership.setDepth(calculateDepth(parentElement, collection));
    } else if (CollectionType.TREE.equals(collection.getType())) {
      // Set materialized path and depth for TREE
      membership.setMaterializedPath(calculateMaterializedPath(parentElement, collection));
      membership.setDepth(calculateDepth(parentElement, collection));
    } else if (CollectionType.STACK.equals(collection.getType())) {
      // STACK behaves like a LIST but only order position matters (LIFO)
      membership.setOrderPosition(getNextOrderPosition(collection));
      // Depth is not used for STACK; set to 0 for consistency
      membership.setDepth(0);
    } else if (CollectionType.GRAPH.equals(collection.getType())) {
      // GRAPH collections do not use ordering or hierarchy fields.
      // No extra fields are required.
    }

    return membershipRepository.save(membership);
  }

  /**
   * Gets the next available order position for a collection.
   *
   * <p>Finds the maximum order position currently in use and returns max + 1. If the collection is
   * empty, returns 0.
   *
   * @param collection the collection to get next order position for
   * @return the next available order position (0-based)
   */
  private int getNextOrderPosition(Collection collection) {
    Integer maxPosition = membershipRepository.findMaxOrderPositionByCollection(collection);
    return (maxPosition == null) ? 0 : maxPosition + 1;
  }

  /**
   * Calculates the depth of an element based on its parent.
   *
   * <p>Root-level elements (no parent) have depth 0. Child elements have depth = parent's depth +
   * 1. Works for both LIST and TREE collections.
   *
   * @param parentElement the parent element (null for root level)
   * @param collection the collection to find parent's depth in
   * @return the calculated depth (0 for root level, parent depth + 1 for nested)
   */
  private int calculateDepth(Element parentElement, Collection collection) {
    if (parentElement == null) {
      return 0; // Root level
    }

    // Find parent's membership to get its depth
    ElementCollectionMembership parentMembership =
        membershipRepository
            .findByElementAndCollection(parentElement, collection)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Parent element membership not found")); // Should not happen due to
    // validation

    Integer parentDepth = parentMembership.getDepth();
    return (parentDepth == null ? 0 : parentDepth) + 1;
  }

  /**
   * Calculates the materialized path for a TREE collection element.
   *
   * <p>Materialized paths represent the hierarchical position using a slash-separated string of
   * element IDs. For example: "/" for root, "/1" for a child of root, "/1/5" for a grandchild. This
   * enables efficient subtree queries using LIKE '/1/%'.
   *
   * @param parentElement the parent element (null for root level)
   * @param collection the TREE collection
   * @return the materialized path string (e.g., "/", "/1", "/1/5/12")
   */
  private String calculateMaterializedPath(Element parentElement, Collection collection) {
    if (parentElement == null) {
      return "/"; // Root level path
    }

    // Find parent's membership to get its path
    ElementCollectionMembership parentMembership =
        membershipRepository
            .findByElementAndCollection(parentElement, collection)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Parent element membership not found")); // Should not happen due to
    // validation

    String parentPath = parentMembership.getMaterializedPath();
    if (parentPath == null) {
      throw new IllegalStateException("Parent element has no materialized path");
    }

    // Append current element's ID to parent's path
    // Parent path ends with "/" if root, otherwise append "/" + parent's element ID
    if ("/".equals(parentPath)) {
      return "/" + parentElement.getId();
    } else {
      return parentPath + "/" + parentElement.getId();
    }
  }

  /**
   * Removes an element from a collection.
   *
   * <p>Deletes the membership record linking the element to the collection. If this was the
   * element's last active collection, the element itself is soft-deleted as well, since elements
   * cannot exist outside of collections. For LIST collections, closes the gap by shifting elements
   * after the removed element up by one position.
   *
   * @param element the element to remove
   * @param collection the collection to remove it from
   * @return true if membership was found and deleted, false if not found
   */
  public boolean removeElementFromCollection(Element element, Collection collection) {
    return removeElementFromCollection(element, collection, SecurityUtils.getCurrentUsername());
  }

  /**
   * Removes an element from a collection with explicit user attribution.
   *
   * @param element the element to remove
   * @param collection the collection to remove it from
   * @param removedBy username performing the removal
   * @return true if membership was found and deleted, false if not found
   */
  public boolean removeElementFromCollection(
      Element element, Collection collection, String removedBy) {
    if (!membershipRepository.existsByElementAndCollection(element, collection)) {
      return false;
    }

    // Get the membership to find its position
    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(); // Already checked existence

    Integer removedPosition = membership.getOrderPosition();

    // Check if this element has any remaining active collections
    long activeMemberships =
        membershipRepository.countActiveMembershipsByElementAndCollectionNot(element, collection);

    // For GRAPH collections, delete all relationships involving this element
    if (CollectionType.GRAPH.equals(collection.getType())) {
      relationshipService.deleteAllRelationshipsForElement(element, collection);
    }

    // Delete the membership
    membershipRepository.deleteByElementAndCollection(element, collection);

    // If this was the last active collection, soft-delete the element
    // Elements cannot exist outside of active collections
    if (activeMemberships == 0) {
      eventPublisher.publishEvent(new BeforeElementDeleteEvent(this, element, removedBy));
      element.softDelete(removedBy);
      elementRepository.save(element);
      eventPublisher.publishEvent(new AfterElementDeleteEvent(this, element, removedBy));
    }

    // For LIST collections, close the gap by shifting elements after the removed position
    if (CollectionType.LIST.equals(collection.getType()) && removedPosition != null) {
      List<ElementCollectionMembership> memberships =
          membershipRepository.findByCollection(collection);

      // Shift all elements after the removed position up by 1
      for (ElementCollectionMembership m : memberships) {
        Integer pos = m.getOrderPosition();
        if (pos != null && pos > removedPosition) {
          m.setOrderPosition(pos - 1);
          membershipRepository.save(m);
        }
      }
    }

    return true;
  }

  /**
   * Checks if an element is in a collection.
   *
   * @param element the element to check
   * @param collection the collection to check
   * @return true if the element is in the collection
   */
  @Transactional(readOnly = true)
  public boolean isElementInCollection(Element element, Collection collection) {
    return membershipRepository.existsByElementAndCollection(element, collection);
  }

  /**
   * Validates that an element's type is allowed in a collection.
   *
   * <p>If the collection has allowedElementTypes specified, the element's type must be in that
   * list. If allowedElementTypes is null or empty, any element type is allowed.
   *
   * @param element the element to validate
   * @param collection the collection to validate against
   * @throws IllegalArgumentException if element type is not allowed in the collection
   */
  public void validateElementTypeAllowed(Element element, Collection collection) {
    if (collection.getAllowedElementTypes() == null
        || collection.getAllowedElementTypes().isEmpty()) {
      // No restrictions - any element type is allowed
      return;
    }

    String elementTypeName = element.getType().getName();
    if (!collection.getAllowedElementTypes().contains(elementTypeName)) {
      throw new IllegalArgumentException(
          String.format(
              "Element type '%s' is not allowed in collection '%s'. Allowed types: %s",
              elementTypeName,
              collection.getName(),
              String.join(", ", collection.getAllowedElementTypes())));
    }
  }

  /**
   * Moves an element up in the order (decreases orderPosition by 1).
   *
   * <p>Swaps the orderPosition with the element immediately above it. Only works for LIST
   * collections. Does nothing if element is already at position 0.
   *
   * @param element the element to move up
   * @param collection the LIST collection
   * @return true if element was moved, false if already at top
   * @throws IllegalArgumentException if collection is not a LIST type
   * @throws IllegalStateException if element is not in the collection
   */
  public boolean moveElementUp(Element element, Collection collection) {
    validateListCollection(collection);

    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(
                () -> new IllegalStateException("Element is not in the specified collection"));

    Integer currentPosition = membership.getOrderPosition();
    if (currentPosition == null || currentPosition == 0) {
      return false; // Already at top
    }

    // Find element at position - 1
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    ElementCollectionMembership aboveMembership =
        memberships.stream()
            .filter(m -> Integer.valueOf(currentPosition - 1).equals(m.getOrderPosition()))
            .findFirst()
            .orElse(null);

    if (aboveMembership == null) {
      return false; // No element above
    }

    // Swap positions
    membership.setOrderPosition(currentPosition - 1);
    aboveMembership.setOrderPosition(currentPosition);

    membershipRepository.save(membership);
    membershipRepository.save(aboveMembership);

    return true;
  }

  /**
   * Moves an element down in the order (increases orderPosition by 1).
   *
   * <p>Swaps the orderPosition with the element immediately below it. Only works for LIST
   * collections. Does nothing if element is already at the last position.
   *
   * @param element the element to move down
   * @param collection the LIST collection
   * @return true if element was moved, false if already at bottom
   * @throws IllegalArgumentException if collection is not a LIST type
   * @throws IllegalStateException if element is not in the collection
   */
  public boolean moveElementDown(Element element, Collection collection) {
    validateListCollection(collection);

    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(
                () -> new IllegalStateException("Element is not in the specified collection"));

    Integer currentPosition = membership.getOrderPosition();
    if (currentPosition == null) {
      return false;
    }

    // Find element at position + 1
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    ElementCollectionMembership belowMembership =
        memberships.stream()
            .filter(m -> Integer.valueOf(currentPosition + 1).equals(m.getOrderPosition()))
            .findFirst()
            .orElse(null);

    if (belowMembership == null) {
      return false; // No element below / already at bottom
    }

    // Swap positions
    membership.setOrderPosition(currentPosition + 1);
    belowMembership.setOrderPosition(currentPosition);

    membershipRepository.save(membership);
    membershipRepository.save(belowMembership);

    return true;
  }

  /**
   * Moves an element to a specific position in the order.
   *
   * <p>Reorders elements to place the specified element at the target position. All elements
   * between the old and new positions are shifted accordingly. Only works for LIST collections.
   *
   * @param element the element to reorder
   * @param collection the LIST collection
   * @param newPosition the target position (0-based)
   * @throws IllegalArgumentException if collection is not a LIST type, or newPosition is invalid
   * @throws IllegalStateException if element is not in the collection
   */
  public void reorderElement(Element element, Collection collection, int newPosition) {
    validateListCollection(collection);

    if (newPosition < 0) {
      throw new IllegalArgumentException("Position must be non-negative");
    }

    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(
                () -> new IllegalStateException("Element is not in the specified collection"));

    Integer currentPosition = membership.getOrderPosition();
    if (currentPosition == null) {
      throw new IllegalStateException("Element has no order position");
    }

    if (currentPosition.equals(newPosition)) {
      return; // Already at target position
    }

    // Get all memberships sorted by position
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    // Validate newPosition is within bounds
    if (newPosition >= memberships.size()) {
      throw new IllegalArgumentException(
          "Position " + newPosition + " is out of bounds (max: " + (memberships.size() - 1) + ")");
    }

    // Reorder logic: shift elements between old and new positions
    if (currentPosition < newPosition) {
      // Moving down: shift elements between current and new position up
      for (ElementCollectionMembership m : memberships) {
        Integer pos = m.getOrderPosition();
        if (pos > currentPosition && pos <= newPosition) {
          m.setOrderPosition(pos - 1);
          membershipRepository.save(m);
        }
      }
    } else {
      // Moving up: shift elements between new and current position down
      for (ElementCollectionMembership m : memberships) {
        Integer pos = m.getOrderPosition();
        if (pos >= newPosition && pos < currentPosition) {
          m.setOrderPosition(pos + 1);
          membershipRepository.save(m);
        }
      }
    }

    // Move the element to new position
    membership.setOrderPosition(newPosition);
    membershipRepository.save(membership);
  }

  /**
   * Retrieves all elements in a subtree of a TREE collection.
   *
   * <p>Returns all descendants (children, grandchildren, etc.) of the specified element. Uses
   * materialized path for efficient querying. Only works for TREE collections.
   *
   * @param element the root element of the subtree
   * @param collection the TREE collection
   * @return list of all elements in the subtree (excluding the root element itself)
   * @throws IllegalArgumentException if collection is not a TREE type
   * @throws IllegalStateException if element is not in the collection
   */
  public List<Element> getSubtree(Element element, Collection collection) {
    validateTreeCollection(collection);

    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(
                () -> new IllegalStateException("Element is not in the specified collection"));

    String pathPrefix = membership.getMaterializedPath();
    if (pathPrefix == null) {
      throw new IllegalStateException("Element has no materialized path");
    }

    // Build search prefix: all paths containing this element's ID in the path
    // For root ("/"), search for all non-root paths (anything not "/")
    // For non-root (e.g., "/5"), search for paths starting with "/5/" or exactly "/5"
    String searchPrefix;
    if ("/".equals(pathPrefix)) {
      // For root, search for all paths containing the element's ID
      // Children: "/{id}", grandchildren: "/{id}/{childId}", etc.
      searchPrefix = "/" + element.getId();
    } else {
      // For non-root, search for paths starting with current path + "/"
      searchPrefix = pathPrefix + "/";
    }

    // Find all memberships with paths starting with the search prefix
    // This excludes the element itself and returns only descendants
    List<ElementCollectionMembership> subtreeMemberships =
        membershipRepository.findByCollectionAndMaterializedPathStartingWith(
            collection, searchPrefix);

    return subtreeMemberships.stream().map(ElementCollectionMembership::getElement).toList();
  }

  /**
   * Retrieves all ancestor elements (parent, grandparent, etc.) in a TREE collection.
   *
   * <p>Returns ancestors in order from immediate parent to root. Only works for TREE collections.
   *
   * @param element the element to find ancestors for
   * @param collection the TREE collection
   * @return list of ancestor elements (parent, grandparent, ..., root) in order
   * @throws IllegalArgumentException if collection is not a TREE type
   * @throws IllegalStateException if element is not in the collection
   */
  public List<Element> getAncestors(Element element, Collection collection) {
    validateTreeCollection(collection);

    ElementCollectionMembership membership =
        membershipRepository
            .findByElementAndCollection(element, collection)
            .orElseThrow(
                () -> new IllegalStateException("Element is not in the specified collection"));

    List<Element> ancestors = new java.util.ArrayList<>();
    ElementCollectionMembership currentMembership = membership;

    // Walk up the tree via parentElement references
    while (currentMembership.getParentElement() != null) {
      Element parentElement = currentMembership.getParentElement();

      // Fetch the parent membership to get the fully loaded element and continue walking
      currentMembership =
          membershipRepository.findByElementAndCollection(parentElement, collection).orElse(null);

      if (currentMembership != null) {
        // Add the fully loaded element from the membership
        ancestors.add(currentMembership.getElement());
      } else {
        break;
      }
    }

    return ancestors;
  }

  /**
   * Retrieves immediate children of an element in a TREE collection.
   *
   * <p>Returns only direct children (depth = parent.depth + 1). Only works for TREE collections.
   *
   * @param element the parent element
   * @param collection the TREE collection
   * @return list of immediate child elements
   * @throws IllegalArgumentException if collection is not a TREE type
   */
  public List<Element> getChildren(Element element, Collection collection) {
    validateTreeCollection(collection);

    List<ElementCollectionMembership> childMemberships =
        membershipRepository.findByCollectionAndParentElement(collection, element);

    return childMemberships.stream().map(ElementCollectionMembership::getElement).toList();
  }

  /**
   * Retrieves root elements (no parent) in a TREE collection.
   *
   * <p>Returns all elements at the top level of the tree hierarchy. Only works for TREE
   * collections.
   *
   * @param collection the TREE collection
   * @return list of root elements
   * @throws IllegalArgumentException if collection is not a TREE type
   */
  public List<Element> getRootElements(Collection collection) {
    validateTreeCollection(collection);

    List<ElementCollectionMembership> rootMemberships =
        membershipRepository.findRootElementsByCollection(collection);

    return rootMemberships.stream().map(ElementCollectionMembership::getElement).toList();
  }

  /**
   * Validates that a collection is of type LIST.
   *
   * @param collection the collection to validate
   * @throws IllegalArgumentException if collection is not a LIST type
   */
  private void validateListCollection(Collection collection) {
    if (!CollectionType.LIST.equals(collection.getType())) {
      throw new IllegalArgumentException(
          "Reordering is only supported for LIST collections. Collection type: "
              + collection.getType());
    }
  }

  /**
   * Validates that a collection is of type TREE.
   *
   * @param collection the collection to validate
   * @throws IllegalArgumentException if collection is not a TREE type
   */
  private void validateTreeCollection(Collection collection) {
    if (!CollectionType.TREE.equals(collection.getType())) {
      throw new IllegalArgumentException(
          "Tree operations are only supported for TREE collections. Collection type: "
              + collection.getType());
    }
  }

  /**
   * Validates that a collection is of type STACK.
   *
   * @param collection the collection to validate
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  private void validateStackCollection(Collection collection) {
    if (!CollectionType.STACK.equals(collection.getType())) {
      throw new IllegalArgumentException(
          "Stack operations are only supported for STACK collections. Collection type: "
              + collection.getType());
    }
  }

  /**
   * Pushes an element onto the top of a STACK collection.
   *
   * <p>LIFO (Last-In-First-Out) operation. The element is added to the top of the stack with
   * orderPosition = current max + 1. Stack operations only work on STACK collections.
   *
   * @param element the element to push
   * @param collection the STACK collection
   * @param addedBy username of user performing the operation
   * @return the created membership
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  public ElementCollectionMembership push(Element element, Collection collection, String addedBy) {
    validateStackCollection(collection);

    // Check if element is already in stack
    if (membershipRepository.existsByElementAndCollection(element, collection)) {
      throw new IllegalArgumentException("Element is already in the stack");
    }

    // Create membership with next order position (top of stack)
    ElementCollectionMembership membership = new ElementCollectionMembership();
    membership.setElement(element);
    membership.setCollection(collection);
    membership.setAddedBy(addedBy);
    membership.setOrderPosition(getNextOrderPosition(collection));
    membership.setDepth(0); // Stacks are flat

    return membershipRepository.save(membership);
  }

  /**
   * Pops the top element from a STACK collection.
   *
   * <p>LIFO (Last-In-First-Out) operation. Removes and returns the element with the highest
   * orderPosition. If the element has no other collection memberships, it is deleted entirely.
   *
   * @param collection the STACK collection
   * @return the popped element, or null if stack is empty
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  public Element pop(Collection collection) {
    validateStackCollection(collection);

    // Find the element with max orderPosition (top of stack)
    Integer maxPosition = membershipRepository.findMaxOrderPositionByCollection(collection);
    if (maxPosition == null) {
      return null; // Stack is empty
    }

    // Find all memberships and get the one with max position
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    ElementCollectionMembership topMembership =
        memberships.stream()
            .filter(m -> maxPosition.equals(m.getOrderPosition()))
            .findFirst()
            .orElse(null);

    if (topMembership == null) {
      return null; // Should not happen
    }

    Element poppedElement = topMembership.getElement();

    // Remove element from stack
    removeElementFromCollection(poppedElement, collection);

    return poppedElement;
  }

  /**
   * Peeks at the top element of a STACK collection without removing it.
   *
   * <p>Returns the element with the highest orderPosition without modifying the stack.
   *
   * @param collection the STACK collection
   * @return the top element, or null if stack is empty
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  @Transactional(readOnly = true)
  public Element peek(Collection collection) {
    validateStackCollection(collection);

    // Find the element with max orderPosition (top of stack)
    Integer maxPosition = membershipRepository.findMaxOrderPositionByCollection(collection);
    if (maxPosition == null) {
      return null; // Stack is empty
    }

    // Find all memberships and get the one with max position
    List<ElementCollectionMembership> memberships =
        membershipRepository.findByCollection(collection);
    Element topElement =
        memberships.stream()
            .filter(m -> maxPosition.equals(m.getOrderPosition()))
            .findFirst()
            .map(ElementCollectionMembership::getElement)
            .orElse(null);

    // Force initialization of type to avoid LazyInitializationException
    if (topElement != null) {
      topElement.getType().getName(); // Touch lazy field
    }

    return topElement;
  }

  /**
   * Gets the size of a STACK collection.
   *
   * <p>Returns the number of elements currently in the stack.
   *
   * @param collection the STACK collection
   * @return the number of elements in the stack
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  public int getStackSize(Collection collection) {
    validateStackCollection(collection);
    return membershipRepository.findByCollection(collection).size();
  }

  /**
   * Checks if a STACK collection is empty.
   *
   * @param collection the STACK collection
   * @return true if the stack is empty
   * @throws IllegalArgumentException if collection is not a STACK type
   */
  public boolean isStackEmpty(Collection collection) {
    validateStackCollection(collection);
    return getStackSize(collection) == 0;
  }
}
