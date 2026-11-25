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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for element soft delete functionality.
 *
 * <p>Tests that soft-deleted elements are properly marked, excluded from queries, and can be
 * restored if needed.
 */
@SpringBootTest
@Transactional
class ElementSoftDeleteTest {

  @Autowired private ElementService elementService;

  @Autowired private ElementRepository elementRepository;

  @Autowired private ElementTypeRepository elementTypeRepository;

  private ElementType testType;
  private Element testElement;

  @BeforeEach
  void setUp() {
    // Create or get element type
    testType =
        elementTypeRepository
            .findByName("xpository-core:base")
            .orElseGet(() -> {
              ElementType type = new ElementType();
              type.setName("xpository-core:base");
              type.setDescription("Base element type for testing");
              type.setSchemaJson("{}");
              return elementTypeRepository.save(type);
            });

    // Create test element
    testElement = new Element();
    testElement.setType(testType);
    testElement.setPropertiesJson("{\"title\": \"Test Element\"}");
    testElement.setMajorVersion(1);
    testElement.setMinorVersion(0);
    testElement.setCreatedBy("test");
    testElement.setModifiedBy("test");
    testElement = elementRepository.save(testElement);
  }

  @Test
  void shouldSoftDeleteElement() {
    // Delete element
    boolean deleted = elementService.deleteByElementId(testElement.getElementId(), "testuser");

    assertThat(deleted).isTrue();

    // Element should not be found via normal queries
    Optional<Element> found = elementService.findByElementId(testElement.getElementId());
    assertThat(found).isEmpty();
  }

  @Test
  void shouldMarkElementAsDeleted() {
    // Delete element
    elementService.deleteByElementId(testElement.getElementId(), "testuser");

    // Load element directly from repository (bypasses soft delete filter)
    Element deletedElement = elementRepository.findById(testElement.getId()).orElseThrow();

    assertThat(deletedElement.isDeleted()).isTrue();
    assertThat(deletedElement.getDeletedBy()).isEqualTo("testuser");
    assertThat(deletedElement.getDeletedDate()).isNotNull();
  }

  @Test
  void shouldExcludeDeletedFromSearch() {
    // Create another element
    Element element2 = new Element();
    element2.setType(testType);
    element2.setPropertiesJson("{\"title\": \"Second Element\"}");
    element2.setMajorVersion(1);
    element2.setMinorVersion(0);
    element2.setCreatedBy("test");
    element2.setModifiedBy("test");
    element2 = elementRepository.save(element2);

    // Delete first element
    elementService.deleteByElementId(testElement.getElementId(), "testuser");

    // Search should only return non-deleted element
    Page<Element> results = elementService.searchElements(null, null, null, PageRequest.of(0, 10));

    assertThat(results.getTotalElements()).isEqualTo(1);
    assertThat(results.getContent().get(0).getElementId()).isEqualTo(element2.getElementId());
  }

  @Test
  void shouldExcludeDeletedFromFindAll() {
    // Create another element
    Element element2 = new Element();
    element2.setType(testType);
    element2.setPropertiesJson("{\"title\": \"Second Element\"}");
    element2.setMajorVersion(1);
    element2.setMinorVersion(0);
    element2.setCreatedBy("test");
    element2.setModifiedBy("test");
    element2 = elementRepository.save(element2);

    // Delete first element
    elementService.deleteByElementId(testElement.getElementId(), "testuser");

    // findAll should only return non-deleted element
    Page<Element> results = elementService.findAll(PageRequest.of(0, 10));

    assertThat(results.getTotalElements()).isEqualTo(1);
    assertThat(results.getContent().get(0).getElementId()).isEqualTo(element2.getElementId());
  }

  @Test
  void shouldReturnFalseWhenDeletingAlreadyDeleted() {
    // Delete element first time
    boolean deleted1 = elementService.deleteByElementId(testElement.getElementId(), "testuser");
    assertThat(deleted1).isTrue();

    // Try to delete again
    boolean deleted2 = elementService.deleteByElementId(testElement.getElementId(), "testuser");
    assertThat(deleted2).isFalse();
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistent() {
    // Try to delete non-existent element
    boolean deleted = elementService.deleteByElementId(java.util.UUID.randomUUID(), "testuser");
    assertThat(deleted).isFalse();
  }

  @Test
  void shouldNotExistAfterSoftDelete() {
    // Delete element
    elementService.deleteByElementId(testElement.getElementId(), "testuser");

    // existsByElementId should return false
    boolean exists = elementService.existsByElementId(testElement.getElementId());
    assertThat(exists).isFalse();
  }
}
