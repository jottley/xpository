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

import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.UpdateElementRequestDto;
import org.springframework.stereotype.Component;

/**
 * Mapper utility for converting between Element entities and DTOs.
 *
 * <p>Handles the transformation logic between domain entities and API data transfer objects,
 * ensuring proper field mapping and type conversions. Separates internal domain model from external
 * API contract.
 */
@Component
public class ElementMapper {

  /**
   * Converts an Element entity to a response DTO.
   *
   * @param element the Element entity
   * @return ElementResponseDto for API response
   */
  public ElementResponseDto toResponseDto(Element element) {
    if (element == null) {
      return null;
    }

    return new ElementResponseDto(
        element.getElementId(),
        element.getVersion(),
        element.getMajorVersion(),
        element.getMinorVersion(),
        element.getVersionString(),
        element.getType() != null ? element.getType().getName() : null,
        element.getPropertiesJson(),
        element.getCreatedBy(),
        element.getCreatedDate(),
        element.getModifiedBy(),
        element.getModifiedDate());
  }

  /**
   * Converts a create request DTO to a new Element entity.
   *
   * <p>Sets all fields from the request DTO. The type field must be resolved by the service layer
   * from elementTypeName. The createdBy and modifiedBy fields must be set by the caller from the
   * authenticated user context.
   *
   * @param dto the create request DTO
   * @return new Element entity (not yet persisted)
   */
  public Element toEntity(CreateElementRequestDto dto) {
    if (dto == null) {
      return null;
    }

    Element element = new Element();
    element.setPropertiesJson(dto.getPropertiesJson());

    // Note: type must be resolved by the service layer from elementTypeName
    // Note: createdBy and modifiedBy must be set by the service layer from auth context

    return element;
  }

  /**
   * Updates an existing Element entity from an update request DTO.
   *
   * <p>Only updates non-null fields from the DTO (partial update/PATCH semantics). Does not update
   * immutable fields like elementId, createdBy, or createdDate. The modifiedBy field must be set by
   * the caller from the authenticated user context.
   *
   * <p>Sets the version field from the DTO for optimistic locking validation by JPA.
   *
   * @param element the existing Element entity to update
   * @param dto the update request DTO
   */
  public void updateEntity(Element element, UpdateElementRequestDto dto) {
    if (element == null || dto == null) {
      return;
    }

    // Set version for optimistic locking - JPA will validate it matches current version
    if (dto.getVersion() != null) {
      element.setVersion(dto.getVersion());
    }

    if (dto.getPropertiesJson() != null) {
      element.setPropertiesJson(dto.getPropertiesJson());
    }

    // Note: type update must be handled by service layer (requires lookup by name)
    // Note: modifiedBy must be set by the service layer from auth context
  }
}
