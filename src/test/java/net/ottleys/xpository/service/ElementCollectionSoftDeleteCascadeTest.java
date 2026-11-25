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

import jakarta.persistence.EntityManager;
import java.util.Optional;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the cascading soft delete and restore lifecycle across collections,
 * memberships, and elements.
 */
@SpringBootTest
@Transactional
class ElementCollectionSoftDeleteCascadeTest {

  @Autowired private CollectionService collectionService;
  @Autowired private ElementCollectionMembershipService membershipService;
  @Autowired private ElementService elementService;
  @Autowired private ElementSoftDeleteCleaner cleaner;

  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementTypeRepository elementTypeRepository;
  @Autowired private ElementCollectionMembershipRepository membershipRepository;
  @Autowired private EntityManager entityManager;

  private ElementType testType;

  @BeforeEach
  void setUp() {
    testType =
        elementTypeRepository
            .findByName("xpository:core:standard")
            .orElseGet(
                () -> {
                  ElementType type = new ElementType();
                  type.setName("xpository:core:standard");
                  type.setDescription("Standard type for test");
                  type.setSchemaJson("{}");
                  return elementTypeRepository.save(type);
                });
  }

  private Element createElement(String title) {
    Element element = new Element();
    element.setType(testType);
    element.setPropertiesJson("{\"title\": \"" + title + "\"}");
    element.setCreatedBy("tester");
    element.setModifiedBy("tester");
    return elementRepository.save(element);
  }

  private Collection createCollection(String name) {
    Collection collection = new Collection(name, CollectionType.LIST);
    collection.setCreatedBy("tester");
    collection.setModifiedBy("tester");
    return collectionRepository.save(collection);
  }

  @Test
  void shouldSoftDeleteElementWhenRemovedFromItsOnlyCollection() {
    Collection col = createCollection("Col-Only");
    Element elem = createElement("Elem-1");

    membershipService.addElementToCollection(elem, col, "tester");
    entityManager.flush();

    // Remove from the only collection
    boolean removed = membershipService.removeElementFromCollection(elem, col, "tester");
    assertThat(removed).isTrue();

    entityManager.flush();
    entityManager.clear();

    // Membership record is deleted
    assertThat(membershipRepository.existsByElementAndCollection(elem, col)).isFalse();

    // Element still exists in DB but is marked soft-deleted
    Element refreshed = elementRepository.findById(elem.getId()).orElseThrow();
    assertThat(refreshed.isDeleted()).isTrue();
    assertThat(refreshed.getDeletedBy()).isEqualTo("tester");
    assertThat(refreshed.getDeletedDate()).isNotNull();

    // Hidden from active element lookups
    Optional<Element> activeElem = elementService.findByElementId(elem.getElementId());
    assertThat(activeElem).isEmpty();
  }

  @Test
  void shouldNotSoftDeleteElementWhenRemovedFromOneCollectionIfStillInAnotherActiveCollection() {
    Collection col1 = createCollection("Col-Multi-1");
    Collection col2 = createCollection("Col-Multi-2");
    Element elem = createElement("Elem-Shared");

    membershipService.addElementToCollection(elem, col1, "tester");
    membershipService.addElementToCollection(elem, col2, "tester");
    entityManager.flush();

    // Remove from col1 only
    membershipService.removeElementFromCollection(elem, col1, "tester");
    entityManager.flush();
    entityManager.clear();

    // Element must remain active because col2 is still active
    Element refreshed = elementRepository.findById(elem.getId()).orElseThrow();
    assertThat(refreshed.isDeleted()).isFalse();

    // Now remove from col2
    membershipService.removeElementFromCollection(elem, col2, "tester");
    entityManager.flush();
    entityManager.clear();

    // Now it should be soft-deleted
    refreshed = elementRepository.findById(elem.getId()).orElseThrow();
    assertThat(refreshed.isDeleted()).isTrue();
  }

  @Test
  void shouldCascadeSoftDeleteUniqueElementsWhenCollectionIsDeleted() {
    Collection col = createCollection("Col-Cascade-Delete");
    Element elem = createElement("Elem-Unique-In-Col");

    membershipService.addElementToCollection(elem, col, "tester");
    entityManager.flush();

    // Soft delete collection
    boolean deleted = collectionService.deleteByCollectionId(col.getCollectionId(), "tester");
    assertThat(deleted).isTrue();

    entityManager.flush();
    entityManager.clear();

    // Collection is soft-deleted
    Collection refreshedCol = collectionRepository.findById(col.getId()).orElseThrow();
    assertThat(refreshedCol.isDeleted()).isTrue();

    // Element is cascade soft-deleted
    Element refreshedElem = elementRepository.findById(elem.getId()).orElseThrow();
    assertThat(refreshedElem.isDeleted()).isTrue();
    assertThat(refreshedElem.getDeletedBy()).isEqualTo("tester");
  }

  @Test
  void shouldNotSoftDeleteSharedElementWhenOneOfItsCollectionsIsDeleted() {
    Collection col1 = createCollection("Col-A");
    Collection col2 = createCollection("Col-B");
    Element elem = createElement("Elem-In-Two-Collections");

    membershipService.addElementToCollection(elem, col1, "tester");
    membershipService.addElementToCollection(elem, col2, "tester");
    entityManager.flush();

    // Soft delete col1
    collectionService.deleteByCollectionId(col1.getCollectionId(), "tester");
    entityManager.flush();
    entityManager.clear();

    // col1 is soft-deleted
    Collection refreshedCol1 = collectionRepository.findById(col1.getId()).orElseThrow();
    assertThat(refreshedCol1.isDeleted()).isTrue();

    // Element remains active because col2 is still active
    Element refreshedElem = elementRepository.findById(elem.getId()).orElseThrow();
    assertThat(refreshedElem.isDeleted()).isFalse();
  }

  @Test
  void shouldCascadeRestoreElementsAndChildCollectionsWhenCollectionIsRestored() {
    Collection parent = createCollection("Parent-Col");
    Collection child = createCollection("Child-Col");
    child.setParentCollection(parent);
    collectionRepository.save(child);

    Element elem = createElement("Elem-Under-Parent");
    membershipService.addElementToCollection(elem, parent, "tester");
    entityManager.flush();

    // Soft delete parent -> cascades to child and unique element
    collectionService.deleteByCollectionId(parent.getCollectionId(), "tester");
    entityManager.flush();
    entityManager.clear();

    assertThat(collectionRepository.findById(parent.getId()).orElseThrow().isDeleted()).isTrue();
    assertThat(collectionRepository.findById(child.getId()).orElseThrow().isDeleted()).isTrue();
    assertThat(elementRepository.findById(elem.getId()).orElseThrow().isDeleted()).isTrue();

    // Restore parent
    boolean restored = collectionService.restoreCollection(parent.getCollectionId());
    assertThat(restored).isTrue();

    entityManager.flush();
    entityManager.clear();

    // All should be restored
    assertThat(collectionRepository.findById(parent.getId()).orElseThrow().isDeleted()).isFalse();
    assertThat(collectionRepository.findById(child.getId()).orElseThrow().isDeleted()).isFalse();
    assertThat(elementRepository.findById(elem.getId()).orElseThrow().isDeleted()).isFalse();
  }

  @Test
  void shouldCleanOrphanedElementsOnStartup() {
    // Create an element with no collection memberships
    Element orphan = createElement("Orphan-Elem");
    entityManager.flush();

    // Run cleaner
    cleaner.cleanOrphanElements();
    entityManager.flush();
    entityManager.clear();

    Element refreshed = elementRepository.findById(orphan.getId()).orElseThrow();
    assertThat(refreshed.isDeleted()).isTrue();
    assertThat(refreshed.getDeletedBy()).isEqualTo("system-cleanup");
  }
}
