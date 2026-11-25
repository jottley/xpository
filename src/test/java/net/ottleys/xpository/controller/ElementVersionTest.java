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
package net.ottleys.xpository.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.dto.ElementVersionResponseDto;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import net.ottleys.xpository.repository.ElementVersionRepository;
import net.ottleys.xpository.service.ElementVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for element versioning functionality.
 *
 * <p>Tests version history retrieval endpoints and version snapshot creation. Uses test profile
 * with in-memory SQLite database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ElementVersionTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementTypeRepository elementTypeRepository;
  @Autowired private ElementVersionRepository versionRepository;
  @Autowired private ElementVersionService versionService;

  private ElementType standardType;
  private Element testElement;

  @BeforeEach
  void setup() {
    versionRepository.deleteAll();
    elementRepository.deleteAll();

    // Get or create standard element type
    standardType =
        elementTypeRepository
            .findByName("xpository:core:standard")
            .orElseGet(() -> {
              ElementType type = new ElementType();
              type.setName("xpository:core:standard");
              type.setDescription("Standard element type");
              return elementTypeRepository.save(type);
            });

    // Create a test element
    testElement = new Element();
    testElement.setType(standardType);
    testElement.setPropertiesJson("{\"title\": \"Test Document\", \"version\": 1}");
    testElement.setCreatedBy("testuser");
    testElement.setModifiedBy("testuser");
    testElement = elementRepository.save(testElement);
  }

  @AfterEach
  void cleanup() {
    versionRepository.deleteAll();
    elementRepository.deleteAll();
  }

  @Test
  void shouldGetEmptyVersionHistoryForNewElement() {
    // When: Getting version history for a new element (no versions created yet)
    ResponseEntity<List<ElementVersionResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/elements/" + testElement.getElementId() + "/versions",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementVersionResponseDto>>() {});

    // Then: Returns empty list
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void shouldGetVersionHistoryAfterCreatingSnapshots() {
    // Given: Element with version snapshots
    versionService.createVersionSnapshot(testElement, "Initial version", "testuser");

    // Update element and create another snapshot
    testElement.setPropertiesJson("{\"title\": \"Updated Document\", \"version\": 2}");
    testElement.setMinorVersion(1); // Increment to 1.1
    testElement = elementRepository.save(testElement);
    versionService.createVersionSnapshot(testElement, "Updated content", "testuser");

    // When: Getting version history
    ResponseEntity<List<ElementVersionResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/elements/" + testElement.getElementId() + "/versions",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementVersionResponseDto>>() {});

    // Then: Returns both versions in descending order
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);

    // Verify ordering (newest first)
    ElementVersionResponseDto firstVersion = response.getBody().get(0);
    assertThat(firstVersion.versionString()).isEqualTo("1.1");
    assertThat(firstVersion.versionComment()).isEqualTo("Updated content");
    assertThat(firstVersion.propertiesJson()).contains("Updated Document");

    ElementVersionResponseDto secondVersion = response.getBody().get(1);
    assertThat(secondVersion.versionString()).isEqualTo("1.0");
    assertThat(secondVersion.versionComment()).isEqualTo("Initial version");
  }

  @Test
  void shouldGetSpecificVersion() {
    // Given: Element with a version snapshot
    versionService.createVersionSnapshot(testElement, "Version 1.0", "testuser");

    // When: Getting specific version 1.0
    ResponseEntity<ElementVersionResponseDto> response =
        restTemplate.getForEntity(
            "/api/v1/elements/" + testElement.getElementId() + "/versions/1.0",
            ElementVersionResponseDto.class);

    // Then: Returns the specific version
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().majorVersion()).isEqualTo(1);
    assertThat(response.getBody().minorVersion()).isEqualTo(0);
    assertThat(response.getBody().versionString()).isEqualTo("1.0");
    assertThat(response.getBody().elementId()).isEqualTo(testElement.getElementId());
  }

  @Test
  void shouldReturn404ForNonexistentVersion() {
    // When: Getting a version that doesn't exist
    ResponseEntity<ElementVersionResponseDto> response =
        restTemplate.getForEntity(
            "/api/v1/elements/" + testElement.getElementId() + "/versions/99.99",
            ElementVersionResponseDto.class);

    // Then: Returns 404
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404ForNonexistentElement() {
    // When: Getting version history for non-existent element
    UUID nonExistentId = UUID.randomUUID();
    ResponseEntity<List<ElementVersionResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/elements/" + nonExistentId + "/versions",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementVersionResponseDto>>() {});

    // Then: Returns 404
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldPreserveVersionSnapshotData() {
    // Given: Element at version 1.0
    String originalProperties = testElement.getPropertiesJson();
    String originalTypeName = testElement.getType().getName();

    // Create snapshot
    versionService.createVersionSnapshot(testElement, "Snapshot before change", "testuser");

    // When: Element is modified after snapshot
    testElement.setPropertiesJson("{\"title\": \"Completely Different\"}");
    testElement.setMinorVersion(1);
    testElement = elementRepository.save(testElement);

    // Then: Snapshot still has original data
    ResponseEntity<ElementVersionResponseDto> response =
        restTemplate.getForEntity(
            "/api/v1/elements/" + testElement.getElementId() + "/versions/1.0",
            ElementVersionResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().propertiesJson()).isEqualTo(originalProperties);
    assertThat(response.getBody().elementTypeName()).isEqualTo(originalTypeName);
    assertThat(response.getBody().versionComment()).isEqualTo("Snapshot before change");
  }

  @Test
  void shouldHandleMultipleMajorVersions() {
    // Given: Element progresses through multiple major versions
    versionService.createVersionSnapshot(reloadElement(testElement), "Version 1.0", "testuser");

    // Increment to 1.1
    testElement.setMinorVersion(1);
    testElement = elementRepository.save(testElement);
    versionService.createVersionSnapshot(reloadElement(testElement), "Version 1.1", "testuser");

    // Increment to 2.0 (major version bump)
    testElement.setMajorVersion(2);
    testElement.setMinorVersion(0);
    testElement = elementRepository.save(testElement);
    versionService.createVersionSnapshot(
        reloadElement(testElement), "Version 2.0 - Breaking change", "testuser");

    // When: Getting version history
    ResponseEntity<List<ElementVersionResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/elements/" + testElement.getElementId() + "/versions",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementVersionResponseDto>>() {});

    // Then: All versions returned in order
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(3);

    // Verify descending order: 2.0, 1.1, 1.0
    assertThat(response.getBody().get(0).versionString()).isEqualTo("2.0");
    assertThat(response.getBody().get(1).versionString()).isEqualTo("1.1");
    assertThat(response.getBody().get(2).versionString()).isEqualTo("1.0");
  }

  /**
   * Helper method to reload an element from the database with type eagerly loaded.
   *
   * <p>This is needed because after saving, the element becomes detached and lazy-loaded
   * relationships may not be accessible.
   */
  private Element reloadElement(Element element) {
    return elementRepository
        .findByElementIdWithType(element.getElementId())
        .orElseThrow(() -> new IllegalStateException("Element not found"));
  }
}
