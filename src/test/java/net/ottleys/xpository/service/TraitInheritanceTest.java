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

import java.util.List;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.domain.Trait.TraitType;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import net.ottleys.xpository.repository.TraitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for trait inheritance functionality.
 *
 * <p>Tests that inheritable traits are properly copied from parent elements to child elements,
 * including value propagation, inherited flag tracking, and refresh mechanisms.
 */
@SpringBootTest
@Transactional
class TraitInheritanceTest {

  @Autowired private ElementTraitService elementTraitService;

  @Autowired private ElementRepository elementRepository;

  @Autowired private ElementTypeRepository elementTypeRepository;

  @Autowired private TraitRepository traitRepository;

  @Autowired private ElementTraitRepository elementTraitRepository;

  private ElementType testType;
  private Element parentElement;
  private Element childElement;
  private Trait inheritableTrait;
  private Trait nonInheritableTrait;

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

    // Create parent element
    parentElement = new Element();
    parentElement.setType(testType);
    parentElement.setPropertiesJson("{\"title\": \"Parent Element\"}");
    parentElement.setMajorVersion(1);
    parentElement.setMinorVersion(0);
    parentElement.setCreatedBy("test");
    parentElement.setModifiedBy("test");
    parentElement = elementRepository.save(parentElement);

    // Create child element
    childElement = new Element();
    childElement.setType(testType);
    childElement.setPropertiesJson("{\"title\": \"Child Element\"}");
    childElement.setMajorVersion(1);
    childElement.setMinorVersion(0);
    childElement.setCreatedBy("test");
    childElement.setModifiedBy("test");
    childElement = elementRepository.save(childElement);

    // Create inheritable trait with unique name
    inheritableTrait = new Trait();
    inheritableTrait.setName("Security Level " + java.util.UUID.randomUUID());
    inheritableTrait.setDescription("Security classification level");
    inheritableTrait.setTraitType(TraitType.STRING);
    inheritableTrait.setInheritable(true);
    inheritableTrait.setRequired(false);
    inheritableTrait = traitRepository.save(inheritableTrait);

    // Create non-inheritable trait with unique name
    nonInheritableTrait = new Trait();
    nonInheritableTrait.setName("Owner " + java.util.UUID.randomUUID());
    nonInheritableTrait.setDescription("Element owner");
    nonInheritableTrait.setTraitType(TraitType.STRING);
    nonInheritableTrait.setInheritable(false);
    nonInheritableTrait.setRequired(false);
    nonInheritableTrait = traitRepository.save(nonInheritableTrait);
  }

  @Test
  void shouldInheritTraitsFromParent() {
    // Assign inheritable trait to parent
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");

    // Inherit traits from parent to child
    int count =
        elementTraitService.inheritTraitsFromParent(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(1);

    // Verify child has inherited trait (filter for our specific trait)
    ElementTrait inheritedTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();

    assertThat(inheritedTrait.getTrait().getTraitId()).isEqualTo(inheritableTrait.getTraitId());
    assertThat(inheritedTrait.getValue()).isEqualTo("Confidential");
    assertThat(inheritedTrait.isInherited()).isTrue();
    assertThat(inheritedTrait.getInheritedFromElementId()).isEqualTo(parentElement.getId());
  }

  @Test
  void shouldNotInheritNonInheritableTraits() {
    // Assign non-inheritable trait to parent
    elementTraitService.assignTrait(
        parentElement.getElementId(), nonInheritableTrait.getTraitId(), "John Doe");

    // Try to inherit traits from parent to child
    int count =
        elementTraitService.inheritTraitsFromParent(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(0);

    // Verify child doesn't have this specific trait
    assertThat(
            elementTraitRepository.findByElementIdAndTraitId(
                childElement.getElementId(), nonInheritableTrait.getTraitId()))
        .isEmpty();
  }

  @Test
  void shouldNotOverrideDirectlyAssignedTraits() {
    // Assign inheritable trait to both parent and child (different values)
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");
    elementTraitService.assignTrait(
        childElement.getElementId(), inheritableTrait.getTraitId(), "Public");

    // Try to inherit traits from parent to child
    int count =
        elementTraitService.inheritTraitsFromParent(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(0); // Should not inherit since child has direct assignment

    // Verify child still has its own value
    ElementTrait childTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();

    assertThat(childTrait.getValue()).isEqualTo("Public");
    assertThat(childTrait.isInherited()).isFalse();
  }

  @Test
  void shouldInheritMultipleTraits() {
    // Create another inheritable trait with unique name
    Trait priorityTrait = new Trait();
    priorityTrait.setName("Priority " + java.util.UUID.randomUUID());
    priorityTrait.setDescription("Element priority");
    priorityTrait.setTraitType(TraitType.STRING);
    priorityTrait.setInheritable(true);
    priorityTrait.setRequired(false);
    priorityTrait = traitRepository.save(priorityTrait);

    // Assign multiple traits to parent
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");
    elementTraitService.assignTrait(
        parentElement.getElementId(), priorityTrait.getTraitId(), "High");

    // Inherit traits from parent to child
    int count =
        elementTraitService.inheritTraitsFromParent(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(2);

    // Verify child has both inherited traits
    ElementTrait inheritedSecurityTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();
    ElementTrait inheritedPriorityTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), priorityTrait.getTraitId())
            .orElseThrow();

    assertThat(inheritedSecurityTrait.isInherited()).isTrue();
    assertThat(inheritedSecurityTrait.getInheritedFromElementId()).isEqualTo(parentElement.getId());
    assertThat(inheritedPriorityTrait.isInherited()).isTrue();
    assertThat(inheritedPriorityTrait.getInheritedFromElementId()).isEqualTo(parentElement.getId());
  }

  @Test
  void shouldRemoveInheritedTraitsFromParent() {
    // Assign and inherit trait
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");
    elementTraitService.inheritTraitsFromParent(
        childElement.getElementId(), parentElement.getElementId());

    // Verify child has inherited trait
    assertThat(
            elementTraitRepository.findByElementIdAndTraitId(
                childElement.getElementId(), inheritableTrait.getTraitId()))
        .isPresent();

    // Remove inherited traits
    int count =
        elementTraitService.removeInheritedTraitsFromParent(
            childElement.getElementId(), parentElement.getId());

    assertThat(count).isEqualTo(1);

    // Verify child no longer has this specific trait
    assertThat(
            elementTraitRepository.findByElementIdAndTraitId(
                childElement.getElementId(), inheritableTrait.getTraitId()))
        .isEmpty();
  }

  @Test
  void shouldRefreshInheritedTraits() {
    // Assign and inherit trait
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");
    elementTraitService.inheritTraitsFromParent(
        childElement.getElementId(), parentElement.getElementId());

    // Verify child has original value
    ElementTrait childTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();
    assertThat(childTrait.getValue()).isEqualTo("Confidential");

    // Update parent trait value
    elementTraitService.updateTraitValue(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Secret");

    // Refresh child's inherited traits
    int count =
        elementTraitService.refreshInheritedTraits(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(1);

    // Verify child has updated value
    ElementTrait refreshedTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(childElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();
    assertThat(refreshedTrait.getValue()).isEqualTo("Secret");
    assertThat(refreshedTrait.isInherited()).isTrue();
  }

  @Test
  void shouldHandleMultipleInheritanceLevels() {
    // Create grandchild element
    Element grandchildElement = new Element();
    grandchildElement.setType(testType);
    grandchildElement.setPropertiesJson("{\"title\": \"Grandchild Element\"}");
    grandchildElement.setMajorVersion(1);
    grandchildElement.setMinorVersion(0);
    grandchildElement.setCreatedBy("test");
    grandchildElement.setModifiedBy("test");
    grandchildElement = elementRepository.save(grandchildElement);

    // Assign trait to parent
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");

    // Inherit to child
    elementTraitService.inheritTraitsFromParent(
        childElement.getElementId(), parentElement.getElementId());

    // Inherit to grandchild (from child - but child's trait is inherited, not directly assigned)
    int count =
        elementTraitService.inheritTraitsFromParent(
            grandchildElement.getElementId(), childElement.getElementId());

    // Should inherit because child has the trait (even though it's inherited)
    assertThat(count).isEqualTo(1);

    ElementTrait grandchildTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(
                grandchildElement.getElementId(), inheritableTrait.getTraitId())
            .orElseThrow();

    assertThat(grandchildTrait.getValue()).isEqualTo("Confidential");
    assertThat(grandchildTrait.isInherited()).isTrue();
    assertThat(grandchildTrait.getInheritedFromElementId()).isEqualTo(childElement.getId());
  }

  @Test
  void shouldNotDuplicateInheritedTraits() {
    // Assign and inherit trait
    elementTraitService.assignTrait(
        parentElement.getElementId(), inheritableTrait.getTraitId(), "Confidential");
    elementTraitService.inheritTraitsFromParent(
        childElement.getElementId(), parentElement.getElementId());

    // Try to inherit again
    int count =
        elementTraitService.inheritTraitsFromParent(
            childElement.getElementId(), parentElement.getElementId());

    assertThat(count).isEqualTo(0); // Should not create duplicate

    // Verify child still has the trait (no duplicates)
    List<ElementTrait> allChildTraits =
        elementTraitRepository.findByElementId(childElement.getElementId());
    long traitCount =
        allChildTraits.stream()
            .filter(et -> et.getTrait().getTraitId().equals(inheritableTrait.getTraitId()))
            .count();
    assertThat(traitCount).isEqualTo(1);
  }
}
