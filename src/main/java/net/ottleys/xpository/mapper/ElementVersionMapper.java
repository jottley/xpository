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

import net.ottleys.xpository.domain.ElementVersion;
import net.ottleys.xpository.dto.ElementVersionResponseDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting ElementVersion entities to DTOs.
 *
 * <p>Handles conversion from internal domain model to external API representation for version
 * history endpoints.
 */
@Component
public class ElementVersionMapper {

  /**
   * Converts an ElementVersion entity to a response DTO.
   *
   * @param version the ElementVersion entity
   * @return ElementVersionResponseDto for API response
   */
  public ElementVersionResponseDto toResponseDto(ElementVersion version) {
    if (version == null) {
      return null;
    }

    return new ElementVersionResponseDto(
        version.getElement().getElementId(),
        version.getMajorVersion(),
        version.getMinorVersion(),
        version.getVersionString(),
        version.getElementTypeName(),
        version.getPropertiesJson(),
        version.getVersionComment(),
        version.getCreatedDate(),
        version.getCreatedBy());
  }
}
