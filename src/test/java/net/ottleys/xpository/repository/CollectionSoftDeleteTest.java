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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Collection soft delete functionality.
 *
 * <p>Tests cover soft delete operations, data preservation, recovery, date-based queries for
 * cleanup, and verification that active queries properly exclude deleted collections. Validates the
 * 30-day retention period pattern and ensures duplicate names are allowed after soft delete.
 */
@SpringBootTest
@Transactional
class CollectionSoftDeleteTest {

  @Autowired private CollectionRepository collectionRepository;

  @BeforeEach
  void setUp() {
    collectionRepository.deleteBySystemCollectionFalse();
  }

  // ==================== Happy Path Tests ====================

  @Test
  void shouldSoftDeleteCollection() {
    Collection collection = createAndSaveCollection("ToDelete", CollectionType.LIST);

    collection.softDelete("testuser");
    collectionRepository.save(collection);

    Collection deleted = collectionRepository.findById(collection.getId()).orElseThrow();
    assertThat(deleted.isDeleted()).isTrue();
    assertThat(deleted.getDeletedDate()).isNotNull();
    assertThat(deleted.getDeletedBy()).isEqualTo("testuser");
  }

  @Test
  void shouldPreserveAllFieldsOnSoftDelete() {
    Collection collection = createAndSaveCollection("Original", CollectionType.TREE);
    collection.setDescription("Important data");
    collection.setMetadataJson("{\"key\": \"value\"}");
    collectionRepository.save(collection);

    collection.softDelete("admin");
    collectionRepository.save(collection);

    Collection deleted = collectionRepository.findById(collection.getId()).orElseThrow();
    assertThat(deleted.getName()).isEqualTo("Original");
    assertThat(deleted.getDescription()).isEqualTo("Important data");
    assertThat(deleted.getMetadataJson()).isEqualTo("{\"key\": \"value\"}");
    assertThat(deleted.getType()).isEqualTo(CollectionType.TREE);
  }

  @Test
  void shouldRecoverSoftDeletedCollection() {
    Collection collection = createAndSaveCollection("Recoverable", CollectionType.LIST);
    collection.softDelete("testuser");
    collectionRepository.save(collection);

    // Recover by unsetting deleted flag
    collection.setDeleted(false);
    collection.setDeletedDate(null);
    collection.setDeletedBy(null);
    collectionRepository.save(collection);

    Collection recovered = collectionRepository.findById(collection.getId()).orElseThrow();
    assertThat(recovered.isDeleted()).isFalse();
    assertThat(recovered.getDeletedDate()).isNull();
    assertThat(recovered.getDeletedBy()).isNull();
  }

  @Test
  void shouldFindAllDeletedCollections() {
    createAndSaveCollection("Active1", CollectionType.LIST);
    Collection deleted1 = createAndSaveCollection("Deleted1", CollectionType.LIST);
    Collection deleted2 = createAndSaveCollection("Deleted2", CollectionType.TREE);

    deleted1.softDelete("user1");
    deleted2.softDelete("user2");
    collectionRepository.save(deleted1);
    collectionRepository.save(deleted2);

    List<Collection> allDeleted = collectionRepository.findAllDeleted();

    assertThat(allDeleted).hasSize(2);
    assertThat(allDeleted)
        .extracting(Collection::getName)
        .containsExactlyInAnyOrder("Deleted1", "Deleted2");
  }

  @Test
  void shouldFindDeletedBeforeCutoffDate() {
    Collection old = createAndSaveCollection("OldDeleted", CollectionType.LIST);
    old.softDelete("testuser");
    old.setDeletedDate(LocalDateTime.now().minusDays(35));
    collectionRepository.save(old);

    Collection recent = createAndSaveCollection("RecentDeleted", CollectionType.LIST);
    recent.softDelete("testuser");
    recent.setDeletedDate(LocalDateTime.now().minusDays(5));
    collectionRepository.save(recent);

    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<Collection> oldDeleted = collectionRepository.findDeletedBefore(cutoff);

    assertThat(oldDeleted).hasSize(1);
    assertThat(oldDeleted.get(0).getName()).isEqualTo("OldDeleted");
  }

  // ==================== Sad Path Tests ====================

  @Test
  void shouldNotFindDeletedInActiveQueries() {
    createAndSaveCollection("Active", CollectionType.LIST);
    Collection deleted = createAndSaveCollection("Deleted", CollectionType.LIST);
    deleted.softDelete("testuser");
    collectionRepository.save(deleted);

    List<Collection> active = collectionRepository.findAllActive();
    assertThat(active).hasSize(2);
    assertThat(active)
        .extracting(Collection::getName)
        .containsExactlyInAnyOrder("System Root", "Active");

    List<Collection> topLevel = collectionRepository.findAllTopLevelActive();
    assertThat(topLevel).hasSize(2);
    assertThat(topLevel)
        .extracting(Collection::getName)
        .containsExactlyInAnyOrder("System Root", "Active");
  }

  @Test
  void shouldNotFindDeletedChildrenInActiveQuery() {
    Collection parent = createAndSaveCollection("Parent", CollectionType.TREE);
    Collection activeChild = createAndSaveCollection("ActiveChild", CollectionType.LIST);
    Collection deletedChild = createAndSaveCollection("DeletedChild", CollectionType.LIST);

    activeChild.setParentCollection(parent);
    deletedChild.setParentCollection(parent);
    collectionRepository.save(activeChild);
    collectionRepository.save(deletedChild);

    deletedChild.softDelete("testuser");
    collectionRepository.save(deletedChild);

    List<Collection> children = collectionRepository.findByParentCollectionActive(parent);

    assertThat(children).hasSize(1);
    assertThat(children.get(0).getName()).isEqualTo("ActiveChild");
  }

  @Test
  void shouldNotFindDeletedByActiveCollectionId() {
    Collection collection = createAndSaveCollection("ToDelete", CollectionType.LIST);
    collection.softDelete("testuser");
    collectionRepository.save(collection);

    var found = collectionRepository.findActiveByCollectionId(collection.getCollectionId());

    assertThat(found).isEmpty();
  }

  @Test
  void shouldAllowDuplicateNameAfterSoftDelete() {
    Collection original = createAndSaveCollection("Duplicate", CollectionType.LIST);
    original.softDelete("testuser");
    collectionRepository.save(original);

    // Should be able to create a new collection with the same name after soft delete
    Collection newCollection = createAndSaveCollection("Duplicate", CollectionType.TREE);

    assertThat(newCollection.getId()).isNotNull();
    assertThat(newCollection.isDeleted()).isFalse();
  }

  @Test
  void shouldReturnEmptyWhenNoDeletedCollections() {
    createAndSaveCollection("Active1", CollectionType.LIST);
    createAndSaveCollection("Active2", CollectionType.TREE);

    List<Collection> deleted = collectionRepository.findAllDeleted();

    assertThat(deleted).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenNoOldDeletedCollections() {
    Collection recent = createAndSaveCollection("Recent", CollectionType.LIST);
    recent.softDelete("testuser");
    recent.setDeletedDate(LocalDateTime.now().minusDays(5));
    collectionRepository.save(recent);

    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<Collection> oldDeleted = collectionRepository.findDeletedBefore(cutoff);

    assertThat(oldDeleted).isEmpty();
  }

  // ==================== Helper Methods ====================

  private Collection createAndSaveCollection(String name, CollectionType type) {
    Collection collection = new Collection(name, type);
    collection.setCreatedBy("testuser");
    collection.setModifiedBy("testuser");
    return collectionRepository.save(collection);
  }
}
