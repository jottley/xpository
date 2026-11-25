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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;

/**
 * Unit tests for CollectionService.
 *
 * <p>Tests business logic, transactional behavior, and repository interactions using mocked
 * dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

  @Mock private CollectionRepository collectionRepository;
  @Mock private ElementTypeRepository elementTypeRepository;
  @Mock private ElementCollectionMembershipRepository membershipRepository;
  @Mock private ElementRepository elementRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private CollectionService collectionService;

  private Collection testCollection;
  private UUID testUuid;

  @BeforeEach
  void setUp() {
    testUuid = UUID.randomUUID();
    testCollection = new Collection("Test Collection", CollectionType.LIST);
    testCollection.setCollectionId(testUuid);
    testCollection.setCreatedBy("testuser");
  }

  // ==================== findAll Tests ====================

  @Test
  void shouldFindAllCollectionsWithPagination() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Collection> expectedPage = new PageImpl<>(List.of(testCollection));
    when(collectionRepository.findAllWithTypes(pageable)).thenReturn(expectedPage);

    Page<Collection> result = collectionService.findAll(pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("Test Collection");
    verify(collectionRepository).findAllWithTypes(pageable);
  }

  // ==================== findByCollectionId Tests ====================

  @Test
  void shouldFindCollectionByUuid() {
    when(collectionRepository.findByCollectionIdWithTypes(testUuid))
        .thenReturn(Optional.of(testCollection));

    Optional<Collection> result = collectionService.findByCollectionId(testUuid);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("Test Collection");
    verify(collectionRepository).findByCollectionIdWithTypes(testUuid);
  }

  @Test
  void shouldReturnEmptyWhenCollectionNotFound() {
    UUID unknownUuid = UUID.randomUUID();
    when(collectionRepository.findByCollectionIdWithTypes(unknownUuid))
        .thenReturn(Optional.empty());

    Optional<Collection> result = collectionService.findByCollectionId(unknownUuid);

    assertThat(result).isEmpty();
    verify(collectionRepository).findByCollectionIdWithTypes(unknownUuid);
  }

  // ==================== create Tests ====================

  @Test
  void shouldCreateNewCollection() {
    Collection newCollection = new Collection("New Collection", CollectionType.TREE);
    when(collectionRepository.save(newCollection)).thenReturn(newCollection);

    Collection result = collectionService.create(newCollection);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("New Collection");
    verify(collectionRepository).save(newCollection);
  }

  // ==================== update Tests ====================

  @Test
  void shouldUpdateExistingCollection() {
    testCollection.setDescription("Updated description");
    when(collectionRepository.save(testCollection)).thenReturn(testCollection);

    Collection result = collectionService.update(testCollection);

    assertThat(result).isNotNull();
    assertThat(result.getDescription()).isEqualTo("Updated description");
    verify(collectionRepository).save(testCollection);
  }

  // ==================== deleteByCollectionId Tests ====================

  @Test
  void shouldSoftDeleteCollectionWhenFound() {
    when(collectionRepository.findByCollectionId(testUuid)).thenReturn(Optional.of(testCollection));
    when(collectionRepository.save(any(Collection.class))).thenReturn(testCollection);

    boolean result = collectionService.deleteByCollectionId(testUuid, "admin");

    assertThat(result).isTrue();
    assertThat(testCollection.isDeleted()).isTrue();
    assertThat(testCollection.getDeletedBy()).isEqualTo("admin");
    assertThat(testCollection.getDeletedDate()).isNotNull();
    verify(collectionRepository).findByCollectionId(testUuid);
    verify(collectionRepository).save(testCollection);
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentCollection() {
    UUID unknownUuid = UUID.randomUUID();
    when(collectionRepository.findByCollectionId(unknownUuid)).thenReturn(Optional.empty());

    boolean result = collectionService.deleteByCollectionId(unknownUuid, "admin");

    assertThat(result).isFalse();
    verify(collectionRepository).findByCollectionId(unknownUuid);
    verify(collectionRepository, never()).save(any(Collection.class));
  }

  // ==================== existsByCollectionId Tests ====================

  @Test
  void shouldReturnTrueWhenCollectionExists() {
    when(collectionRepository.findByCollectionId(testUuid)).thenReturn(Optional.of(testCollection));

    boolean result = collectionService.existsByCollectionId(testUuid);

    assertThat(result).isTrue();
    verify(collectionRepository).findByCollectionId(testUuid);
  }

  @Test
  void shouldReturnFalseWhenCollectionDoesNotExist() {
    UUID unknownUuid = UUID.randomUUID();
    when(collectionRepository.findByCollectionId(unknownUuid)).thenReturn(Optional.empty());

    boolean result = collectionService.existsByCollectionId(unknownUuid);

    assertThat(result).isFalse();
    verify(collectionRepository).findByCollectionId(unknownUuid);
  }

  // ==================== findByParentCollectionId Tests ====================

  @Test
  void shouldFindChildCollectionsByParentId() {
    UUID parentUuid = UUID.randomUUID();
    Collection childCollection = new Collection("Child Collection", CollectionType.LIST);
    Pageable pageable = PageRequest.of(0, 20);
    Page<Collection> expectedPage = new PageImpl<>(List.of(childCollection));

    when(collectionRepository.findByParentCollectionCollectionId(parentUuid, pageable))
        .thenReturn(expectedPage);

    Page<Collection> result = collectionService.findByParentCollectionId(parentUuid, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("Child Collection");
    verify(collectionRepository).findByParentCollectionCollectionId(parentUuid, pageable);
  }

  // ==================== findSystemCollections Tests ====================

  @Test
  void shouldFindSystemCollections() {
    Collection systemCollection = new Collection("System Root", CollectionType.TREE);
    systemCollection.setSystemCollection(true);
    when(collectionRepository.findBySystemCollectionTrue()).thenReturn(List.of(systemCollection));

    List<Collection> result = collectionService.findSystemCollections();

    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isSystemCollection()).isTrue();
    assertThat(result.get(0).getName()).isEqualTo("System Root");
    verify(collectionRepository).findBySystemCollectionTrue();
  }

  @Test
  void shouldReturnEmptyListWhenNoSystemCollections() {
    when(collectionRepository.findBySystemCollectionTrue()).thenReturn(List.of());

    List<Collection> result = collectionService.findSystemCollections();

    assertThat(result).isEmpty();
    verify(collectionRepository).findBySystemCollectionTrue();
  }
}
