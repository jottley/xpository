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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for ElementService.
 *
 * <p>Tests business logic, validation, and repository interactions using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class ElementServiceTest {

  @Mock private ElementRepository elementRepository;

  @Mock private ElementTypeRepository elementTypeRepository;

  @Mock private JsonSchemaValidationService schemaValidationService;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private ElementService elementService;

  private ElementType testElementType;
  private Element testElement;

  @BeforeEach
  void setUp() {
    testElementType = new ElementType();
    testElementType.setId(1L);
    testElementType.setName("xpository:core:standard");
    testElementType.setDescription("Standard element type");

    testElement = new Element();
    testElement.setId(1L);
    testElement.setElementId(UUID.randomUUID());
    testElement.setType(testElementType);
    testElement.setPropertiesJson("{\"title\": \"Test Element\"}");
    testElement.setCreatedBy("testuser");
    testElement.setModifiedBy("testuser");
  }

  @Test
  void shouldFindAllElements() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Element> expectedPage = new PageImpl<>(java.util.List.of(testElement));

    when(elementRepository.findAllWithTypes(pageable)).thenReturn(expectedPage);

    Page<Element> result = elementService.findAll(pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(testElement.getElementId(), result.getContent().get(0).getElementId());
    verify(elementRepository, times(1)).findAllWithTypes(pageable);
  }

  @Test
  void shouldFindElementByElementId() {
    UUID elementId = testElement.getElementId();
    when(elementRepository.findByElementIdWithType(elementId)).thenReturn(Optional.of(testElement));

    Optional<Element> result = elementService.findByElementId(elementId);

    assertTrue(result.isPresent());
    assertEquals(elementId, result.get().getElementId());
    verify(elementRepository, times(1)).findByElementIdWithType(elementId);
  }

  @Test
  void shouldReturnEmptyWhenElementNotFound() {
    UUID elementId = UUID.randomUUID();
    when(elementRepository.findByElementIdWithType(elementId)).thenReturn(Optional.empty());

    Optional<Element> result = elementService.findByElementId(elementId);

    assertFalse(result.isPresent());
    verify(elementRepository, times(1)).findByElementIdWithType(elementId);
  }

  @Test
  void shouldCreateElement() {
    when(elementTypeRepository.findByName(testElementType.getName()))
        .thenReturn(Optional.of(testElementType));
    when(elementRepository.save(testElement)).thenReturn(testElement);

    Element result = elementService.create(testElement);

    assertNotNull(result);
    assertEquals(testElement.getElementId(), result.getElementId());
    verify(elementTypeRepository, times(1)).findByName(testElementType.getName());
    verify(elementRepository, times(1)).save(testElement);
  }

  @Test
  void shouldThrowExceptionWhenCreatingElementWithNullType() {
    testElement.setType(null);

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> elementService.create(testElement));

    assertEquals("Element type is required", exception.getMessage());
    verify(elementRepository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenCreatingElementWithNonPersistedType() {
    testElementType.setId(null); // Type not persisted

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> elementService.create(testElement));

    assertEquals("Element type must be persisted before use", exception.getMessage());
    verify(elementRepository, never()).save(any());
  }

  @Test
  void shouldThrowExceptionWhenCreatingElementWithNonExistentType() {
    when(elementTypeRepository.findByName(testElementType.getName())).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> elementService.create(testElement));

    assertEquals(
        "Element type with name " + testElementType.getName() + " does not exist",
        exception.getMessage());
    verify(elementRepository, never()).save(any());
  }

  @Test
  void shouldUpdateElement() {
    when(elementRepository.findById(testElement.getId())).thenReturn(Optional.of(testElement));
    when(elementTypeRepository.findByName(testElementType.getName()))
        .thenReturn(Optional.of(testElementType));
    when(elementRepository.save(testElement)).thenReturn(testElement);

    Element result = elementService.update(testElement);

    assertNotNull(result);
    assertEquals(testElement.getElementId(), result.getElementId());
    assertEquals(1, result.getMajorVersion());
    assertEquals(1, result.getMinorVersion());
    assertEquals("1.1", result.getVersionString());
    verify(elementRepository, times(1)).findById(testElement.getId());
    verify(elementTypeRepository, times(1)).findByName(testElementType.getName());
    verify(elementRepository, times(1)).save(testElement);
  }

  @Test
  void shouldIncrementMajorVersionWhenSpecifiedOnUpdate() {
    when(elementRepository.findById(testElement.getId())).thenReturn(Optional.of(testElement));
    when(elementTypeRepository.findByName(testElementType.getName()))
        .thenReturn(Optional.of(testElementType));
    when(elementRepository.save(testElement)).thenReturn(testElement);

    Element result = elementService.update(testElement, true);

    assertNotNull(result);
    assertEquals(testElement.getElementId(), result.getElementId());
    assertEquals(2, result.getMajorVersion());
    assertEquals(0, result.getMinorVersion());
    assertEquals("2.0", result.getVersionString());
    verify(elementRepository, times(1)).findById(testElement.getId());
    verify(elementTypeRepository, times(1)).findByName(testElementType.getName());
    verify(elementRepository, times(1)).save(testElement);
  }

  @Test
  void shouldThrowExceptionWhenUpdatingElementWithNonExistentType() {
    when(elementRepository.findById(testElement.getId())).thenReturn(Optional.of(testElement));
    when(elementTypeRepository.findByName(testElementType.getName())).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> elementService.update(testElement));

    assertEquals(
        "Element type with name " + testElementType.getName() + " does not exist",
        exception.getMessage());
    verify(elementRepository, never()).save(any());
  }

  @Test
  void shouldDeleteElementByElementId() {
    UUID elementId = testElement.getElementId();
    when(elementRepository.findByElementId(elementId)).thenReturn(Optional.of(testElement));

    boolean result = elementService.deleteByElementId(elementId, "testuser");

    assertTrue(result);
    verify(elementRepository, times(1)).findByElementId(elementId);
    verify(elementRepository, times(1)).save(any(Element.class)); // Soft delete saves
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentElement() {
    UUID elementId = UUID.randomUUID();
    when(elementRepository.findByElementId(elementId)).thenReturn(Optional.empty());

    boolean result = elementService.deleteByElementId(elementId, "testuser");

    assertFalse(result);
    verify(elementRepository, times(1)).findByElementId(elementId);
    verify(elementRepository, never()).save(any(Element.class));
  }

  @Test
  void shouldCheckIfElementExists() {
    UUID elementId = testElement.getElementId();
    when(elementRepository.existsByElementId(elementId)).thenReturn(true);

    boolean result = elementService.existsByElementId(elementId);

    assertTrue(result);
    verify(elementRepository, times(1)).existsByElementId(elementId);
  }

  @Test
  void shouldReturnFalseWhenElementDoesNotExist() {
    UUID elementId = UUID.randomUUID();
    when(elementRepository.existsByElementId(elementId)).thenReturn(false);

    boolean result = elementService.existsByElementId(elementId);

    assertFalse(result);
    verify(elementRepository, times(1)).existsByElementId(elementId);
  }

  @Test
  void shouldSearchElementsWithFilters() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Element> expectedPage = new PageImpl<>(java.util.List.of(testElement));

    when(elementRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(expectedPage);

    Page<Element> result =
        elementService.searchElements("xpository:core:standard", "testuser", "testuser", pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(elementRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }
}
