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
package net.ottleys.xpository.service;

import java.util.List;
import java.util.Optional;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementVersion;
import net.ottleys.xpository.repository.ElementVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing element version history.
 *
 * <p>Handles creation of version snapshots, retrieval of version history, and navigation between
 * versions. Versions follow semantic versioning (major.minor) where major versions represent
 * breaking changes and minor versions represent backward-compatible changes.
 *
 * <p>All version operations are transactional to ensure consistency between element state and
 * version history.
 */
@Service
@Transactional
public class ElementVersionService {

  private final ElementVersionRepository versionRepository;

  public ElementVersionService(ElementVersionRepository versionRepository) {
    this.versionRepository = versionRepository;
  }

  /**
   * Creates a version snapshot of an element's current state.
   *
   * <p>Captures the element's current properties, type, and version numbers in an immutable version
   * record. This should be called before updating an element to preserve its pre-update state.
   *
   * @param element the element to snapshot
   * @param versionComment optional comment describing the changes in this version
   * @param createdBy username of user creating the version
   * @return the created version snapshot
   */
  public ElementVersion createVersionSnapshot(
      Element element, String versionComment, String createdBy) {
    return createVersionSnapshot(
        element,
        element.getMajorVersion(),
        element.getMinorVersion(),
        element.getType().getName(),
        element.getPropertiesJson(),
        versionComment,
        createdBy);
  }

  /**
   * Creates a version snapshot of an element with explicitly provided state.
   *
   * @param element the element entity that owns the version
   * @param majorVersion major version number of the snapshot
   * @param minorVersion minor version number of the snapshot
   * @param elementTypeName element type name of the snapshot
   * @param propertiesJson properties JSON of the snapshot
   * @param versionComment optional comment describing the changes
   * @param createdBy username of user creating the version
   * @return the created version snapshot
   */
  public ElementVersion createVersionSnapshot(
      Element element,
      Integer majorVersion,
      Integer minorVersion,
      String elementTypeName,
      String propertiesJson,
      String versionComment,
      String createdBy) {
    ElementVersion version =
        new ElementVersion(
            element, majorVersion, minorVersion, elementTypeName, propertiesJson, createdBy);
    version.setVersionComment(versionComment);
    return versionRepository.save(version);
  }

  /**
   * Increments the element's minor version and creates a snapshot of the previous version.
   *
   * <p>Minor version changes represent backward-compatible updates. Creates a snapshot of the
   * current state, then increments minorVersion by 1.
   *
   * @param element the element to version
   * @param versionComment optional comment describing the changes
   * @param modifiedBy username of user making the change
   * @return the created version snapshot of the previous state
   */
  public ElementVersion incrementMinorVersion(
      Element element, String versionComment, String modifiedBy) {
    // Create snapshot of current state before incrementing
    ElementVersion snapshot = createVersionSnapshot(element, versionComment, modifiedBy);

    // Increment minor version
    element.setMinorVersion(element.getMinorVersion() + 1);

    return snapshot;
  }

  /**
   * Increments the element's major version and creates a snapshot of the previous version.
   *
   * <p>Major version changes represent breaking changes. Creates a snapshot of the current state,
   * then increments majorVersion by 1 and resets minorVersion to 0.
   *
   * @param element the element to version
   * @param versionComment optional comment describing the breaking changes
   * @param modifiedBy username of user making the change
   * @return the created version snapshot of the previous state
   */
  public ElementVersion incrementMajorVersion(
      Element element, String versionComment, String modifiedBy) {
    // Create snapshot of current state before incrementing
    ElementVersion snapshot = createVersionSnapshot(element, versionComment, modifiedBy);

    // Increment major version and reset minor
    element.setMajorVersion(element.getMajorVersion() + 1);
    element.setMinorVersion(0);

    return snapshot;
  }

  /**
   * Retrieves all versions of an element, ordered newest to oldest.
   *
   * @param element the element
   * @return list of all versions ordered by version descending
   */
  @Transactional(readOnly = true)
  public List<ElementVersion> getVersionHistory(Element element) {
    return versionRepository.findByElementOrderByVersionDesc(element);
  }

  /**
   * Retrieves a specific version of an element.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @param minorVersion the minor version number
   * @return Optional containing the version if it exists
   */
  @Transactional(readOnly = true)
  public Optional<ElementVersion> getVersion(
      Element element, Integer majorVersion, Integer minorVersion) {
    return versionRepository.findByElementAndMajorVersionAndMinorVersion(
        element, majorVersion, minorVersion);
  }

  /**
   * Retrieves the latest version snapshot for an element.
   *
   * <p>Note: This returns the latest snapshot, which may be behind the element's current version if
   * the element has been modified without creating a snapshot.
   *
   * @param element the element
   * @return Optional containing the latest version snapshot
   */
  @Transactional(readOnly = true)
  public Optional<ElementVersion> getLatestVersion(Element element) {
    return versionRepository.findLatestVersionByElement(element);
  }

  /**
   * Gets all distinct major versions for an element.
   *
   * @param element the element
   * @return list of major version numbers in descending order
   */
  @Transactional(readOnly = true)
  public List<Integer> getMajorVersions(Element element) {
    return versionRepository.findDistinctMajorVersionsByElement(element);
  }

  /**
   * Gets all versions within a specific major version.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @return list of versions within the major version, ordered by minor version descending
   */
  @Transactional(readOnly = true)
  public List<ElementVersion> getVersionsForMajorVersion(Element element, Integer majorVersion) {
    return versionRepository.findByElementAndMajorVersionOrderByMinorVersionDesc(
        element, majorVersion);
  }

  /**
   * Counts the total number of versions for an element.
   *
   * @param element the element
   * @return total version count
   */
  @Transactional(readOnly = true)
  public long getVersionCount(Element element) {
    return versionRepository.countByElement(element);
  }

  /**
   * Checks if a specific version exists.
   *
   * @param element the element
   * @param majorVersion the major version number
   * @param minorVersion the minor version number
   * @return true if the version exists
   */
  @Transactional(readOnly = true)
  public boolean versionExists(Element element, Integer majorVersion, Integer minorVersion) {
    return versionRepository.existsByElementAndMajorVersionAndMinorVersion(
        element, majorVersion, minorVersion);
  }
}
