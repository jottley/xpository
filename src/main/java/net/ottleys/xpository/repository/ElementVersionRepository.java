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
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for ElementVersion operations.
 *
 * <p>Manages version history for elements, supporting queries for version retrieval, listing all
 * versions of an element, and finding specific version snapshots.
 */
public interface ElementVersionRepository extends JpaRepository<ElementVersion, Long> {

  /**
   * Finds all versions of an element, ordered by version (newest first).
   *
   * @param element the element
   * @return list of all versions ordered by major.minor descending
   */
  @Query(
      "SELECT v FROM ElementVersion v JOIN FETCH v.element WHERE v.element = :element ORDER BY v.majorVersion DESC, v.minorVersion DESC")
  List<ElementVersion> findByElementOrderByVersionDesc(@Param("element") Element element);

  /**
   * Finds a specific version of an element.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @param minorVersion the minor version number
   * @return Optional containing the version if it exists
   */
  @Query(
      "SELECT v FROM ElementVersion v JOIN FETCH v.element WHERE v.element = :element AND v.majorVersion = :major AND v.minorVersion = :minor")
  Optional<ElementVersion> findByElementAndMajorVersionAndMinorVersion(
      @Param("element") Element element,
      @Param("major") Integer majorVersion,
      @Param("minor") Integer minorVersion);

  /**
   * Checks if a specific version exists for an element.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @param minorVersion the minor version number
   * @return true if the version exists
   */
  boolean existsByElementAndMajorVersionAndMinorVersion(
      Element element, Integer majorVersion, Integer minorVersion);

  /**
   * Finds the latest version snapshot for an element.
   *
   * @param element the element
   * @return Optional containing the latest version
   */
  @Query(
      "SELECT v FROM ElementVersion v WHERE v.element = :element ORDER BY v.majorVersion DESC, v.minorVersion DESC LIMIT 1")
  Optional<ElementVersion> findLatestVersionByElement(@Param("element") Element element);

  /**
   * Finds all major versions of an element.
   *
   * @param element the element
   * @return list of distinct major version numbers in descending order
   */
  @Query(
      "SELECT DISTINCT v.majorVersion FROM ElementVersion v WHERE v.element = :element ORDER BY v.majorVersion DESC")
  List<Integer> findDistinctMajorVersionsByElement(@Param("element") Element element);

  /**
   * Finds all minor versions within a specific major version.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @return list of versions within the major version, ordered by minor version descending
   */
  @Query(
      "SELECT v FROM ElementVersion v WHERE v.element = :element AND v.majorVersion = :majorVersion ORDER BY v.minorVersion DESC")
  List<ElementVersion> findByElementAndMajorVersionOrderByMinorVersionDesc(
      @Param("element") Element element, @Param("majorVersion") Integer majorVersion);

  /**
   * Counts the total number of versions for an element.
   *
   * @param element the element
   * @return total version count
   */
  long countByElement(Element element);

  /**
   * Deletes all versions for an element.
   *
   * @param element the element
   */
  void deleteByElement(Element element);
}
