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

import net.ottleys.xpository.domain.ElementRelationship;
import net.ottleys.xpository.dto.RelationshipResponseDto;

/**
 * Mapper for converting ElementRelationship entities to DTOs.
 *
 * <p>Handles conversion from internal domain model to external API representation, exposing only
 * public UUIDs and never internal Long IDs.
 */
public class RelationshipMapper {

  /**
   * Converts an ElementRelationship entity to a RelationshipResponseDto.
   *
   * @param relationship the entity to convert
   * @return the response DTO
   */
  public static RelationshipResponseDto toDto(ElementRelationship relationship) {
    return new RelationshipResponseDto(
        relationship.getCollection().getCollectionId(),
        relationship.getSourceElement().getElementId(),
        relationship.getTargetElement().getElementId(),
        relationship.getRelationshipType(),
        relationship.getRelationshipMetadata(),
        relationship.getCreatedDate(),
        relationship.getCreatedBy());
  }
}
