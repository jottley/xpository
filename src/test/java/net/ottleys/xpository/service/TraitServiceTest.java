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
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.TraitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Unit tests for TraitService. */
@ExtendWith(MockitoExtension.class)
class TraitServiceTest {

  @Mock private TraitRepository traitRepository;

  @Mock private ElementTraitRepository elementTraitRepository;

  @InjectMocks private TraitService traitService;

  private ObjectMapper objectMapper;

  private Trait testTrait;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    traitService = new TraitService(traitRepository, elementTraitRepository, objectMapper);

    testTrait = new Trait();
    testTrait.setId(1L);
    testTrait.setTraitId(UUID.randomUUID());
    testTrait.setName("Security Classification");
    testTrait.setDescription("Classification level");
    testTrait.setTraitType(Trait.TraitType.STRING);
    testTrait.setAllowedValuesJson("[\"Public\", \"Confidential\", \"Secret\"]");
    testTrait.setDefaultValue("Public");
    testTrait.setRequired(false);
    testTrait.setInheritable(true);
  }

  @Test
  void findAll_shouldReturnPageOfTraits() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Trait> page = new PageImpl<>(List.of(testTrait));
    when(traitRepository.findAll(pageable)).thenReturn(page);

    Page<Trait> result = traitService.findAll(pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("Security Classification");
  }

  @Test
  void searchByName_shouldReturnMatchingTraits() {
    when(traitRepository.findByNameContainingIgnoreCase("security")).thenReturn(List.of(testTrait));

    List<Trait> result = traitService.searchByName("security");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Security Classification");
  }

  @Test
  void searchByName_withBlankName_shouldReturnEmptyList() {
    List<Trait> result = traitService.searchByName("  ");

    assertThat(result).isEmpty();
    verify(traitRepository, never()).findByNameContainingIgnoreCase(any());
  }

  @Test
  void findByType_shouldReturnTraitsOfType() {
    when(traitRepository.findByTraitType(Trait.TraitType.STRING)).thenReturn(List.of(testTrait));

    List<Trait> result = traitService.findByType(Trait.TraitType.STRING);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTraitType()).isEqualTo(Trait.TraitType.STRING);
  }

  @Test
  void findRequired_shouldReturnRequiredTraits() {
    testTrait.setRequired(true);
    when(traitRepository.findByRequiredTrue()).thenReturn(List.of(testTrait));

    List<Trait> result = traitService.findRequired();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isRequired()).isTrue();
  }

  @Test
  void findInheritable_shouldReturnInheritableTraits() {
    when(traitRepository.findByInheritableTrue()).thenReturn(List.of(testTrait));

    List<Trait> result = traitService.findInheritable();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isInheritable()).isTrue();
  }

  @Test
  void findByTraitId_shouldReturnTrait() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));

    Optional<Trait> result = traitService.findByTraitId(testTrait.getTraitId());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Security Classification");
  }

  @Test
  void findByName_shouldReturnTrait() {
    when(traitRepository.findByName("Security Classification")).thenReturn(Optional.of(testTrait));

    Optional<Trait> result = traitService.findByName("Security Classification");

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Security Classification");
  }

  @Test
  void create_shouldSaveTrait() {
    when(traitRepository.existsByName("Security Classification")).thenReturn(false);
    when(traitRepository.save(testTrait)).thenReturn(testTrait);

    Trait result = traitService.create(testTrait);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Security Classification");
    verify(traitRepository).save(testTrait);
  }

  @Test
  void create_withDuplicateName_shouldThrowException() {
    when(traitRepository.existsByName("Security Classification")).thenReturn(true);

    assertThatThrownBy(() -> traitService.create(testTrait))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("already exists");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void create_withInvalidAllowedValuesJson_shouldThrowException() {
    testTrait.setAllowedValuesJson("not valid json");

    when(traitRepository.existsByName("Security Classification")).thenReturn(false);

    assertThatThrownBy(() -> traitService.create(testTrait))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("not valid JSON");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void create_withNonArrayAllowedValues_shouldThrowException() {
    testTrait.setAllowedValuesJson("{\"key\": \"value\"}");

    when(traitRepository.existsByName("Security Classification")).thenReturn(false);

    assertThatThrownBy(() -> traitService.create(testTrait))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("must be a JSON array");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void update_shouldUpdateTrait() {
    when(traitRepository.existsById(1L)).thenReturn(true);
    when(traitRepository.findByName("Security Classification")).thenReturn(Optional.of(testTrait));
    when(traitRepository.save(testTrait)).thenReturn(testTrait);

    testTrait.setDescription("Updated description");
    Trait result = traitService.update(testTrait);

    assertThat(result.getDescription()).isEqualTo("Updated description");
    verify(traitRepository).save(testTrait);
  }

  @Test
  void update_withNullId_shouldThrowException() {
    testTrait.setId(null);

    assertThatThrownBy(() -> traitService.update(testTrait))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ID is required");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void update_withNonexistentId_shouldThrowException() {
    when(traitRepository.existsById(1L)).thenReturn(false);

    assertThatThrownBy(() -> traitService.update(testTrait))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void update_withDuplicateName_shouldThrowException() {
    Trait otherTrait = new Trait();
    otherTrait.setId(2L);
    otherTrait.setName("Security Classification");

    when(traitRepository.existsById(1L)).thenReturn(true);
    when(traitRepository.findByName("Security Classification")).thenReturn(Optional.of(otherTrait));

    assertThatThrownBy(() -> traitService.update(testTrait))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("already exists");

    verify(traitRepository, never()).save(any());
  }

  @Test
  void deleteByTraitId_shouldDeleteTrait() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.countByTrait(testTrait)).thenReturn(0L);

    boolean result = traitService.deleteByTraitId(testTrait.getTraitId());

    assertThat(result).isTrue();
    verify(traitRepository).delete(testTrait);
  }

  @Test
  void deleteByTraitId_withNonexistentTrait_shouldReturnFalse() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.empty());

    boolean result = traitService.deleteByTraitId(testTrait.getTraitId());

    assertThat(result).isFalse();
    verify(traitRepository, never()).delete(any());
  }

  @Test
  void deleteByTraitId_withTraitInUse_shouldThrowException() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));
    when(elementTraitRepository.countByTrait(testTrait)).thenReturn(5L);

    assertThatThrownBy(() -> traitService.deleteByTraitId(testTrait.getTraitId()))
        .isInstanceOf(BusinessValidationException.class)
        .hasMessageContaining("is used by 5 element(s)");

    verify(traitRepository, never()).delete(any());
  }

  @Test
  void forceDeleteByTraitId_shouldDeleteTraitAndAssociations() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));

    boolean result = traitService.forceDeleteByTraitId(testTrait.getTraitId());

    assertThat(result).isTrue();
    verify(elementTraitRepository).deleteByTrait(testTrait);
    verify(traitRepository).delete(testTrait);
  }

  @Test
  void existsByTraitId_shouldReturnTrue() {
    when(traitRepository.findByTraitId(testTrait.getTraitId())).thenReturn(Optional.of(testTrait));

    boolean result = traitService.existsByTraitId(testTrait.getTraitId());

    assertThat(result).isTrue();
  }

  @Test
  void existsByName_shouldReturnTrue() {
    when(traitRepository.existsByName("Security Classification")).thenReturn(true);

    boolean result = traitService.existsByName("Security Classification");

    assertThat(result).isTrue();
  }
}
