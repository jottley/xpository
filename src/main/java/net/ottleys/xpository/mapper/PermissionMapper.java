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

import net.ottleys.xpository.domain.Permission;
import net.ottleys.xpository.dto.PermissionResponseDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for Permission entity and DTOs.
 *
 * <p>Converts between Permission entities and response DTOs.
 */
@Component
public class PermissionMapper {

  /**
   * Converts a Permission entity to a response DTO.
   *
   * @param permission the permission entity
   * @return the response DTO
   */
  public PermissionResponseDto toResponseDto(Permission permission) {
    if (permission == null) {
      return null;
    }

    return new PermissionResponseDto(
        permission.getSubjectId(),
        permission.getSubjectType(),
        permission.getResourceId(),
        permission.getResourceType(),
        permission.getAction(),
        permission.isInherited(),
        permission.getCreatedDate(),
        permission.getCreatedBy());
  }
}
