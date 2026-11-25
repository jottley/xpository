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
import net.ottleys.xpository.domain.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for Collection entity persistence and query operations.
 *
 * <p>Provides CRUD operations and custom queries for collections including soft delete support,
 * hierarchical queries (parent-child relationships), name uniqueness checks, and system collection
 * management. All "Active" queries exclude soft-deleted collections.
 */
public interface CollectionRepository
    extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {

  /**
   * Finds all non-deleted collections with allowed element types eagerly loaded for pagination.
   *
   * <p>Uses JOIN FETCH to eagerly load allowedElementTypes in a single query. Prevents
   * LazyInitializationException when collections are accessed in REST responses. Only returns
   * active (non-deleted) collections.
   *
   * <p>Note: Hibernate requires a count query for pagination with JOIN FETCH to avoid loading all
   * results just to count them.
   *
   * @param pageable pagination parameters
   * @return page of non-deleted collections with types loaded
   */
  @Query(
      value =
          "SELECT DISTINCT c FROM Collection c LEFT JOIN FETCH c.allowedElementTypes LEFT JOIN FETCH c.parentCollection WHERE c.deleted = false",
      countQuery = "SELECT COUNT(c) FROM Collection c WHERE c.deleted = false")
  Page<Collection> findAllWithTypes(Pageable pageable);

  /**
   * Finds a non-deleted collection by its public UUID.
   *
   * <p>Note: This method uses lazy loading for allowedElementTypes. For REST API responses, use
   * {@link #findByCollectionIdWithTypes(UUID)} instead to avoid LazyInitializationException.
   *
   * <p>Only returns collections where deleted=false. For accessing soft-deleted collections, use
   * repository methods that explicitly include deleted records.
   *
   * @param collectionId the public UUID to search for
   * @return Optional containing the non-deleted collection if found
   */
  @Query("SELECT c FROM Collection c WHERE c.collectionId = :collectionId AND c.deleted = false")
  Optional<Collection> findByCollectionId(@Param("collectionId") UUID collectionId);

  /**
   * Finds a collection by its public UUID with allowed element types eagerly loaded.
   *
   * <p>Uses JOIN FETCH to eagerly load the allowedElementTypes collection in a single query. This
   * prevents LazyInitializationException when accessed outside a transaction (e.g., in REST
   * controllers). Only returns non-deleted collections.
   *
   * @param collectionId the public UUID to search for
   * @return Optional containing the non-deleted collection with types loaded if found
   */
  @Query(
      "SELECT c FROM Collection c LEFT JOIN FETCH c.allowedElementTypes WHERE c.collectionId = :collectionId AND c.deleted = false")
  Optional<Collection> findByCollectionIdWithTypes(@Param("collectionId") UUID collectionId);

  /**
   * Finds a top-level collection by name (where parent is null).
   *
   * @param name the collection name to search for
   * @return Optional containing the collection if found
   */
  Optional<Collection> findByNameAndParentCollectionIsNull(String name);

  /**
   * Finds a collection by name within a specific parent collection.
   *
   * @param name the collection name to search for
   * @param parentCollection the parent collection
   * @return Optional containing the collection if found
   */
  Optional<Collection> findByNameAndParentCollection(String name, Collection parentCollection);

  /**
   * Checks if a top-level collection with the given name exists.
   *
   * @param name the collection name to check
   * @return true if a top-level collection with this name exists
   */
  boolean existsByNameAndParentCollectionIsNull(String name);

  /**
   * Checks if a collection with the given name exists within a specific parent.
   *
   * @param name the collection name to check
   * @param parentCollection the parent collection
   * @return true if a collection with this name exists within the parent
   */
  boolean existsByNameAndParentCollection(String name, Collection parentCollection);

  /**
   * Finds all active (non-deleted) collections.
   *
   * @return list of all active collections
   */
  @Query("SELECT c FROM Collection c WHERE c.deleted = false")
  List<Collection> findAllActive();

  /**
   * Finds an active collection by its public UUID.
   *
   * @param collectionId the public UUID to search for
   * @return Optional containing the active collection if found
   */
  @Query("SELECT c FROM Collection c WHERE c.collectionId = :collectionId AND c.deleted = false")
  Optional<Collection> findActiveByCollectionId(@Param("collectionId") UUID collectionId);

  /**
   * Finds all top-level active collections ordered by name.
   *
   * @return list of top-level active collections
   */
  @Query(
      "SELECT c FROM Collection c WHERE c.parentCollection IS NULL AND c.deleted = false ORDER BY c.name")
  List<Collection> findAllTopLevelActive();

  /**
   * Finds all active child collections of a parent, ordered by name.
   *
   * <p>Eagerly loads allowedElementTypes to prevent N+1 queries when accessing the collection
   * outside of a transaction context.
   *
   * @param parent the parent collection
   * @return list of active child collections with types loaded
   */
  @Query(
      "SELECT c FROM Collection c LEFT JOIN FETCH c.allowedElementTypes WHERE c.parentCollection = :parent AND c.deleted = false ORDER BY c.name")
  List<Collection> findByParentCollectionActive(@Param("parent") Collection parent);

  /**
   * Finds the System Root collection.
   *
   * @return Optional containing the System Root if it exists and is active
   */
  @Query("SELECT c FROM Collection c WHERE c.systemCollection = true AND c.deleted = false")
  Optional<Collection> findSystemRootCollection();

  /**
   * Finds soft-deleted collections older than the cutoff date for cleanup.
   *
   * @param cutoffDate the date threshold for cleanup
   * @return list of collections deleted before the cutoff date
   */
  @Query("SELECT c FROM Collection c WHERE c.deleted = true AND c.deletedDate < :cutoffDate")
  List<Collection> findDeletedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * Finds all soft-deleted collections ordered by deletion date.
   *
   * @return list of all deleted collections
   */
  @Query("SELECT c FROM Collection c WHERE c.deleted = true ORDER BY c.deletedDate DESC")
  List<Collection> findAllDeleted();

  /**
   * Finds all active child collections of a parent by parent UUID with pagination.
   *
   * @param parentCollectionId the public UUID of the parent collection
   * @param pageable pagination parameters
   * @return page of active child collections
   */
  @Query(
      "SELECT c FROM Collection c WHERE c.parentCollection.collectionId = :parentCollectionId AND c.deleted = false")
  Page<Collection> findByParentCollectionCollectionId(
      @Param("parentCollectionId") UUID parentCollectionId, Pageable pageable);

  /**
   * Finds all system collections.
   *
   * @return list of active system collections
   */
  @Query("SELECT c FROM Collection c WHERE c.systemCollection = true AND c.deleted = false")
  List<Collection> findBySystemCollectionTrue();

  /** Deletes all non-system collections. */
  @Transactional
  @Modifying
  @Query("DELETE FROM Collection c WHERE c.systemCollection = false")
  void deleteBySystemCollectionFalse();

  /**
   * Finds a collection by its public UUID, including soft-deleted collections.
   *
   * @param collectionId the public UUID to search for
   * @return Optional containing the collection if found
   */
  @Query("SELECT c FROM Collection c WHERE c.collectionId = :collectionId")
  Optional<Collection> findByCollectionIdIncludingDeleted(@Param("collectionId") UUID collectionId);

  /**
   * Finds all soft-deleted child collections of a parent collection.
   *
   * @param parent the parent collection
   * @return list of deleted child collections
   */
  @Query("SELECT c FROM Collection c WHERE c.parentCollection = :parent AND c.deleted = true")
  List<Collection> findByParentCollectionDeleted(@Param("parent") Collection parent);

  /**
   * Finds all active child collections of parent collections that inherit permissions.
   *
   * @param parentCollectionIds list of parent collection public UUIDs
   * @return list of active child collections inheriting permissions
   */
  @Query(
      "SELECT c FROM Collection c WHERE c.parentCollection.collectionId IN :parentCollectionIds AND c.deleted = false AND c.inheritPermissions = true")
  List<Collection> findActiveChildrenInheritingPermissions(
      @Param("parentCollectionIds") List<UUID> parentCollectionIds);
}
