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

import jakarta.persistence.criteria.JoinType;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for building dynamic Collection queries.
 *
 * <p>Provides reusable query predicates that can be combined using {@code Specification.where()}
 * and logical operators (and, or) to build complex filtered queries. All specifications
 * automatically filter out soft-deleted records (deleted=false).
 *
 * <p>Example usage:
 *
 * <pre>
 * Specification&lt;Collection&gt; spec = Specification.where(CollectionSpecifications.isNotDeleted())
 *     .and(CollectionSpecifications.hasType(CollectionType.TREE))
 *     .and(CollectionSpecifications.nameContains("project"));
 * Page&lt;Collection&gt; results = repository.findAll(spec, pageable);
 * </pre>
 */
public class CollectionSpecifications {

  private CollectionSpecifications() {
    // Utility class - prevent instantiation
  }

  /**
   * Filters collections that are not soft-deleted.
   *
   * <p>Returns collections where deleted=false. This should be included in all queries to ensure
   * soft-deleted collections are hidden from normal operations.
   *
   * @return specification filtering non-deleted collections
   */
  public static Specification<Collection> isNotDeleted() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
  }

  /**
   * Filters collections by parent collection UUID.
   *
   * <p>Returns collections where the parent collection has the specified UUID. Use null to find
   * top-level collections (where parent is null).
   *
   * @param parentCollectionId the parent collection UUID, or null for top-level collections
   * @return specification filtering by parent collection
   */
  public static Specification<Collection> hasParent(UUID parentCollectionId) {
    return (root, query, criteriaBuilder) -> {
      if (parentCollectionId == null) {
        // Find top-level collections (no parent)
        return criteriaBuilder.isNull(root.get("parentCollection"));
      } else {
        // Find children of specific parent by UUID
        return criteriaBuilder.equal(
            root.join("parentCollection", JoinType.LEFT).get("collectionId"), parentCollectionId);
      }
    };
  }

  /**
   * Filters collections by type.
   *
   * @param type the collection type (LIST, TREE, GRAPH, STACK)
   * @return specification filtering by collection type
   */
  public static Specification<Collection> hasType(CollectionType type) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
  }

  /**
   * Searches collections by name using case-insensitive partial matching.
   *
   * <p>Returns collections where the name contains the search query (case-insensitive). For
   * example, searching for "proj" will match "Projects", "project-alpha", and "My Project".
   *
   * @param nameQuery the search query to match against collection names
   * @return specification for case-insensitive name search
   */
  public static Specification<Collection> nameContains(String nameQuery) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.like(
            criteriaBuilder.lower(root.get("name")), "%" + nameQuery.toLowerCase() + "%");
  }

  /**
   * Filters collections by system collection flag.
   *
   * <p>Returns collections based on whether they are system-managed. System collections are created
   * and managed by the application (e.g., "System Root").
   *
   * @param isSystemCollection true to find only system collections, false for non-system
   * @return specification filtering by system collection flag
   */
  public static Specification<Collection> isSystemCollection(Boolean isSystemCollection) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("systemCollection"), isSystemCollection);
  }

  /**
   * Eagerly fetches allowedElementTypes to prevent LazyInitializationException.
   *
   * <p>Use this specification when the query results will be accessed outside a transaction (e.g.,
   * in REST controller responses) and you need to access the allowedElementTypes collection.
   *
   * @return specification that eagerly loads allowedElementTypes
   */
  public static Specification<Collection> fetchElementTypes() {
    return (root, query, criteriaBuilder) -> {
      if (query != null
          && query.getResultType() != Long.class
          && query.getResultType() != long.class) {
        root.fetch("allowedElementTypes", JoinType.LEFT);
        root.fetch("parentCollection", JoinType.LEFT);
        query.distinct(true); // Prevent duplicates from join
      }
      return criteriaBuilder.conjunction(); // Always true - just adds the fetch
    };
  }
}
