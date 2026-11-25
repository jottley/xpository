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
import net.ottleys.xpository.domain.Permission;
import net.ottleys.xpository.domain.Permission.Action;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.GrantPermissionRequestDto;
import net.ottleys.xpository.dto.PermissionResponseDto;
import net.ottleys.xpository.mapper.PermissionMapper;
import net.ottleys.xpository.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for Permission resources.
 *
 * <p>Provides operations for managing permissions. All endpoints are under /api/v1/permissions.
 * Requires ADMIN role for all operations.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "APIs for managing permissions")
public class PermissionController {

  private final PermissionService permissionService;
  private final PermissionMapper permissionMapper;

  /**
   * Constructs a PermissionController with required dependencies.
   *
   * @param permissionService service for permission business logic
   * @param permissionMapper mapper for permission entity/DTO conversion
   */
  public PermissionController(
      PermissionService permissionService, PermissionMapper permissionMapper) {
    this.permissionService = permissionService;
    this.permissionMapper = permissionMapper;
  }

  /**
   * Grants a permission to a user.
   *
   * @param userId the user UUID
   * @param request the grant permission request
   * @return the created permission
   */
  @Operation(summary = "Grant permission to user", description = "Grants a permission to a user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Permission granted successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PermissionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Permission already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/users/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PermissionResponseDto> grantToUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Valid @RequestBody GrantPermissionRequestDto request) {
    Permission permission =
        permissionService.grantToUser(
            userId, request.resourceId(), request.resourceType(), request.action());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(permissionMapper.toResponseDto(permission));
  }

  /**
   * Grants a permission to a group.
   *
   * @param groupId the group UUID
   * @param request the grant permission request
   * @return the created permission
   */
  @Operation(summary = "Grant permission to group", description = "Grants a permission to a group")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Permission granted successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PermissionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Permission already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/groups/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PermissionResponseDto> grantToGroup(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId,
      @Valid @RequestBody GrantPermissionRequestDto request) {
    Permission permission =
        permissionService.grantToGroup(
            groupId, request.resourceId(), request.resourceType(), request.action());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(permissionMapper.toResponseDto(permission));
  }

  /**
   * Grants a permission to everyone (public access).
   *
   * @param request the grant permission request
   * @return the created permission
   */
  @Operation(
      summary = "Grant permission to everyone",
      description = "Grants a permission to everyone (public access)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Permission granted successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PermissionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Permission already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/everyone")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PermissionResponseDto> grantToEveryone(
      @Valid @RequestBody GrantPermissionRequestDto request) {
    Permission permission =
        permissionService.grantToEveryone(
            request.resourceId(), request.resourceType(), request.action());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(permissionMapper.toResponseDto(permission));
  }

  /**
   * Revokes a permission from a user.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @param action the action
   * @return no content
   */
  @Operation(
      summary = "Revoke permission from user",
      description = "Revokes a permission from a user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Permission revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
      })
  @DeleteMapping("/users/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> revokeFromUser(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Parameter(description = "Resource identifier") @RequestParam String resourceId,
      @Parameter(description = "Action") @RequestParam Action action) {
    permissionService.revokeFromUser(userId, resourceId, action);
    return ResponseEntity.noContent().build();
  }

  /**
   * Revokes a permission from a group.
   *
   * @param groupId the group UUID
   * @param resourceId the resource identifier
   * @param action the action
   * @return no content
   */
  @Operation(
      summary = "Revoke permission from group",
      description = "Revokes a permission from a group")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Permission revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
      })
  @DeleteMapping("/groups/{groupId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> revokeFromGroup(
      @Parameter(description = "Group UUID") @PathVariable UUID groupId,
      @Parameter(description = "Resource identifier") @RequestParam String resourceId,
      @Parameter(description = "Action") @RequestParam Action action) {
    permissionService.revokeFromGroup(groupId, resourceId, action);
    return ResponseEntity.noContent().build();
  }

  /**
   * Revokes a permission from everyone.
   *
   * @param resourceId the resource identifier
   * @param action the action
   * @return no content
   */
  @Operation(
      summary = "Revoke permission from everyone",
      description = "Revokes a public permission")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Permission revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found")
      })
  @DeleteMapping("/everyone")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> revokeFromEveryone(
      @Parameter(description = "Resource identifier") @RequestParam String resourceId,
      @Parameter(description = "Action") @RequestParam Action action) {
    permissionService.revokeFromEveryone(resourceId, action);
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists all permissions for a resource.
   *
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  @Operation(
      summary = "Get resource permissions",
      description = "Retrieves all permissions for a resource")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permissions retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/resources/{resourceId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<PermissionResponseDto>> getResourcePermissions(
      @Parameter(description = "Resource identifier") @PathVariable String resourceId) {
    List<PermissionResponseDto> permissions =
        permissionService.findByResourceId(resourceId).stream()
            .map(permissionMapper::toResponseDto)
            .toList();
    return ResponseEntity.ok(permissions);
  }

  /**
   * Checks if a user has a specific permission on a resource.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @param action the action
   * @return true if user has permission, false otherwise
   */
  @Operation(
      summary = "Check user permission",
      description = "Checks if a user has a specific permission on a resource")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permission check completed",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{userId}/check")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
  public ResponseEntity<Boolean> checkUserPermission(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Parameter(description = "Resource identifier") @RequestParam String resourceId,
      @Parameter(description = "Action") @RequestParam Action action) {
    boolean hasPermission = permissionService.hasPermission(userId, resourceId, action);
    return ResponseEntity.ok(hasPermission);
  }

  /**
   * Lists all permissions for a user on a resource.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  @Operation(
      summary = "Get user permissions on resource",
      description = "Retrieves all permissions a user has on a resource")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permissions retrieved successfully",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/users/{userId}/resources/{resourceId}")
  @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
  public ResponseEntity<List<PermissionResponseDto>> getUserPermissionsOnResource(
      @Parameter(description = "User UUID") @PathVariable UUID userId,
      @Parameter(description = "Resource identifier") @PathVariable String resourceId) {
    List<PermissionResponseDto> permissions =
        permissionService.findPermissionsOnResource(userId, resourceId).stream()
            .map(permissionMapper::toResponseDto)
            .toList();
    return ResponseEntity.ok(permissions);
  }
}
