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
package net.ottleys.xpository.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.dto.CreateUserRequestDto;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.GroupResponseDto;
import net.ottleys.xpository.dto.UpdateUserRequestDto;
import net.ottleys.xpository.dto.UserResponseDto;
import net.ottleys.xpository.mapper.GroupMapper;
import net.ottleys.xpository.mapper.UserMapper;
import net.ottleys.xpository.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for User resources.
 *
 * <p>Provides CRUD operations for users following REST conventions. All endpoints are under
 * /api/v1/users. Requires ADMIN role for most operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "APIs for managing users")
public class UserController {

  private final UserService userService;
  private final UserMapper userMapper;
  private final GroupMapper groupMapper;

  /**
   * Constructs a UserController with required dependencies.
   *
   * @param userService service for user business logic
   * @param userMapper mapper for user entity/DTO conversion
   * @param groupMapper mapper for group entity/DTO conversion
   */
  public UserController(UserService userService, UserMapper userMapper, GroupMapper groupMapper) {
    this.userService = userService;
    this.userMapper = userMapper;
    this.groupMapper = groupMapper;
  }

  /**
   * Creates a new user.
   *
   * @param request the user creation request
   * @return the created user
   */
  @Operation(summary = "Create a new user", description = "Creates a new user account")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Username or email already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponseDto> createUser(
      @Valid @RequestBody CreateUserRequestDto request) {
    User user = userMapper.toEntity(request);
    User createdUser = userService.createUser(user, request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponseDto(createdUser));
  }

  /**
   * Gets a user by UUID.
   *
   * @param userId the user UUID
   * @return the user
   */
  @Operation(summary = "Get user by ID", description = "Retrieves a user by their UUID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
  public ResponseEntity<UserResponseDto> getUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    return userService
        .findByUserId(userId)
        .map(userMapper::toResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Lists all users.
   *
   * @return list of all users
   */
  @Operation(summary = "List all users", description = "Retrieves a list of all users")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponseDto>> listUsers() {
    List<UserResponseDto> users =
        userService.findAll().stream().map(userMapper::toResponseDto).toList();
    return ResponseEntity.ok(users);
  }

  /**
   * Searches users by term.
   *
   * @param searchTerm the search term
   * @return list of matching users
   */
  @Operation(
      summary = "Search users",
      description = "Searches users by username, email, or display name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/search")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponseDto>> searchUsers(
      @Parameter(description = "Search term") @RequestParam String searchTerm) {
    List<UserResponseDto> users =
        userService.searchUsers(searchTerm).stream().map(userMapper::toResponseDto).toList();
    return ResponseEntity.ok(users);
  }

  /**
   * Updates a user.
   *
   * @param userId the user UUID
   * @param request the update request
   * @return the updated user
   */
  @Operation(summary = "Update user", description = "Updates an existing user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PatchMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponseDto> updateUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRequestDto request) {
    User updates = userMapper.toEntity(request);
    User updatedUser = userService.updateUser(userId, updates);
    return ResponseEntity.ok(userMapper.toResponseDto(updatedUser));
  }

  /**
   * Deletes a user.
   *
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Delete user", description = "Deletes a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @DeleteMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    userService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Enables a user account.
   *
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Enable user", description = "Enables a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User enabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PostMapping("/{userId}/enable")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> enableUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    userService.enableUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Disables a user account.
   *
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Disable user", description = "Disables a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User disabled successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PostMapping("/{userId}/disable")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> disableUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    userService.disableUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Locks a user account.
   *
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Lock user", description = "Locks a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User locked successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PostMapping("/{userId}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> lockUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    userService.lockUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Unlocks a user account.
   *
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Unlock user", description = "Unlocks a user account")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User unlocked successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @PostMapping("/{userId}/unlock")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> unlockUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    userService.unlockUser(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all groups a user belongs to.
   *
   * @param userId the user UUID
   * @return list of groups
   */
  @Operation(summary = "Get user groups", description = "Retrieves all groups a user belongs to")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Groups retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  @GetMapping("/{userId}/groups")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
  public ResponseEntity<List<GroupResponseDto>> getUserGroups(
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    List<GroupResponseDto> groups =
        userService.findUserGroups(userId).stream().map(groupMapper::toResponseDto).toList();
    return ResponseEntity.ok(groups);
  }

  /**
   * Adds a user to a group.
   *
   * @param userId the user UUID
   * @param groupId the group UUID
   * @return no content
   */
  @Operation(summary = "Add user to group", description = "Adds a user to a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User added to group successfully"),
        @ApiResponse(responseCode = "404", description = "User or group not found")
      })
  @PostMapping("/{userId}/groups/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> addToGroup(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Parameter(description = "Group UUID") @PathVariable UUID groupId) {
    userService.addToGroup(userId, groupId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Removes a user from a group.
   *
   * @param userId the user UUID
   * @param groupId the group UUID
   * @return no content
   */
  @Operation(summary = "Remove user from group", description = "Removes a user from a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User removed from group successfully"),
        @ApiResponse(responseCode = "404", description = "User or group not found")
      })
  @DeleteMapping("/{userId}/groups/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeFromGroup(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Parameter(description = "Group UUID") @PathVariable UUID groupId) {
    userService.removeFromGroup(userId, groupId);
    return ResponseEntity.noContent().build();
  }
}
