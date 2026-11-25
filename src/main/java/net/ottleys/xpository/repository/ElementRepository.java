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

import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Element entity persistence and query operations.
 *
 * <p>Provides CRUD operations and custom queries for elements including UUID-based lookups, type
 * filtering, and pagination support. Supports dynamic queries via JpaSpecificationExecutor for
 * advanced filtering and search capabilities.
 */
public interface ElementRepository
    extends JpaRepository<Element, Long>, JpaSpecificationExecutor<Element> {

  /**
   * Finds an element by its public UUID (non-deleted only).
   *
   * @param elementId the public UUID to search for
   * @return Optional containing the element if found and not deleted
   */
  @Query("SELECT e FROM Element e WHERE e.elementId = :elementId AND e.deleted = false")
  Optional<Element> findByElementId(@Param("elementId") UUID elementId);

  /**
   * Finds an element by its public UUID with element type eagerly loaded (non-deleted only).
   *
   * <p>Uses JOIN FETCH to eagerly load the element type in a single query. This prevents
   * LazyInitializationException when accessed outside a transaction (e.g., in REST controllers).
   *
   * @param elementId the public UUID to search for
   * @return Optional containing the element with type loaded if found and not deleted
   */
  @Query(
      "SELECT e FROM Element e LEFT JOIN FETCH e.type WHERE e.elementId = :elementId AND e.deleted"
          + " = false")
  Optional<Element> findByElementIdWithType(@Param("elementId") UUID elementId);

  /**
   * Finds an element by its internal database ID with element type eagerly loaded.
   *
   * <p>Uses {@link EntityGraph} to eagerly load the element type in a single query. This prevents
   * {@code LazyInitializationException} when accessed outside a transaction (e.g., in REST
   * controllers or event listeners). Unlike {@link #findByElementIdWithType(UUID)}, this lookup
   * does not filter out soft-deleted elements.
   *
   * @param id the internal primary key ID to search for
   * @return Optional containing the element with type loaded if found
   */
  @Override
  @EntityGraph(attributePaths = {"type"})
  Optional<Element> findById(Long id);

  /**
   * Finds all non-deleted elements with pagination and element types eagerly loaded.
   *
   * <p>Uses JOIN FETCH to eagerly load element types in a single query. Prevents
   * LazyInitializationException when elements are accessed in REST responses.
   *
   * <p>Note: Hibernate requires a count query for pagination with JOIN FETCH to avoid loading all
   * results just to count them.
   *
   * @param pageable pagination parameters
   * @return page of non-deleted elements with types loaded
   */
  @Query(
      value = "SELECT e FROM Element e LEFT JOIN FETCH e.type WHERE e.deleted = false",
      countQuery = "SELECT COUNT(e) FROM Element e WHERE e.deleted = false")
  Page<Element> findAllWithTypes(Pageable pageable);

  /**
   * Finds non‑deleted elements belonging to a specific collection, paginated.
   *
   * @param collectionId the public UUID of the collection
   * @param pageable pagination parameters
   * @return a page of elements belonging to the collection
   */
  @Query(
      value =
          "SELECT DISTINCT e FROM Element e JOIN e.memberships m WHERE m.collection.collectionId = :collectionId AND e.deleted = false",
      countQuery =
          "SELECT COUNT(DISTINCT e) FROM Element e JOIN e.memberships m WHERE m.collection.collectionId = :collectionId AND e.deleted = false")
  Page<Element> findByCollectionId(@Param("collectionId") UUID collectionId, Pageable pageable);

  /**
   * Checks if a non-deleted element exists by its public UUID.
   *
   * @param elementId the public UUID to check
   * @return true if a non-deleted element with this UUID exists
   */
  @Query("SELECT COUNT(e) > 0 FROM Element e WHERE e.elementId = :elementId AND e.deleted = false")
  boolean existsByElementId(@Param("elementId") UUID elementId);

  /**
   * Finds all non-deleted elements that have no collection memberships.
   *
   * @return list of orphaned non-deleted elements
   */
  @Query("SELECT e FROM Element e WHERE e.deleted = false AND e.memberships IS EMPTY")
  java.util.List<Element> findOrphanedNotDeleted();

  /**
   * Finds an element by its public UUID, including soft-deleted elements.
   *
   * @param elementId the public UUID to search for
   * @return Optional containing the element if found
   */
  @Query("SELECT e FROM Element e WHERE e.elementId = :elementId")
  Optional<Element> findByElementIdIncludingDeleted(@Param("elementId") UUID elementId);
}
