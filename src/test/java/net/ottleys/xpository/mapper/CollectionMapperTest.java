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
package net.ottleys.xpository.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.UpdateCollectionRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CollectionMapper.
 *
 * <p>Tests conversion between Collection entities and DTOs including field mapping, null handling,
 * and partial updates.
 */
class CollectionMapperTest {

  private CollectionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new CollectionMapper();
  }

  // ==================== toResponseDto Tests ====================

  @Test
  void shouldConvertEntityToResponseDto() {
    Collection collection = new Collection("Test Collection", CollectionType.LIST);
    collection.setCollectionId(UUID.randomUUID());
    collection.setDescription("Test description");
    collection.setConfigurationJson("{\"maxDepth\": 10}");
    collection.setAllowedElementTypes(Set.of("xpository:core:base"));
    collection.setMetadataJson("{\"tags\": [\"test\"]}");
    collection.setInheritPermissions(true);
    collection.setSystemCollection(false);
    collection.setCreatedBy("testuser");
    collection.setCreatedDate(LocalDateTime.now());
    collection.setModifiedBy("testuser");
    collection.setModifiedDate(LocalDateTime.now());

    CollectionResponseDto dto = mapper.toResponseDto(collection);

    assertThat(dto).isNotNull();
    assertThat(dto.collectionId()).isEqualTo(collection.getCollectionId());
    assertThat(dto.name()).isEqualTo("Test Collection");
    assertThat(dto.description()).isEqualTo("Test description");
    assertThat(dto.type()).isEqualTo("LIST");
    assertThat(dto.configurationJson()).isEqualTo("{\"maxDepth\": 10}");
    assertThat(dto.allowedElementTypes()).containsExactly("xpository:core:base");
    assertThat(dto.metadataJson()).isEqualTo("{\"tags\": [\"test\"]}");
    assertThat(dto.inheritPermissions()).isTrue();
    assertThat(dto.systemCollection()).isFalse();
    assertThat(dto.createdBy()).isEqualTo("testuser");
    assertThat(dto.modifiedBy()).isEqualTo("testuser");
  }

  @Test
  void shouldHandleNullEntityInToResponseDto() {
    CollectionResponseDto dto = mapper.toResponseDto(null);

    assertThat(dto).isNull();
  }

  @Test
  void shouldConvertEntityWithParentToResponseDto() {
    Collection parent = new Collection("Parent", CollectionType.TREE);
    parent.setCollectionId(UUID.randomUUID());

    Collection child = new Collection("Child", CollectionType.LIST);
    child.setCollectionId(UUID.randomUUID());
    child.setParentCollection(parent);

    CollectionResponseDto dto = mapper.toResponseDto(child);

    assertThat(dto).isNotNull();
    assertThat(dto.parentCollectionId()).isEqualTo(parent.getCollectionId());
  }

  // ==================== toEntity Tests ====================

  @Test
  void shouldConvertCreateDtoToEntity() {
    CreateCollectionRequestDto dto =
        new CreateCollectionRequestDto(
            "New Collection",
            "Description",
            "TREE",
            "{\"maxDepth\": 20}",
            Set.of("xpository:core:base", "xpository:core:standard"),
            "{\"tags\": [\"new\"]}",
            "{\"rules\": []}",
            null,
            true);

    Collection entity = mapper.toEntity(dto);

    assertThat(entity).isNotNull();
    assertThat(entity.getName()).isEqualTo("New Collection");
    assertThat(entity.getDescription()).isEqualTo("Description");
    assertThat(entity.getType()).isEqualTo(CollectionType.TREE);
    assertThat(entity.getConfigurationJson()).isEqualTo("{\"maxDepth\": 20}");
    assertThat(entity.getAllowedElementTypes())
        .containsExactlyInAnyOrder("xpository:core:base", "xpository:core:standard");
    assertThat(entity.getMetadataJson()).isEqualTo("{\"tags\": [\"new\"]}");
    assertThat(entity.getValidationRulesJson()).isEqualTo("{\"rules\": []}");
    assertThat(entity.isInheritPermissions()).isTrue();
    assertThat(entity.isSystemCollection()).isFalse();
  }

  @Test
  void shouldHandleNullDtoInToEntity() {
    Collection entity = mapper.toEntity(null);

    assertThat(entity).isNull();
  }

  @Test
  void shouldDefaultInheritPermissionsToTrueWhenNull() {
    CreateCollectionRequestDto dto =
        new CreateCollectionRequestDto("Test", null, "LIST", null, null, null, null, null, null);

    Collection entity = mapper.toEntity(dto);

    assertThat(entity.isInheritPermissions()).isTrue();
  }

  @Test
  void shouldNotCreateSystemCollectionFromDto() {
    CreateCollectionRequestDto dto =
        new CreateCollectionRequestDto("Test", null, "LIST", null, null, null, null, null, true);

    Collection entity = mapper.toEntity(dto);

    assertThat(entity.isSystemCollection()).isFalse();
  }

  // ==================== updateEntity Tests ====================

  @Test
  void shouldUpdateEntityFromUpdateDto() {
    Collection existing = new Collection("Original Name", CollectionType.LIST);
    existing.setDescription("Original description");
    existing.setMetadataJson("{\"old\": \"value\"}");

    UpdateCollectionRequestDto dto =
        new UpdateCollectionRequestDto(
            0L, // version
            "Updated Name",
            "Updated description",
            null,
            Set.of("xpository:core:base"),
            "{\"new\": \"value\"}",
            null,
            null,
            false);

    mapper.updateEntity(existing, dto);

    assertThat(existing.getName()).isEqualTo("Updated Name");
    assertThat(existing.getDescription()).isEqualTo("Updated description");
    assertThat(existing.getAllowedElementTypes()).containsExactly("xpository:core:base");
    assertThat(existing.getMetadataJson()).isEqualTo("{\"new\": \"value\"}");
    assertThat(existing.isInheritPermissions()).isFalse();
    assertThat(existing.getType()).isEqualTo(CollectionType.LIST); // Unchanged
  }

  @Test
  void shouldOnlyUpdateNonNullFieldsInPartialUpdate() {
    Collection existing = new Collection("Original Name", CollectionType.LIST);
    existing.setDescription("Original description");
    existing.setMetadataJson("{\"old\": \"value\"}");

    UpdateCollectionRequestDto dto =
        new UpdateCollectionRequestDto(
            0L, "Updated Name", null, null, null, null, null, null, null);

    mapper.updateEntity(existing, dto);

    assertThat(existing.getName()).isEqualTo("Updated Name");
    assertThat(existing.getDescription()).isEqualTo("Original description"); // Unchanged
    assertThat(existing.getMetadataJson()).isEqualTo("{\"old\": \"value\"}"); // Unchanged
  }

  @Test
  void shouldHandleNullEntityInUpdateEntity() {
    UpdateCollectionRequestDto dto =
        new UpdateCollectionRequestDto(0L, "Test", null, null, null, null, null, null, null);

    // Should not throw exception
    mapper.updateEntity(null, dto);
  }

  @Test
  void shouldHandleNullDtoInUpdateEntity() {
    Collection existing = new Collection("Test", CollectionType.LIST);

    // Should not throw exception
    mapper.updateEntity(existing, null);

    // Entity should remain unchanged
    assertThat(existing.getName()).isEqualTo("Test");
  }
}
