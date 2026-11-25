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

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementCollectionMembership;

/**
 * Repository for ElementCollectionMembership junction table operations.
 *
 * <p>Manages the many-to-many relationships between elements and collections, providing queries for
 * membership lookups, hierarchical structure navigation (parent-child relationships, tree paths),
 * and bulk operations. Supports LIST, TREE, and future GRAPH collection types.
 */
public interface ElementCollectionMembershipRepository
    extends JpaRepository<ElementCollectionMembership, Long> {

  /**
   * Finds all collection memberships for an element.
   *
   * @param element the element to search for
   * @return list of memberships for the element
   */
  List<ElementCollectionMembership> findByElement(Element element);

  /**
   * Finds all element memberships in a collection.
   *
   * <p>Eagerly fetches the element and parent element to prevent LazyInitializationException.
   *
   * @param collection the collection to search for
   * @return list of memberships in the collection with elements eagerly loaded
   */
  @Query(
      "SELECT m FROM ElementCollectionMembership m LEFT JOIN FETCH m.element LEFT JOIN FETCH m.parentElement WHERE m.collection = :collection")
  List<ElementCollectionMembership> findByCollection(@Param("collection") Collection collection);

  /**
   * Finds a specific membership between an element and collection.
   *
   * @param element the element
   * @param collection the collection
   * @return Optional containing the membership if it exists with element and type eagerly loaded
   */
  @Query(
      "SELECT m FROM ElementCollectionMembership m JOIN FETCH m.element e JOIN FETCH e.type WHERE m.element = :element AND m.collection = :collection")
  Optional<ElementCollectionMembership> findByElementAndCollection(
      @Param("element") Element element, @Param("collection") Collection collection);

  /**
   * Checks if an element is a member of a collection.
   *
   * @param element the element to check
   * @param collection the collection to check
   * @return true if the element is in the collection
   */
  boolean existsByElementAndCollection(Element element, Collection collection);

  /**
   * Counts how many collections contain an element.
   *
   * @param element the element to count memberships for
   * @return number of collections containing the element
   */
  long countByElement(Element element);

  /**
   * Counts collections containing an element, excluding a specific collection.
   *
   * @param element the element to count memberships for
   * @param excludeCollection the collection to exclude from the count
   * @return number of collections containing the element, excluding the specified one
   */
  @Query(
      "SELECT COUNT(m) FROM ElementCollectionMembership m WHERE m.element = :element AND m.collection != :excludeCollection")
  long countByElementAndCollectionNot(
      @Param("element") Element element, @Param("excludeCollection") Collection excludeCollection);

  /**
   * Finds all elements in a collection.
   *
   * @param collection the collection to query
   * @param pageable pagination parameters
   * @return list of elements in the collection with element types eagerly loaded
   */
  @Query(
      value =
          "SELECT m.element FROM ElementCollectionMembership m JOIN FETCH m.element.type WHERE m.collection = :collection",
      countQuery =
          "SELECT COUNT(m) FROM ElementCollectionMembership m WHERE m.collection = :collection")
  Page<Element> findElementsByCollection(
      @Param("collection") Collection collection, Pageable pageable);

  /**
   * Finds all collections containing an element.
   *
   * @param element the element to query
   * @return list of collections containing the element with allowed types eagerly loaded
   */
  @Query(
      "SELECT DISTINCT c FROM ElementCollectionMembership m JOIN m.collection c LEFT JOIN FETCH c.allowedElementTypes WHERE m.element = :element")
  List<Collection> findCollectionsByElement(@Param("element") Element element);

  /**
   * Finds child elements of a parent within a collection, ordered by position.
   *
   * @param collection the collection to query
   * @param parent the parent element
   * @return list of child memberships ordered by position with elements eagerly loaded
   */
  @Query(
      "SELECT m FROM ElementCollectionMembership m JOIN FETCH m.element e JOIN FETCH e.type WHERE m.collection = :collection AND m.parentElement = :parent ORDER BY m.orderPosition")
  List<ElementCollectionMembership> findByCollectionAndParentElement(
      @Param("collection") Collection collection, @Param("parent") Element parent);

  /**
   * Finds root elements (with no parent) in a collection, ordered by position.
   *
   * @param collection the collection to query
   * @return list of root-level memberships ordered by position with elements eagerly loaded
   */
  @Query(
      "SELECT m FROM ElementCollectionMembership m JOIN FETCH m.element e JOIN FETCH e.type WHERE m.collection = :collection AND m.parentElement IS NULL ORDER BY m.orderPosition")
  List<ElementCollectionMembership> findRootElementsByCollection(
      @Param("collection") Collection collection);

  /**
   * Finds memberships with a materialized path starting with the given prefix (for subtree
   * queries).
   *
   * @param collection the collection to query
   * @param pathPrefix the materialized path prefix (e.g., "/1/5/")
   * @return list of memberships in the subtree with elements eagerly loaded
   */
  @Query(
      "SELECT m FROM ElementCollectionMembership m JOIN FETCH m.element e JOIN FETCH e.type WHERE m.collection = :collection AND m.materializedPath LIKE CONCAT(:pathPrefix, '%')")
  List<ElementCollectionMembership> findByCollectionAndMaterializedPathStartingWith(
      @Param("collection") Collection collection, @Param("pathPrefix") String pathPrefix);

  /**
   * Finds the maximum order position in a collection.
   *
   * <p>Used for LIST collections to determine the next available position when adding elements.
   * Returns null if the collection is empty.
   *
   * @param collection the collection to query
   * @return the maximum order position, or null if collection is empty
   */
  @Query(
      "SELECT MAX(m.orderPosition) FROM ElementCollectionMembership m WHERE m.collection = :collection")
  Integer findMaxOrderPositionByCollection(@Param("collection") Collection collection);

  /**
   * Deletes a specific membership between an element and collection.
   *
   * @param element the element
   * @param collection the collection
   */
  void deleteByElementAndCollection(Element element, Collection collection);

  /**
   * Deletes all memberships for a collection (typically during collection deletion).
   *
   * @param collection the collection whose memberships should be deleted
   */
  void deleteByCollection(Collection collection);

  /**
   * Counts active (non-deleted) collections containing an element.
   *
   * @param element the element to count active memberships for
   * @return number of active collections containing the element
   */
  @Query(
      "SELECT COUNT(m) FROM ElementCollectionMembership m WHERE m.element = :element AND m.collection.deleted = false")
  long countActiveMembershipsByElement(@Param("element") Element element);

  /**
   * Counts active (non-deleted) collections containing an element, excluding a specific collection.
   *
   * @param element the element to count memberships for
   * @param excludeCollection the collection to exclude from the count
   * @return number of active collections containing the element, excluding the specified one
   */
  @Query(
      "SELECT COUNT(m) FROM ElementCollectionMembership m WHERE m.element = :element AND m.collection.deleted = false AND m.collection != :excludeCollection")
  long countActiveMembershipsByElementAndCollectionNot(
      @Param("element") Element element, @Param("excludeCollection") Collection excludeCollection);
}
