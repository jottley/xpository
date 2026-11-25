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

import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.dto.CreateGroupRequestDto;
import net.ottleys.xpository.dto.GroupResponseDto;
import net.ottleys.xpository.dto.UpdateGroupRequestDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for Group entity and DTOs.
 *
 * <p>Converts between Group entities and request/response DTOs. Never exposes internal Long IDs in
 * response DTOs.
 */
@Component
public class GroupMapper {

  /**
   * Converts a Group entity to a response DTO.
   *
   * @param group the group entity
   * @param memberCount the number of members (passed separately to avoid lazy loading)
   * @return the response DTO
   */
  public GroupResponseDto toResponseDto(Group group, long memberCount) {
    if (group == null) {
      return null;
    }

    return new GroupResponseDto(
        group.getGroupId(),
        group.getName(),
        group.getDescription(),
        group.isSystemGroup(),
        group.isInheritPermissions(),
        group.getParentGroup() != null ? group.getParentGroup().getGroupId() : null,
        memberCount,
        group.getCreatedDate(),
        group.getCreatedBy(),
        group.getModifiedDate(),
        group.getModifiedBy());
  }

  /**
   * Converts a Group entity to a response DTO (without member count).
   *
   * @param group the group entity
   * @return the response DTO with memberCount = 0
   */
  public GroupResponseDto toResponseDto(Group group) {
    return toResponseDto(group, 0);
  }

  /**
   * Converts a create request DTO to a Group entity.
   *
   * @param dto the create request DTO
   * @return the group entity
   */
  public Group toEntity(CreateGroupRequestDto dto) {
    if (dto == null) {
      return null;
    }

    Group group = new Group();
    group.setName(dto.name());
    group.setDescription(dto.description());
    group.setInheritPermissions(dto.inheritPermissions());

    return group;
  }

  /**
   * Converts an update request DTO to a Group entity.
   *
   * <p>Only sets fields that are non-null in the DTO.
   *
   * @param dto the update request DTO
   * @return the group entity
   */
  public Group toEntity(UpdateGroupRequestDto dto) {
    if (dto == null) {
      return null;
    }

    Group group = new Group();
    if (dto.name() != null) {
      group.setName(dto.name());
    }
    if (dto.description() != null) {
      group.setDescription(dto.description());
    }
    if (dto.inheritPermissions() != null) {
      group.setInheritPermissions(dto.inheritPermissions());
    }

    return group;
  }
}
