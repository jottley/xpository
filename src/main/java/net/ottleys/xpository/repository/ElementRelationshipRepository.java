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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementRelationship;

/**
 * Repository for ElementRelationship operations in GRAPH collections.
 *
 * <p>Manages directed relationships between elements, supporting queries for outgoing relationships
 * (from source), incoming relationships (to target), and bidirectional queries.
 */
public interface ElementRelationshipRepository extends JpaRepository<ElementRelationship, Long> {

  /**
   * Finds all outgoing relationships from a source element in a collection.
   *
   * @param sourceElement the source element
   * @param collection the GRAPH collection
   * @return list of relationships with target elements eagerly loaded
   */
  @Query(
      "SELECT r FROM ElementRelationship r JOIN FETCH r.collection JOIN FETCH r.sourceElement JOIN FETCH r.targetElement t JOIN FETCH t.type WHERE r.sourceElement = :source AND r.collection = :collection")
  List<ElementRelationship> findBySourceElementAndCollection(
      @Param("source") Element sourceElement, @Param("collection") Collection collection);

  /**
   * Finds all incoming relationships to a target element in a collection.
   *
   * @param targetElement the target element
   * @param collection the GRAPH collection
   * @return list of relationships with source elements eagerly loaded
   */
  @Query(
      "SELECT r FROM ElementRelationship r JOIN FETCH r.collection JOIN FETCH r.sourceElement s JOIN FETCH s.type JOIN FETCH r.targetElement WHERE r.targetElement = :target AND r.collection = :collection")
  List<ElementRelationship> findByTargetElementAndCollection(
      @Param("target") Element targetElement, @Param("collection") Collection collection);

  /**
   * Finds a specific relationship between two elements in a collection.
   *
   * @param sourceElement the source element
   * @param targetElement the target element
   * @param relationshipType the relationship type
   * @param collection the GRAPH collection
   * @return Optional containing the relationship if it exists
   */
  @Query(
      "SELECT r FROM ElementRelationship r WHERE r.sourceElement = :source AND r.targetElement = :target AND r.relationshipType = :type AND r.collection = :collection")
  Optional<ElementRelationship> findBySourceAndTargetAndTypeAndCollection(
      @Param("source") Element sourceElement,
      @Param("target") Element targetElement,
      @Param("type") String relationshipType,
      @Param("collection") Collection collection);

  /**
   * Finds all relationships of a specific type in a collection.
   *
   * @param relationshipType the relationship type
   * @param collection the GRAPH collection
   * @return list of relationships with source and target elements eagerly loaded
   */
  @Query(
      "SELECT r FROM ElementRelationship r JOIN FETCH r.sourceElement s JOIN FETCH s.type JOIN FETCH r.targetElement t JOIN FETCH t.type WHERE r.relationshipType = :type AND r.collection = :collection")
  List<ElementRelationship> findByRelationshipTypeAndCollection(
      @Param("type") String relationshipType, @Param("collection") Collection collection);

  /**
   * Checks if a relationship exists between two elements.
   *
   * @param sourceElement the source element
   * @param targetElement the target element
   * @param relationshipType the relationship type
   * @param collection the GRAPH collection
   * @return true if the relationship exists
   */
  boolean existsBySourceElementAndTargetElementAndRelationshipTypeAndCollection(
      Element sourceElement, Element targetElement, String relationshipType, Collection collection);

  /**
   * Deletes all relationships involving an element (as source or target).
   *
   * @param element the element
   * @param collection the GRAPH collection
   */
  @Modifying
  @Query(
      "DELETE FROM ElementRelationship r WHERE r.collection = :collection AND (r.sourceElement = :element OR r.targetElement = :element)")
  void deleteByElementAndCollection(
      @Param("element") Element element, @Param("collection") Collection collection);

  /**
   * Deletes a specific relationship.
   *
   * @param sourceElement the source element
   * @param targetElement the target element
   * @param relationshipType the relationship type
   * @param collection the GRAPH collection
   */
  void deleteBySourceElementAndTargetElementAndRelationshipTypeAndCollection(
      Element sourceElement, Element targetElement, String relationshipType, Collection collection);

  /**
   * Deletes all relationships in a collection.
   *
   * @param collection the GRAPH collection
   */
  void deleteByCollection(Collection collection);
}
