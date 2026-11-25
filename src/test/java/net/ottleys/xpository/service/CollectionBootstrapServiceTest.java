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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.repository.CollectionRepository;

/**
 * Unit tests for CollectionBootstrapService system root collection creation.
 *
 * <p>Tests verify that the System Root collection is created on application startup with correct
 * properties (TREE type, system flag, no permission inheritance), is not duplicated if it already
 * exists, and handles repository interactions properly. Uses Mockito for isolated unit testing.
 */
@ExtendWith(MockitoExtension.class)
class CollectionBootstrapServiceTest {

  @Mock private CollectionRepository collectionRepository;

  @InjectMocks private CollectionBootstrapService bootstrapService;

  @BeforeEach
  void setUp() {
    reset(collectionRepository);
  }

  // ==================== Happy Path Tests ====================

  @Test
  void shouldCreateSystemRootOnStartup() {
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.empty());
    when(collectionRepository.save(any(Collection.class))).thenAnswer(i -> i.getArgument(0));

    bootstrapService.createSystemRootCollection();

    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    verify(collectionRepository).save(captor.capture());

    Collection saved = captor.getValue();
    assertThat(saved.getName()).isEqualTo("System Root");
    assertThat(saved.getType()).isEqualTo(CollectionType.LIST);
    assertThat(saved.isSystemCollection()).isTrue();
    assertThat(saved.getCreatedBy()).isEqualTo("system");
    assertThat(saved.getModifiedBy()).isEqualTo("system");
    assertThat(saved.isInheritPermissions()).isFalse();
  }

  @Test
  void shouldNotDuplicateSystemRootIfExists() {
    Collection existingRoot = new Collection("System Root", CollectionType.LIST);
    existingRoot.setSystemCollection(true);
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.of(existingRoot));

    bootstrapService.createSystemRootCollection();

    verify(collectionRepository, never()).save(any(Collection.class));
  }

  @Test
  void shouldSetCorrectPropertiesOnSystemRoot() {
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.empty());
    when(collectionRepository.save(any(Collection.class))).thenAnswer(i -> i.getArgument(0));

    bootstrapService.createSystemRootCollection();

    ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
    verify(collectionRepository).save(captor.capture());

    Collection saved = captor.getValue();
    assertThat(saved.getDescription())
        .isEqualTo(
            "System root collection. All top-level collections are contained within this"
                + " collection.");
    assertThat(saved.isSystemCollection()).isTrue();
    assertThat(saved.isInheritPermissions()).isFalse();
  }

  @Test
  void shouldCallRepositoryOnlyOnce() {
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.empty());
    when(collectionRepository.save(any(Collection.class))).thenAnswer(i -> i.getArgument(0));

    bootstrapService.createSystemRootCollection();

    verify(collectionRepository, times(1)).findSystemRootCollection();
    verify(collectionRepository, times(1)).save(any(Collection.class));
  }

  // ==================== Sad Path Tests ====================

  @Test
  void shouldHandleExistingSystemRootGracefully() {
    Collection existingRoot = new Collection("System Root", CollectionType.TREE);
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.of(existingRoot));

    // Should not throw exception
    bootstrapService.createSystemRootCollection();

    verify(collectionRepository, times(1)).findSystemRootCollection();
    verify(collectionRepository, never()).save(any(Collection.class));
  }

  @Test
  void shouldOnlyCheckSystemRootOnce() {
    Collection existingRoot = new Collection("System Root", CollectionType.TREE);
    when(collectionRepository.findSystemRootCollection()).thenReturn(Optional.of(existingRoot));

    bootstrapService.createSystemRootCollection();
    bootstrapService.createSystemRootCollection();

    verify(collectionRepository, times(2)).findSystemRootCollection();
    verify(collectionRepository, never()).save(any(Collection.class));
  }
}
