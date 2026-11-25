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

import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.dto.CreateTraitRequestDto;
import net.ottleys.xpository.dto.TraitResponseDto;
import net.ottleys.xpository.dto.UpdateTraitRequestDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Trait entities and DTOs.
 *
 * <p>Separates external API contracts (DTOs) from internal domain models (entities).
 */
@Component
public class TraitMapper {

  /**
   * Converts a CreateTraitRequestDto to a Trait entity.
   *
   * @param dto the request DTO
   * @return the trait entity
   */
  public Trait toEntity(CreateTraitRequestDto dto) {
    Trait trait = new Trait();
    trait.setName(dto.getName());
    trait.setDescription(dto.getDescription());
    trait.setTraitType(dto.getTraitType());
    trait.setAllowedValuesJson(dto.getAllowedValuesJson());
    trait.setDefaultValue(dto.getDefaultValue());
    trait.setRequired(dto.isRequired());
    trait.setInheritable(dto.isInheritable());
    return trait;
  }

  /**
   * Applies updates from UpdateTraitRequestDto to an existing Trait entity.
   *
   * <p>Only updates fields that are present (not null) in the DTO.
   *
   * @param dto the request DTO with updates
   * @param trait the existing trait entity to update
   */
  public void applyUpdates(UpdateTraitRequestDto dto, Trait trait) {
    if (dto.getDescription() != null) {
      trait.setDescription(dto.getDescription());
    }
    if (dto.getAllowedValuesJson() != null) {
      trait.setAllowedValuesJson(dto.getAllowedValuesJson());
    }
    if (dto.getDefaultValue() != null) {
      trait.setDefaultValue(dto.getDefaultValue());
    }
    if (dto.getRequired() != null) {
      trait.setRequired(dto.getRequired());
    }
    if (dto.getInheritable() != null) {
      trait.setInheritable(dto.getInheritable());
    }
  }

  /**
   * Converts a Trait entity to a TraitResponseDto.
   *
   * <p>Exposes only public UUID, not internal database ID.
   *
   * @param trait the trait entity
   * @return the response DTO
   */
  public TraitResponseDto toResponseDto(Trait trait) {
    TraitResponseDto dto = new TraitResponseDto();
    dto.setTraitId(trait.getTraitId());
    dto.setName(trait.getName());
    dto.setDescription(trait.getDescription());
    dto.setTraitType(trait.getTraitType());
    dto.setAllowedValuesJson(trait.getAllowedValuesJson());
    dto.setDefaultValue(trait.getDefaultValue());
    dto.setRequired(trait.isRequired());
    dto.setInheritable(trait.isInheritable());
    dto.setCreatedDate(trait.getCreatedDate());
    dto.setCreatedBy(trait.getCreatedBy());
    dto.setModifiedDate(trait.getModifiedDate());
    dto.setModifiedBy(trait.getModifiedBy());
    return dto;
  }
}
