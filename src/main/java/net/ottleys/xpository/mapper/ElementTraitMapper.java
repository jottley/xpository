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

import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.dto.ElementTraitResponseDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ElementTrait entities and DTOs.
 *
 * <p>Separates external API contracts (DTOs) from internal domain models (entities).
 */
@Component
public class ElementTraitMapper {

  /**
   * Converts an ElementTrait entity to an ElementTraitResponseDto.
   *
   * <p>Exposes public UUIDs for element and trait, not internal database IDs.
   *
   * @param elementTrait the element-trait entity
   * @return the response DTO
   */
  public ElementTraitResponseDto toResponseDto(ElementTrait elementTrait) {
    ElementTraitResponseDto dto = new ElementTraitResponseDto();
    dto.setElementId(elementTrait.getElement().getElementId());
    dto.setTraitId(elementTrait.getTrait().getTraitId());
    dto.setTraitName(elementTrait.getTrait().getName());
    dto.setValue(elementTrait.getValue());
    dto.setInherited(elementTrait.isInherited());
    dto.setInheritedFromElementId(elementTrait.getInheritedFromElementId());
    dto.setCreatedDate(elementTrait.getCreatedDate());
    dto.setCreatedBy(elementTrait.getCreatedBy());
    dto.setModifiedDate(elementTrait.getModifiedDate());
    dto.setModifiedBy(elementTrait.getModifiedBy());
    return dto;
  }
}
