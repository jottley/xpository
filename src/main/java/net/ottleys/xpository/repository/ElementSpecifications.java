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
import net.ottleys.xpository.domain.Element;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for building dynamic Element queries.
 *
 * <p>Provides reusable query predicates that can be combined using {@code Specification.where()}
 * and logical operators (and, or) to build complex filtered queries.
 *
 * <p>Example usage:
 *
 * <pre>
 * Specification&lt;Element&gt; spec = Specification.where(ElementSpecifications.hasElementType("xpository:core:standard"))
 *     .and(ElementSpecifications.createdByUser("admin"));
 * Page&lt;Element&gt; results = repository.findAll(spec, pageable);
 * </pre>
 */
public class ElementSpecifications {

  private ElementSpecifications() {
    // Utility class - prevent instantiation
  }

  /**
   * Filters to only include non-deleted elements.
   *
   * @return specification filtering out deleted elements
   */
  public static Specification<Element> isNotDeleted() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), false);
  }

  /**
   * Filters elements by element type name.
   *
   * @param elementTypeName the element type name (namespace format)
   * @return specification filtering by element type
   */
  public static Specification<Element> hasElementType(String elementTypeName) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.join("type", JoinType.LEFT).get("name"), elementTypeName);
  }

  /**
   * Filters elements created by a specific user.
   *
   * @param username the username who created the element
   * @return specification filtering by creator
   */
  public static Specification<Element> createdByUser(String username) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createdBy"), username);
  }

  /**
   * Filters elements modified by a specific user.
   *
   * @param username the username who modified the element
   * @return specification filtering by modifier
   */
  public static Specification<Element> modifiedByUser(String username) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("modifiedBy"), username);
  }

  /**
   * Eagerly fetches element type to prevent LazyInitializationException.
   *
   * <p>Use this specification when the query results will be accessed outside a transaction (e.g.,
   * in REST controller responses) and you need to access the element type.
   *
   * @return specification that eagerly loads the element type
   */
  public static Specification<Element> fetchType() {
    return (root, query, criteriaBuilder) -> {
      if (query != null) {
        root.fetch("type", JoinType.LEFT);
        query.distinct(true); // Prevent duplicates from join
      }
      return criteriaBuilder.conjunction(); // Always true - just adds the fetch
    };
  }
}
