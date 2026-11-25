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

import java.util.HashSet;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.UpdateCollectionRequestDto;
import org.springframework.stereotype.Component;

/**
 * Mapper utility for converting between Collection entities and DTOs.
 *
 * <p>Handles the transformation logic between domain entities and API data transfer objects,
 * ensuring proper field mapping and type conversions. Separates internal domain model from external
 * API contract.
 */
@Component
public class CollectionMapper {

  /**
   * Converts a Collection entity to a response DTO.
   *
   * @param collection the Collection entity
   * @return CollectionResponseDto for API response
   */
  public CollectionResponseDto toResponseDto(Collection collection) {
    if (collection == null) {
      return null;
    }

    return new CollectionResponseDto(
        collection.getCollectionId(),
        collection.getVersion(),
        collection.getName(),
        collection.getDescription(),
        collection.getType() != null ? collection.getType().name() : null,
        collection.getConfigurationJson(),
        collection.getAllowedElementTypes() != null
            ? new HashSet<>(collection.getAllowedElementTypes())
            : null,
        collection.getMetadataJson(),
        collection.getParentCollection() != null
            ? collection.getParentCollection().getCollectionId()
            : null,
        collection.isInheritPermissions(),
        collection.isSystemCollection(),
        collection.getCreatedBy(),
        collection.getCreatedDate(),
        collection.getModifiedBy(),
        collection.getModifiedDate());
  }

  /**
   * Converts a create request DTO to a new Collection entity.
   *
   * <p>Sets all fields from the request DTO. The createdBy and modifiedBy fields must be set by the
   * caller from the authenticated user context.
   *
   * @param dto the create request DTO
   * @return new Collection entity (not yet persisted)
   */
  public Collection toEntity(CreateCollectionRequestDto dto) {
    if (dto == null) {
      return null;
    }

    Collection collection = new Collection();
    collection.setName(dto.getName());
    collection.setDescription(dto.getDescription());
    collection.setType(CollectionType.valueOf(dto.getType()));
    collection.setConfigurationJson(dto.getConfigurationJson());
    collection.setAllowedElementTypes(
        dto.getAllowedElementTypes() != null
            ? new HashSet<>(dto.getAllowedElementTypes())
            : new HashSet<>());
    collection.setMetadataJson(dto.getMetadataJson());
    collection.setValidationRulesJson(dto.getValidationRulesJson());
    collection.setInheritPermissions(
        dto.getInheritPermissions() != null ? dto.getInheritPermissions() : true);
    collection.setSystemCollection(false); // Cannot create system collections via API

    // Note: parentCollection must be resolved by the service layer from parentCollectionId
    // Note: createdBy and modifiedBy must be set by the service layer from auth context

    return collection;
  }

  /**
   * Updates an existing Collection entity from an update request DTO.
   *
   * <p>Only updates non-null fields from the DTO (partial update/PATCH semantics). Does not update
   * immutable fields like collectionId, createdBy, or createdDate. The modifiedBy field must be set
   * by the caller from the authenticated user context.
   *
   * <p>Sets the version field from the DTO for optimistic locking validation by JPA.
   *
   * @param collection the existing Collection entity to update
   * @param dto the update request DTO
   */
  public void updateEntity(Collection collection, UpdateCollectionRequestDto dto) {
    if (collection == null || dto == null) {
      return;
    }

    // Set version for optimistic locking - JPA will validate it matches current version
    if (dto.getVersion() != null) {
      collection.setVersion(dto.getVersion());
    }

    if (dto.getName() != null) {
      collection.setName(dto.getName());
    }
    if (dto.getDescription() != null) {
      collection.setDescription(dto.getDescription());
    }
    if (dto.getConfigurationJson() != null) {
      collection.setConfigurationJson(dto.getConfigurationJson());
    }
    if (dto.getAllowedElementTypes() != null) {
      collection.setAllowedElementTypes(new HashSet<>(dto.getAllowedElementTypes()));
    }
    if (dto.getMetadataJson() != null) {
      collection.setMetadataJson(dto.getMetadataJson());
    }
    if (dto.getValidationRulesJson() != null) {
      collection.setValidationRulesJson(dto.getValidationRulesJson());
    }
    if (dto.getInheritPermissions() != null) {
      collection.setInheritPermissions(dto.getInheritPermissions());
    }

    // Note: parentCollection update must be handled by service layer (requires lookup by UUID)
    // Note: modifiedBy must be set by the service layer from auth context
  }
}
