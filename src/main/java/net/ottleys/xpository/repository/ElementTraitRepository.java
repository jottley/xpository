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
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.domain.Trait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for ElementTrait operations.
 *
 * <p>Provides CRUD operations and custom queries for element-trait associations, including lookups
 * by element, trait, and inheritance status. Uses JOIN FETCH to eagerly load relationships and
 * prevent lazy loading issues.
 */
public interface ElementTraitRepository extends JpaRepository<ElementTrait, Long> {

  /**
   * Finds all traits associated with an element.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param element the element
   * @return list of element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.element = :element")
  List<ElementTrait> findByElement(@Param("element") Element element);

  /**
   * Finds all traits associated with an element by its UUID.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param elementId the element UUID
   * @return list of element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element e JOIN FETCH et.trait WHERE e.elementId = :elementId")
  List<ElementTrait> findByElementId(@Param("elementId") UUID elementId);

  /**
   * Finds all elements associated with a trait.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param trait the trait
   * @return list of element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.trait = :trait")
  List<ElementTrait> findByTrait(@Param("trait") Trait trait);

  /**
   * Finds all elements associated with a trait by its UUID.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param traitId the trait UUID
   * @return list of element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait t WHERE t.traitId = :traitId")
  List<ElementTrait> findByTraitId(@Param("traitId") UUID traitId);

  /**
   * Finds a specific element-trait association.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param element the element
   * @param trait the trait
   * @return Optional containing the association if found
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.element = :element AND et.trait = :trait")
  Optional<ElementTrait> findByElementAndTrait(
      @Param("element") Element element, @Param("trait") Trait trait);

  /**
   * Finds a specific element-trait association by UUIDs.
   *
   * <p>Eagerly loads element and trait relationships to prevent LazyInitializationException in REST
   * responses.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @return Optional containing the association if found
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element e JOIN FETCH et.trait t WHERE e.elementId = :elementId AND t.traitId = :traitId")
  Optional<ElementTrait> findByElementIdAndTraitId(
      @Param("elementId") UUID elementId, @Param("traitId") UUID traitId);

  /**
   * Finds all inherited trait values for an element.
   *
   * <p>Returns only traits that were inherited from parent elements.
   *
   * @param element the element
   * @return list of inherited element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.element = :element AND et.inherited = true")
  List<ElementTrait> findInheritedByElement(@Param("element") Element element);

  /**
   * Finds all directly assigned (non-inherited) trait values for an element.
   *
   * <p>Returns only traits that were explicitly set on this element, not inherited.
   *
   * @param element the element
   * @return list of directly assigned element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.element = :element AND et.inherited = false")
  List<ElementTrait> findDirectByElement(@Param("element") Element element);

  /**
   * Finds all inherited trait values for an element by UUID.
   *
   * <p>Returns only traits that were inherited from parent elements.
   *
   * @param elementId the element UUID
   * @return list of inherited element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element e JOIN FETCH et.trait WHERE e.elementId = :elementId AND et.inherited = true")
  List<ElementTrait> findInheritedByElementId(@Param("elementId") UUID elementId);

  /**
   * Finds all directly assigned (non-inherited) trait values for an element by UUID.
   *
   * <p>Returns only traits that were explicitly set on this element, not inherited.
   *
   * @param elementId the element UUID
   * @return list of directly assigned element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element e JOIN FETCH et.trait WHERE e.elementId = :elementId AND et.inherited = false")
  List<ElementTrait> findDirectByElementId(@Param("elementId") UUID elementId);

  /**
   * Finds all elements that inherited a trait value from a specific element.
   *
   * <p>Returns all children/descendants that inherited this trait from the given parent.
   *
   * @param inheritedFromElementId the parent element's internal ID
   * @return list of element-trait associations
   */
  @Query(
      "SELECT et FROM ElementTrait et JOIN FETCH et.element JOIN FETCH et.trait WHERE et.inheritedFromElementId = :inheritedFromElementId")
  List<ElementTrait> findByInheritedFromElementId(
      @Param("inheritedFromElementId") Long inheritedFromElementId);

  /**
   * Checks if an element has a specific trait assigned.
   *
   * @param element the element
   * @param trait the trait
   * @return true if the element has this trait
   */
  boolean existsByElementAndTrait(Element element, Trait trait);

  /**
   * Deletes all trait associations for an element.
   *
   * <p>Used during element deletion to cascade-remove all trait values.
   *
   * @param element the element
   */
  void deleteByElement(Element element);

  /**
   * Deletes all trait associations for a trait.
   *
   * <p>Used during trait deletion to cascade-remove all element associations.
   *
   * @param trait the trait
   */
  void deleteByTrait(Trait trait);

  /**
   * Counts the number of elements with a specific trait.
   *
   * @param trait the trait
   * @return count of elements
   */
  long countByTrait(Trait trait);
}
