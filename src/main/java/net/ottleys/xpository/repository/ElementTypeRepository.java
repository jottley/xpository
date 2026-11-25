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
import net.ottleys.xpository.domain.ElementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for ElementType entity persistence and query operations.
 *
 * <p>Provides CRUD operations and queries for element type definitions including name-based lookups
 * and existence checks to support type validation and inheritance relationship management.
 */
public interface ElementTypeRepository extends JpaRepository<ElementType, Long> {

  /**
   * Finds an element type by its unique name.
   *
   * @param name the element type name to search for
   * @return Optional containing the element type if found
   */
  Optional<ElementType> findByName(String name);

  /**
   * Checks if an element type with the given name exists.
   *
   * @param name the element type name to check
   * @return true if an element type with this name exists
   */
  boolean existsByName(String name);

  @Transactional
  @Modifying
  @Query("DELETE FROM ElementType e WHERE e.name NOT LIKE 'xpository:%'")
  void deleteElementTypes(); // or deleteByNameNotStartingWith("xpository:")
}
