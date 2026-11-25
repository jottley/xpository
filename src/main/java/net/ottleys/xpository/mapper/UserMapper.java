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

import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.dto.CreateUserRequestDto;
import net.ottleys.xpository.dto.UpdateUserRequestDto;
import net.ottleys.xpository.dto.UserResponseDto;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entity and DTOs.
 *
 * <p>Converts between User entities and request/response DTOs. Never exposes internal Long IDs or
 * password hashes in response DTOs.
 */
@Component
public class UserMapper {

  /**
   * Converts a User entity to a response DTO.
   *
   * @param user the user entity
   * @return the response DTO
   */
  public UserResponseDto toResponseDto(User user) {
    if (user == null) {
      return null;
    }

    return new UserResponseDto(
        user.getUserId(),
        user.getUsername(),
        user.getEmail(),
        user.getDisplayName(),
        user.isEnabled(),
        user.isAccountLocked(),
        user.isAccountExpired(),
        user.isCredentialsExpired(),
        user.getLastLoginDate(),
        user.getCreatedDate(),
        user.getCreatedBy(),
        user.getModifiedDate(),
        user.getModifiedBy());
  }

  /**
   * Converts a create request DTO to a User entity.
   *
   * @param dto the create request DTO
   * @return the user entity (without password set)
   */
  public User toEntity(CreateUserRequestDto dto) {
    if (dto == null) {
      return null;
    }

    User user = new User();
    user.setUsername(dto.username());
    user.setEmail(dto.email());
    user.setDisplayName(dto.displayName());
    user.setEnabled(dto.enabled());

    return user;
  }

  /**
   * Converts an update request DTO to a User entity.
   *
   * <p>Only sets fields that are non-null in the DTO.
   *
   * @param dto the update request DTO
   * @return the user entity
   */
  public User toEntity(UpdateUserRequestDto dto) {
    if (dto == null) {
      return null;
    }

    User user = new User();
    if (dto.username() != null) {
      user.setUsername(dto.username());
    }
    if (dto.email() != null) {
      user.setEmail(dto.email());
    }
    if (dto.displayName() != null) {
      user.setDisplayName(dto.displayName());
    }
    if (dto.enabled() != null) {
      user.setEnabled(dto.enabled());
    }
    if (dto.accountLocked() != null) {
      user.setAccountLocked(dto.accountLocked());
    }
    if (dto.accountExpired() != null) {
      user.setAccountExpired(dto.accountExpired());
    }
    if (dto.credentialsExpired() != null) {
      user.setCredentialsExpired(dto.credentialsExpired());
    }

    return user;
  }
}
