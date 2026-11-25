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
import java.util.UUID;
import net.ottleys.xpository.domain.Trait;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for Trait operations.
 *
 * <p>Provides CRUD operations and custom queries for trait definitions, including lookups by UUID,
 * name, and trait type.
 */
public interface TraitRepository extends JpaRepository<Trait, Long> {

  /**
   * Finds a trait by its public UUID.
   *
   * @param traitId the trait UUID
   * @return Optional containing the trait if found
   */
  Optional<Trait> findByTraitId(UUID traitId);

  /**
   * Finds a trait by name.
   *
   * @param name the trait name
   * @return Optional containing the trait if found
   */
  Optional<Trait> findByName(String name);

  /**
   * Checks if a trait exists with the given name.
   *
   * @param name the trait name
   * @return true if trait exists
   */
  boolean existsByName(String name);

  /**
   * Finds all traits by type.
   *
   * @param traitType the trait type
   * @return list of traits of the given type
   */
  List<Trait> findByTraitType(Trait.TraitType traitType);

  /**
   * Finds all required traits.
   *
   * @return list of required traits
   */
  List<Trait> findByRequiredTrue();

  /**
   * Finds all inheritable traits.
   *
   * @return list of inheritable traits
   */
  List<Trait> findByInheritableTrue();

  /**
   * Finds traits by name containing the search term (case-insensitive).
   *
   * @param name the search term
   * @return list of matching traits
   */
  List<Trait> findByNameContainingIgnoreCase(String name);

  /**
   * Counts the total number of traits.
   *
   * @return count of traits
   */
  long count();
}
