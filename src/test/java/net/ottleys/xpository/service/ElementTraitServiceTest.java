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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.TraitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for ElementTraitService. */
@ExtendWith(MockitoExtension.class)
class ElementTraitServiceTest {

  @Mock private ElementTraitRepository elementTraitRepository;

  @Mock private ElementRepository elementRepository;

  @Mock private TraitRepository traitRepository;

  @InjectMocks private ElementTraitService elementTraitService;

  private ObjectMapper objectMapper;

  private Element testElement;
  private Trait testTrait;
  private ElementTrait testElementTrait;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    elementTraitService =
        new ElementTraitService(
            elementTraitRepository, elementRepository, traitRepository, objectMapper);

    ElementType elementType = new ElementType();
    elementType.setId(1L);
    elementType.setName("xpository:core:standard");

    testElement = new Element();
    testElement.setId(1L);
    testElement.setElementId(UUID.randomUUID());
    testElement.setType(elementType);
    testElement.setPropertiesJson("{}");

    testTrait = new Trait();
    testTrait.setId(1L);
    testTrait.setTraitId(UUID.randomUUID());
    testTrait.setName("Security Classification");
    testTrait.setTraitType(Trait.TraitType.STRING);
    testTrait.setAllowedValuesJson("[\"Public\", \"Confidential\", \"Secret\"]");

    testElementTrait = new ElementTrait(testElement, testTrait, "Confidential");
    testElementTrait.setId(1L);
  }

  @Test
  void findByElementId_shouldReturnElementTraits() {
    when(elementTraitRepository.findByElementId(testElement.getElementId()))
        .thenReturn(List.of(testElementTrait));

    List<ElementTrait> result = elementTraitService.findByElementId(testElement.getElementId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getValue()).isEqualTo("Confidential");
  }

  @Test
  void findByTraitId_shouldReturnElementTraits() {
    when(elementTraitRepository.findByTraitId(testTrait.getTraitId()))
        .thenReturn(List.of(testElementTrait));

    List<ElementTrait> result = elementTraitService.findByTraitId(testTrait.getTraitId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTrait().getName()).isEqualTo("Security Classification");
  }

  @Test
  void findByElementIdAndTraitId_shouldReturnElementTrait() {
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.of(testElementTrait));

    Optional<ElementTrait> result =
        elementTraitService.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId());

    assertThat(result).isPresent();
    assertThat(result.get().getValue()).isEqualTo("Confidential");
  }

  @Test
  void findInheritedByElementId_shouldReturnInheritedTraits() {
    testElementTrait.setInherited(true);
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(elementTraitRepository.findInheritedByElement(testElement))
        .thenReturn(List.of(testElementTrait));

    List<ElementTrait> result =
        elementTraitService.findInheritedByElementId(testElement.getElementId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isInherited()).isTrue();
  }

  @Test
  void findDirectByElementId_shouldReturnDirectTraits() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(elementTraitRepository.findDirectByElement(testElement))
        .thenReturn(List.of(testElementTrait));

    List<ElementTrait> result =
        elementTraitService.findDirectByElementId(testElement.getElementId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isInherited()).isFalse();
  }

  @Test
  void assignTrait_shouldCreateAssociation() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(false);
    when(elementTraitRepository.save(any(ElementTrait.class))).thenReturn(testElementTrait);

    ElementTrait result =
        elementTraitService.assignTrait(
            testElement.getElementId(), testTrait.getTraitId(), "Confidential");

    assertThat(result).isNotNull();
    assertThat(result.getValue()).isEqualTo("Confidential");
    verify(elementTraitRepository).save(any(ElementTrait.class));
  }

  @Test
  void assignTrait_withNonexistentElement_shouldThrowException() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "Confidential"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Element not found");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withNonexistentTrait_shouldThrowException() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "Confidential"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Trait not found");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withAlreadyAssignedTrait_shouldThrowException() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(true);

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "Confidential"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("already assigned");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withInvalidValue_shouldThrowException() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(false);

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "Invalid"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("not in the allowed values list");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withNumberType_shouldValidateNumber() {
    testTrait.setTraitType(Trait.TraitType.NUMBER);
    testTrait.setAllowedValuesJson(null);

    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(false);

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "not a number"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("expected a number");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withBooleanType_shouldValidateBoolean() {
    testTrait.setTraitType(Trait.TraitType.BOOLEAN);
    testTrait.setAllowedValuesJson(null);

    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(false);

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "maybe"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("expected 'true' or 'false'");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void assignTrait_withDateType_shouldValidateDate() {
    testTrait.setTraitType(Trait.TraitType.DATE);
    testTrait.setAllowedValuesJson(null);

    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.existsByElementAndTrait(testElement, testTrait)).thenReturn(false);

    assertThatThrownBy(() ->
            elementTraitService.assignTrait(
                testElement.getElementId(), testTrait.getTraitId(), "not a date"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("expected ISO-8601 date");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void updateTraitValue_shouldUpdateValue() {
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.of(testElementTrait));
    when(elementTraitRepository.save(testElementTrait)).thenReturn(testElementTrait);

    ElementTrait result =
        elementTraitService.updateTraitValue(
            testElement.getElementId(), testTrait.getTraitId(), "Secret");

    assertThat(result.getValue()).isEqualTo("Secret");
    verify(elementTraitRepository).save(testElementTrait);
  }

  @Test
  void updateTraitValue_withNonexistentAssociation_shouldThrowException() {
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            elementTraitService.updateTraitValue(
                testElement.getElementId(), testTrait.getTraitId(), "Secret"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not assigned to this element");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void updateTraitValue_withInheritedTrait_shouldThrowException() {
    testElementTrait.setInherited(true);
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.of(testElementTrait));

    assertThatThrownBy(() ->
            elementTraitService.updateTraitValue(
                testElement.getElementId(), testTrait.getTraitId(), "Secret"))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("Cannot update inherited trait");

    verify(elementTraitRepository, never()).save(any());
  }

  @Test
  void removeTrait_shouldDeleteAssociation() {
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.of(testElementTrait));

    boolean result =
        elementTraitService.removeTrait(testElement.getElementId(), testTrait.getTraitId());

    assertThat(result).isTrue();
    verify(elementTraitRepository).delete(testElementTrait);
  }

  @Test
  void removeTrait_withNonexistentAssociation_shouldReturnFalse() {
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.empty());

    boolean result =
        elementTraitService.removeTrait(testElement.getElementId(), testTrait.getTraitId());

    assertThat(result).isFalse();
    verify(elementTraitRepository, never()).delete(any());
  }

  @Test
  void removeTrait_withInheritedTrait_shouldThrowException() {
    testElementTrait.setInherited(true);
    when(elementTraitRepository.findByElementIdAndTraitId(
            testElement.getElementId(), testTrait.getTraitId()))
        .thenReturn(Optional.of(testElementTrait));

    assertThatThrownBy(() ->
            elementTraitService.removeTrait(testElement.getElementId(), testTrait.getTraitId()))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("Cannot remove inherited trait");

    verify(elementTraitRepository, never()).delete(any());
  }

  @Test
  void removeAllTraits_shouldDeleteAllAssociations() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.of(testElement));

    elementTraitService.removeAllTraits(testElement.getElementId());

    verify(elementTraitRepository).deleteByElement(testElement);
  }

  @Test
  void removeAllTraits_withNonexistentElement_shouldDoNothing() {
    when(elementRepository.findByElementId(testElement.getElementId()))
        .thenReturn(Optional.empty());

    elementTraitService.removeAllTraits(testElement.getElementId());

    verify(elementTraitRepository, never()).deleteByElement(any());
  }
}
