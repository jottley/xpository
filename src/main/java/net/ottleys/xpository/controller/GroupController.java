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
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.dto.CreateGroupRequestDto;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.GroupResponseDto;
import net.ottleys.xpository.dto.UpdateGroupRequestDto;
import net.ottleys.xpository.dto.UserResponseDto;
import net.ottleys.xpository.mapper.GroupMapper;
import net.ottleys.xpository.mapper.UserMapper;
import net.ottleys.xpository.service.GroupService;
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
 * REST API controller for Group resources.
 *
 * <p>Provides CRUD operations for groups following REST conventions. All endpoints are under
 * /api/v1/groups. Requires ADMIN role for most operations.
 */
@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "APIs for managing groups")
public class GroupController {

  private final GroupService groupService;
  private final GroupMapper groupMapper;
  private final UserMapper userMapper;

  /**
   * Constructs a GroupController with required dependencies.
   *
   * @param groupService service for group business logic
   * @param groupMapper mapper for group entity/DTO conversion
   * @param userMapper mapper for user entity/DTO conversion
   */
  public GroupController(
      GroupService groupService, GroupMapper groupMapper, UserMapper userMapper) {
    this.groupService = groupService;
    this.groupMapper = groupMapper;
    this.userMapper = userMapper;
  }

  /**
   * Creates a new group.
   *
   * @param request the group creation request
   * @return the created group
   */
  @Operation(summary = "Create a new group", description = "Creates a new group")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Group created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Group name already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GroupResponseDto> createGroup(
      @Valid @RequestBody CreateGroupRequestDto request) {
    Group group = groupMapper.toEntity(request);

    // Set parent group if provided
    if (request.parentGroupId() != null) {
      Group parentGroup =
          groupService
              .findByGroupId(request.parentGroupId())
              .orElseThrow(() ->
                  new IllegalArgumentException(
                      "Parent group not found: " + request.parentGroupId()));
      group.setParentGroup(parentGroup);
    }

    Group createdGroup = groupService.createGroup(group);
    long memberCount = groupService.countMembers(createdGroup.getGroupId());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(groupMapper.toResponseDto(createdGroup, memberCount));
  }

  /**
   * Gets a group by UUID.
   *
   * @param groupId the group UUID
   * @return the group
   */
  @Operation(summary = "Get group by ID", description = "Retrieves a group by its UUID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Group found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Group not found")
      })
  @GetMapping("/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GroupResponseDto> getGroup(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId) {
    return groupService
        .findByGroupId(groupId)
        .map(group -> {
          long memberCount = groupService.countMembers(groupId);
          return groupMapper.toResponseDto(group, memberCount);
        })
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Lists all groups.
   *
   * @return list of all groups
   */
  @Operation(summary = "List all groups", description = "Retrieves a list of all groups")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Groups retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<GroupResponseDto>> listGroups() {
    List<GroupResponseDto> groups =
        groupService.findAll().stream()
            .map(group -> {
              long memberCount = groupService.countMembers(group.getGroupId());
              return groupMapper.toResponseDto(group, memberCount);
            })
            .toList();
    return ResponseEntity.ok(groups);
  }

  /**
   * Searches groups by term.
   *
   * @param searchTerm the search term
   * @return list of matching groups
   */
  @Operation(summary = "Search groups", description = "Searches groups by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/search")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<GroupResponseDto>> searchGroups(
      @Parameter(description = "Search term") @RequestParam String searchTerm) {
    List<GroupResponseDto> groups =
        groupService.searchGroups(searchTerm).stream().map(groupMapper::toResponseDto).toList();
    return ResponseEntity.ok(groups);
  }

  /**
   * Updates a group.
   *
   * @param groupId the group UUID
   * @param request the update request
   * @return the updated group
   */
  @Operation(summary = "Update group", description = "Updates an existing group")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Group updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GroupResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or cannot modify system group",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PatchMapping("/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<GroupResponseDto> updateGroup(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId,
      @Valid @RequestBody UpdateGroupRequestDto request) {
    Group updates = groupMapper.toEntity(request);
    Group updatedGroup = groupService.updateGroup(groupId, updates);
    long memberCount = groupService.countMembers(groupId);
    return ResponseEntity.ok(groupMapper.toResponseDto(updatedGroup, memberCount));
  }

  /**
   * Deletes a group.
   *
   * @param groupId the group UUID
   * @return no content
   */
  @Operation(summary = "Delete group", description = "Deletes a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Group deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Group not found"),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot delete system group",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteGroup(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId) {
    groupService.deleteGroup(groupId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all members of a group.
   *
   * @param groupId the group UUID
   * @return list of users
   */
  @Operation(summary = "Get group members", description = "Retrieves all members of a group")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Members retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Group not found")
      })
  @GetMapping("/{groupId}/members")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponseDto>> getGroupMembers(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId) {
    List<UserResponseDto> members =
        groupService.findGroupMembers(groupId).stream().map(userMapper::toResponseDto).toList();
    return ResponseEntity.ok(members);
  }

  /**
   * Adds a member to a group.
   *
   * @param groupId the group UUID
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Add member to group", description = "Adds a user to a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User added to group successfully"),
        @ApiResponse(responseCode = "404", description = "Group or user not found")
      })
  @PostMapping("/{groupId}/members/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> addMember(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId,
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    groupService.addMember(groupId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Removes a member from a group.
   *
   * @param groupId the group UUID
   * @param userId the user UUID
   * @return no content
   */
  @Operation(summary = "Remove member from group", description = "Removes a user from a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User removed from group successfully"),
        @ApiResponse(responseCode = "404", description = "Group or user not found")
      })
  @DeleteMapping("/{groupId}/members/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> removeMember(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId,
      @Parameter(description = "User UUID") @PathVariable UUID userId) {
    groupService.removeMember(groupId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all child groups.
   *
   * @param groupId the parent group UUID
   * @return list of child groups
   */
  @Operation(summary = "Get child groups", description = "Retrieves all child groups of a parent")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Child groups retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/{groupId}/children")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<GroupResponseDto>> getChildGroups(
      @Parameter(description = "Parent group UUID") @PathVariable UUID groupId) {
    List<GroupResponseDto> children =
        groupService.findChildGroups(groupId).stream().map(groupMapper::toResponseDto).toList();
    return ResponseEntity.ok(children);
  }
}
