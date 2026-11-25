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

import java.util.Set;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for collection validation logic in CollectionService.
 *
 * <p>Focuses on parent collection reference validation and circular reference prevention.
 */
@SpringBootTest
@Transactional
class CollectionValidationTest {

  @Autowired private CollectionService collectionService;

  @Autowired private CollectionRepository collectionRepository;

  @Autowired private ElementTypeRepository elementTypeRepository;

  private Collection parentCollection;
  private Collection childCollection;
  private ElementType documentType;
  private ElementType imageType;

  @BeforeEach
  void setUp() {
    // Clean up non-system test data
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);

    // Create test element types
    documentType = new ElementType("document");
    documentType.setDescription("Document element type for testing");
    documentType = elementTypeRepository.save(documentType);

    imageType = new ElementType("image");
    imageType.setDescription("Image element type for testing");
    imageType = elementTypeRepository.save(imageType);

    // Create parent collection
    parentCollection = new Collection("Parent", CollectionType.TREE);
    parentCollection.setDescription("Parent for testing");
    parentCollection.setCreatedBy("test");
    parentCollection.setModifiedBy("test");
    parentCollection = collectionService.create(parentCollection);

    // Create child collection
    childCollection = new Collection("Child", CollectionType.LIST);
    childCollection.setDescription("Child for testing");
    childCollection.setParentCollection(parentCollection);
    childCollection.setCreatedBy("test");
    childCollection.setModifiedBy("test");
    childCollection = collectionService.create(childCollection);
  }

  @Test
  void shouldCreateCollectionWithoutParent() {
    // When: Creating a top-level collection
    Collection topLevel = new Collection("Top Level", CollectionType.TREE);
    topLevel.setCreatedBy("test");
    topLevel.setModifiedBy("test");

    Collection created = collectionService.create(topLevel);

    // Then: Collection is created successfully
    assertThat(created.getId()).isNotNull();
    assertThat(created.getParentCollection()).isNull();
  }

  @Test
  void shouldCreateCollectionWithValidParent() {
    // When: Creating a collection with valid parent
    Collection child = new Collection("Another Child", CollectionType.LIST);
    child.setParentCollection(parentCollection);
    child.setCreatedBy("test");
    child.setModifiedBy("test");

    Collection created = collectionService.create(child);

    // Then: Collection is created successfully
    assertThat(created.getId()).isNotNull();
    assertThat(created.getParentCollection().getCollectionId())
        .isEqualTo(parentCollection.getCollectionId());
  }

  @Test
  void shouldRejectCreateWithNonExistentParent() {
    // Given: A collection reference that doesn't exist in database
    Collection fakeParent = new Collection("Fake", CollectionType.TREE);
    fakeParent.setCreatedBy("test");
    fakeParent.setModifiedBy("test");
    // Don't save it - just create the object

    // When/Then: Creating collection with unpersisted parent fails
    Collection child = new Collection("Orphan", CollectionType.LIST);
    child.setParentCollection(fakeParent);
    child.setCreatedBy("test");
    child.setModifiedBy("test");

    assertThatThrownBy(() -> collectionService.create(child))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Parent collection must be persisted");
  }

  @Test
  void shouldRejectCreateWithDeletedParent() {
    // Given: A parent that gets soft-deleted
    collectionService.deleteByCollectionId(parentCollection.getCollectionId(), "test");

    // When/Then: Creating collection with deleted parent fails
    Collection orphan = new Collection("Orphan", CollectionType.LIST);
    orphan.setParentCollection(parentCollection);
    orphan.setCreatedBy("test");
    orphan.setModifiedBy("test");

    assertThatThrownBy(() -> collectionService.create(orphan))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist or is deleted");
  }

  @Test
  void shouldUpdateCollectionParentToNull() {
    // When: Moving child to top-level (removing parent)
    childCollection.setParentCollection(null);
    Collection updated = collectionService.update(childCollection);

    // Then: Update succeeds
    assertThat(updated.getParentCollection()).isNull();

    // Verify in database
    Collection reloaded =
        collectionService.findByCollectionId(childCollection.getCollectionId()).orElseThrow();
    assertThat(reloaded.getParentCollection()).isNull();
  }

  @Test
  void shouldUpdateCollectionParentToValidCollection() {
    // Given: Another potential parent
    Collection newParent = new Collection("New Parent", CollectionType.TREE);
    newParent.setCreatedBy("test");
    newParent.setModifiedBy("test");
    newParent = collectionService.create(newParent);

    // When: Changing child's parent
    childCollection.setParentCollection(newParent);
    Collection updated = collectionService.update(childCollection);

    // Then: Update succeeds
    assertThat(updated.getParentCollection().getCollectionId())
        .isEqualTo(newParent.getCollectionId());
  }

  @Test
  void shouldRejectUpdateWithNonExistentParent() {
    // Given: A collection reference that doesn't exist
    Collection fakeParent = new Collection("Fake", CollectionType.TREE);
    fakeParent.setCreatedBy("test");
    fakeParent.setModifiedBy("test");

    // When/Then: Updating with unpersisted parent fails
    childCollection.setParentCollection(fakeParent);

    assertThatThrownBy(() -> collectionService.update(childCollection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Parent collection must be persisted");
  }

  @Test
  void shouldRejectCollectionBecomingOwnParent() {
    // When/Then: Setting collection as its own parent fails
    parentCollection.setParentCollection(parentCollection);

    assertThatThrownBy(() -> collectionService.update(parentCollection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be its own parent");
  }

  @Test
  void shouldRejectDirectCircularReference() {
    // When/Then: Parent becoming child of its own child fails
    // Current: parent -> child
    // Attempting: child -> parent (circular!)
    parentCollection.setParentCollection(childCollection);

    assertThatThrownBy(() -> collectionService.update(parentCollection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("circular reference");
  }

  @Test
  void shouldRejectIndirectCircularReference() {
    // Given: Three-level hierarchy: grandparent -> parent -> child
    Collection grandparent = new Collection("Grandparent", CollectionType.TREE);
    grandparent.setCreatedBy("test");
    grandparent.setModifiedBy("test");
    final Collection savedGrandparent = collectionService.create(grandparent);

    parentCollection.setParentCollection(savedGrandparent);
    parentCollection = collectionService.update(parentCollection);

    // When/Then: Grandparent becoming child of its grandchild fails
    // Current: grandparent -> parent -> child
    // Attempting: child -> grandparent (circular!)
    savedGrandparent.setParentCollection(childCollection);

    assertThatThrownBy(() -> collectionService.update(savedGrandparent))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("circular reference");
  }

  @Test
  void shouldAllowDeepHierarchyWithoutCircularReference() {
    // Given: Create a 5-level hierarchy
    Collection level2 = new Collection("Level 2", CollectionType.LIST);
    level2.setParentCollection(childCollection);
    level2.setCreatedBy("test");
    level2.setModifiedBy("test");
    level2 = collectionService.create(level2);

    Collection level3 = new Collection("Level 3", CollectionType.LIST);
    level3.setParentCollection(level2);
    level3.setCreatedBy("test");
    level3.setModifiedBy("test");
    level3 = collectionService.create(level3);

    Collection level4 = new Collection("Level 4", CollectionType.LIST);
    level4.setParentCollection(level3);
    level4.setCreatedBy("test");
    level4.setModifiedBy("test");
    level4 = collectionService.create(level4);

    // When: Creating another branch from parent (no cycle)
    Collection anotherBranch = new Collection("Another Branch", CollectionType.LIST);
    anotherBranch.setParentCollection(parentCollection);
    anotherBranch.setCreatedBy("test");
    anotherBranch.setModifiedBy("test");

    // Then: Should succeed - no circular reference
    Collection created = collectionService.create(anotherBranch);
    assertThat(created.getId()).isNotNull();
    assertThat(created.getParentCollection().getCollectionId())
        .isEqualTo(parentCollection.getCollectionId());
  }

  @Test
  void shouldRejectMovingParentUnderItsOwnDescendant() {
    // Given: Hierarchy parent -> child -> grandchild
    Collection grandchild = new Collection("Grandchild", CollectionType.LIST);
    grandchild.setParentCollection(childCollection);
    grandchild.setCreatedBy("test");
    grandchild.setModifiedBy("test");
    grandchild = collectionService.create(grandchild);

    // When/Then: Moving parent under grandchild creates cycle
    parentCollection.setParentCollection(grandchild);

    assertThatThrownBy(() -> collectionService.update(parentCollection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("circular reference");
  }

  // ==================== Element Type Validation Tests ====================

  @Test
  void shouldCreateCollectionWithNoAllowedElementTypes() {
    // When: Creating collection with no element type restrictions
    Collection collection = new Collection("Unrestricted", CollectionType.LIST);
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");

    Collection created = collectionService.create(collection);

    // Then: Collection is created successfully
    assertThat(created.getId()).isNotNull();
    assertThat(created.getAllowedElementTypes()).isEmpty();
  }

  @Test
  void shouldCreateCollectionWithValidElementTypes() {
    // When: Creating collection with valid element types
    Collection collection = new Collection("Restricted", CollectionType.LIST);
    collection.setAllowedElementTypes(Set.of("document", "image"));
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");

    Collection created = collectionService.create(collection);

    // Then: Collection is created successfully
    assertThat(created.getId()).isNotNull();
    assertThat(created.getAllowedElementTypes()).containsExactlyInAnyOrder("document", "image");
  }

  @Test
  void shouldRejectCreateWithNonExistentElementType() {
    // When/Then: Creating collection with non-existent element type fails
    Collection collection = new Collection("Invalid Types", CollectionType.LIST);
    collection.setAllowedElementTypes(Set.of("document", "video", "audio"));
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");

    assertThatThrownBy(() -> collectionService.create(collection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("element types do not exist")
        .hasMessageContaining("video")
        .hasMessageContaining("audio");
  }

  @Test
  void shouldRejectCreateWithAllInvalidElementTypes() {
    // When/Then: Creating collection with all invalid types fails
    Collection collection = new Collection("All Invalid", CollectionType.LIST);
    collection.setAllowedElementTypes(Set.of("nonexistent", "fake"));
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");

    assertThatThrownBy(() -> collectionService.create(collection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("element types do not exist")
        .hasMessageContaining("nonexistent")
        .hasMessageContaining("fake");
  }

  @Test
  void shouldUpdateCollectionWithValidElementTypes() {
    // Given: Existing collection with no restrictions
    Collection collection = new Collection("To Update", CollectionType.LIST);
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    final Collection savedCollection = collectionService.create(collection);

    // When: Updating with valid element types
    savedCollection.setAllowedElementTypes(Set.of("document"));
    Collection updated = collectionService.update(savedCollection);

    // Then: Update succeeds
    assertThat(updated.getAllowedElementTypes()).containsExactly("document");

    // Verify in database
    Collection reloaded =
        collectionService.findByCollectionId(savedCollection.getCollectionId()).orElseThrow();
    assertThat(reloaded.getAllowedElementTypes()).containsExactly("document");
  }

  @Test
  void shouldRejectUpdateWithInvalidElementTypes() {
    // Given: Existing collection
    Collection collection = new Collection("To Update", CollectionType.LIST);
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    final Collection savedCollection = collectionService.create(collection);

    // When/Then: Updating with invalid element types fails
    savedCollection.setAllowedElementTypes(Set.of("document", "invalid"));

    assertThatThrownBy(() -> collectionService.update(savedCollection))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("element types do not exist")
        .hasMessageContaining("invalid");
  }

  @Test
  void shouldAllowRemovingElementTypeRestrictions() {
    // Given: Collection with element type restrictions
    Collection collection = new Collection("Restricted", CollectionType.LIST);
    collection.setAllowedElementTypes(Set.of("document", "image"));
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    final Collection savedCollection = collectionService.create(collection);

    // When: Removing all restrictions (using new HashSet for mutability)
    savedCollection.setAllowedElementTypes(new java.util.HashSet<>());
    Collection updated = collectionService.update(savedCollection);

    // Then: Update succeeds
    assertThat(updated.getAllowedElementTypes()).isEmpty();
  }

  @Test
  void shouldValidateElementTypesOnCreateEvenWithEmptySet() {
    // When: Creating with explicitly empty set (no restrictions)
    Collection collection = new Collection("Explicit Empty", CollectionType.LIST);
    collection.setAllowedElementTypes(Set.of());
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");

    Collection created = collectionService.create(collection);

    // Then: Creation succeeds
    assertThat(created.getId()).isNotNull();
    assertThat(created.getAllowedElementTypes()).isEmpty();
  }

  // ==================== System Collection Protection Tests ====================

  @Test
  void shouldDeleteNonSystemCollection() {
    // Given: A regular (non-system) collection
    Collection regular = new Collection("Regular", CollectionType.LIST);
    regular.setSystemCollection(false);
    regular.setCreatedBy("test");
    regular.setModifiedBy("test");
    regular = collectionService.create(regular);

    // When: Deleting the collection
    boolean deleted = collectionService.deleteByCollectionId(regular.getCollectionId(), "test");

    // Then: Deletion succeeds
    assertThat(deleted).isTrue();

    // Verify it's deleted
    assertThat(collectionService.findByCollectionId(regular.getCollectionId())).isEmpty();
  }

  @Test
  void shouldRejectDeletionOfSystemCollection() {
    // Given: Find the System Root collection (created by SystemRootInitializer)
    java.util.List<Collection> systemCollections = collectionService.findSystemCollections();
    assertThat(systemCollections).isNotEmpty();
    Collection systemRoot = systemCollections.get(0);

    // When/Then: Attempting to delete system collection fails
    assertThatThrownBy(
            () -> collectionService.deleteByCollectionId(systemRoot.getCollectionId(), "test"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot delete system collection");
  }

  @Test
  void shouldPreventDeletionOfManuallyCreatedSystemCollection() {
    // Given: Manually create a system collection (simulating bootstrap data)
    Collection systemCollection = new Collection("System Config", CollectionType.LIST);
    systemCollection.setSystemCollection(true);
    systemCollection.setCreatedBy("system");
    systemCollection.setModifiedBy("system");
    systemCollection = collectionRepository.save(systemCollection); // Bypass service validation

    // When/Then: Attempting to delete it fails
    final UUID systemId = systemCollection.getCollectionId();
    assertThatThrownBy(() -> collectionService.deleteByCollectionId(systemId, "test"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot delete system collection")
        .hasMessageContaining("System Config");
  }
}
