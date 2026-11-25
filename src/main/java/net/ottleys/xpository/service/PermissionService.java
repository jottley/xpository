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
package net.ottleys.xpository.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.Permission;
import net.ottleys.xpository.domain.Permission.Action;
import net.ottleys.xpository.domain.Permission.ResourceType;
import net.ottleys.xpository.domain.Permission.SubjectType;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.GroupRepository;
import net.ottleys.xpository.repository.PermissionRepository;
import net.ottleys.xpository.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Permission management operations.
 *
 * <p>Provides business logic for permission checks, grants, revokes, and inheritance. Supports
 * permission evaluation for users, groups, and "everyone" access. All operations are transactional
 * and include validation.
 */
@Service
@Transactional
public class PermissionService {

  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final GroupRepository groupRepository;
  private final CollectionRepository collectionRepository;

  /**
   * Constructs a PermissionService with required dependencies.
   *
   * @param permissionRepository repository for permission data access
   * @param userRepository repository for user data access
   * @param groupRepository repository for group data access
   * @param collectionRepository repository for collection data access
   */
  public PermissionService(
      PermissionRepository permissionRepository,
      UserRepository userRepository,
      GroupRepository groupRepository,
      CollectionRepository collectionRepository) {
    this.permissionRepository = permissionRepository;
    this.userRepository = userRepository;
    this.groupRepository = groupRepository;
    this.collectionRepository = collectionRepository;
  }

  /**
   * Grants a permission to a subject.
   *
   * @param subjectId the subject identifier (user UUID, group UUID, or "everyone")
   * @param subjectType the subject type
   * @param resourceId the resource identifier
   * @param resourceType the resource type
   * @param action the action to grant
   * @return the created permission
   * @throws BusinessValidationException if permission already exists
   */
  public Permission grantPermission(
      String subjectId,
      SubjectType subjectType,
      String resourceId,
      ResourceType resourceType,
      Action action) {

    if (permissionRepository.existsBySubjectIdAndResourceIdAndAction(
        subjectId, resourceId, action)) {
      throw new BusinessValidationException(
          "Permission already exists: " + subjectId + " -> " + resourceId + " : " + action);
    }

    Permission permission =
        new Permission(subjectId, subjectType, resourceId, resourceType, action);
    return permissionRepository.save(permission);
  }

  /**
   * Revokes a permission from a subject.
   *
   * @param subjectId the subject identifier
   * @param resourceId the resource identifier
   * @param action the action to revoke
   * @throws BusinessValidationException if permission not found
   */
  public void revokePermission(String subjectId, String resourceId, Action action) {
    Permission permission =
        permissionRepository
            .findBySubjectIdAndResourceIdAndAction(subjectId, resourceId, action)
            .orElseThrow(() ->
                new BusinessValidationException(
                    "Permission not found: " + subjectId + " -> " + resourceId + " : " + action));

    permissionRepository.delete(permission);
  }

  /**
   * Checks if a user has a specific permission on a resource.
   *
   * <p>Evaluates permissions from: 1. Direct user permissions 2. Group permissions (all groups the
   * user belongs to) 3. "Everyone" permissions.
   *
   * <p>If the resourceId corresponds to a Collection, this method automatically checks the collection
   * permission inheritance hierarchy.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @param action the action
   * @return true if user has permission
   */
  @Transactional(readOnly = true)
  public boolean hasPermission(UUID userId, String resourceId, Action action) {
    UUID collectionUuid = tryParseUuid(resourceId);
    if (collectionUuid != null
        && collectionRepository.findByCollectionId(collectionUuid).isPresent()) {
      return hasCollectionPermission(userId, collectionUuid, action);
    }

    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    String userUuid = user.getUserId().toString();
    List<String> groupUuids =
        user.getGroups().stream().map(g -> g.getGroupId().toString()).collect(Collectors.toList());

    // Handle empty group list for query
    if (groupUuids.isEmpty()) {
      groupUuids.add("__no_groups__"); // Placeholder to prevent SQL errors
    }

    return permissionRepository.hasPermission(userUuid, groupUuids, resourceId, action);
  }

  /**
   * Checks if a user has permission on a collection, evaluating the collection permission
   * hierarchy if inheritPermissions is enabled.
   *
   * @param userId the user UUID
   * @param collectionId the collection UUID
   * @param action the action
   * @return true if user has permission directly or through inheritance
   */
  @Transactional(readOnly = true)
  public boolean hasCollectionPermission(UUID userId, UUID collectionId, Action action) {
    List<String> hierarchy = getCollectionHierarchy(collectionId);
    return hasPermissionOnCollectionHierarchy(userId, hierarchy, action);
  }

  /**
   * Finds all effective permissions for a user on a resource.
   *
   * <p>If the resourceId corresponds to a Collection, this method automatically includes inherited
   * permissions from ancestor collections, marked with {@code inherited = true}.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findPermissionsOnResource(UUID userId, String resourceId) {
    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    String userUuid = user.getUserId().toString();
    List<String> groupUuids =
        user.getGroups().stream().map(g -> g.getGroupId().toString()).collect(Collectors.toList());

    if (groupUuids.isEmpty()) {
      groupUuids.add("__no_groups__");
    }

    UUID collectionUuid = tryParseUuid(resourceId);
    if (collectionUuid != null
        && collectionRepository.findByCollectionId(collectionUuid).isPresent()) {
      return findEffectivePermissionsOnCollection(userUuid, groupUuids, collectionUuid);
    }

    return permissionRepository.findPermissionsOnResource(userUuid, groupUuids, resourceId);
  }

  /**
   * Finds all effective permissions for a user on a collection, including inherited permissions
   * from ancestor collections.
   *
   * <p>Permissions from ancestor collections will have {@code inherited = true}, while permissions
   * directly granted on the collection will have {@code inherited = false}.
   *
   * @param userId the user UUID
   * @param collectionId the collection UUID
   * @return list of effective permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findEffectivePermissionsOnCollection(UUID userId, UUID collectionId) {
    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    String userUuid = user.getUserId().toString();
    List<String> groupUuids =
        user.getGroups().stream().map(g -> g.getGroupId().toString()).collect(Collectors.toList());

    if (groupUuids.isEmpty()) {
      groupUuids.add("__no_groups__");
    }

    return findEffectivePermissionsOnCollection(userUuid, groupUuids, collectionId);
  }

  private List<Permission> findEffectivePermissionsOnCollection(
      String userUuid, List<String> groupUuids, UUID collectionId) {
    List<String> hierarchy = getCollectionHierarchy(collectionId);
    List<Permission> rawPermissions =
        permissionRepository.findPermissionsOnCollectionHierarchy(userUuid, groupUuids, hierarchy);

    String targetResourceId = collectionId.toString();
    List<Permission> effectivePermissions = new java.util.ArrayList<>();

    for (Permission p : rawPermissions) {
      boolean isInherited = !p.getResourceId().equals(targetResourceId);
      Permission effective =
          new Permission(
              p.getSubjectId(),
              p.getSubjectType(),
              p.getResourceId(),
              p.getResourceType(),
              p.getAction());
      effective.setId(p.getId());
      effective.setInherited(isInherited);
      effective.setCreatedDate(p.getCreatedDate());
      effective.setCreatedBy(p.getCreatedBy());
      effectivePermissions.add(effective);
    }

    effectivePermissions.sort(
        java.util.Comparator.comparingInt(p -> hierarchy.indexOf(p.getResourceId())));

    return effectivePermissions;
  }

  /**
   * Finds all resources a user has a specific permission on.
   *
   * <p>When resourceType is {@link ResourceType#COLLECTION}, this method recursively includes
   * all descendant collections that inherit permissions from the directly permitted collections.
   *
   * @param userId the user UUID
   * @param action the action
   * @param resourceType the resource type
   * @return list of resource IDs
   */
  @Transactional(readOnly = true)
  public List<String> findResourcesWithPermission(
      UUID userId, Action action, ResourceType resourceType) {
    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    String userUuid = user.getUserId().toString();
    List<String> groupUuids =
        user.getGroups().stream().map(g -> g.getGroupId().toString()).collect(Collectors.toList());

    if (groupUuids.isEmpty()) {
      groupUuids.add("__no_groups__");
    }

    List<String> directResourceIds =
        permissionRepository.findResourceIdsWithPermission(
            userUuid, groupUuids, action, resourceType);

    if (resourceType != ResourceType.COLLECTION || directResourceIds.isEmpty()) {
      return directResourceIds;
    }

    Set<String> allCollectionIds = new LinkedHashSet<>(directResourceIds);
    List<UUID> currentLevelUuids =
        directResourceIds.stream()
            .map(this::tryParseUuid)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    int maxDepth = 100;
    int depth = 0;

    while (!currentLevelUuids.isEmpty() && depth < maxDepth) {
      List<Collection> children =
          collectionRepository.findActiveChildrenInheritingPermissions(currentLevelUuids);

      List<UUID> nextLevelUuids = new java.util.ArrayList<>();
      for (Collection child : children) {
        String childIdStr = child.getCollectionId().toString();
        if (allCollectionIds.add(childIdStr)) {
          nextLevelUuids.add(child.getCollectionId());
        }
      }

      currentLevelUuids = nextLevelUuids;
      depth++;
    }

    return new java.util.ArrayList<>(allCollectionIds);
  }

  /**
   * Finds all permissions for a subject.
   *
   * @param subjectId the subject identifier
   * @return list of permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findBySubjectId(String subjectId) {
    return permissionRepository.findBySubjectId(subjectId);
  }

  /**
   * Finds all permissions for a resource.
   *
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findByResourceId(String resourceId) {
    return permissionRepository.findByResourceId(resourceId);
  }

  /**
   * Finds all "everyone" permissions (public access).
   *
   * @return list of everyone permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findEveryonePermissions() {
    return permissionRepository.findEveryonePermissions();
  }

  /**
   * Finds all "everyone" permissions on a specific resource.
   *
   * @param resourceId the resource identifier
   * @return list of everyone permissions
   */
  @Transactional(readOnly = true)
  public List<Permission> findEveryonePermissionsOnResource(String resourceId) {
    return permissionRepository.findEveryonePermissionsOnResource(resourceId);
  }

  /**
   * Finds a specific permission.
   *
   * @param subjectId the subject identifier
   * @param resourceId the resource identifier
   * @param action the action
   * @return Optional containing the permission if found
   */
  @Transactional(readOnly = true)
  public Optional<Permission> findPermission(String subjectId, String resourceId, Action action) {
    return permissionRepository.findBySubjectIdAndResourceIdAndAction(
        subjectId, resourceId, action);
  }

  /**
   * Deletes all permissions for a subject.
   *
   * @param subjectId the subject identifier
   */
  public void deleteBySubjectId(String subjectId) {
    permissionRepository.deleteBySubjectId(subjectId);
  }

  /**
   * Deletes all permissions for a resource.
   *
   * @param resourceId the resource identifier
   */
  public void deleteByResourceId(String resourceId) {
    permissionRepository.deleteByResourceId(resourceId);
  }

  /**
   * Grants a permission to a user.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @param resourceType the resource type
   * @param action the action
   * @return the created permission
   */
  public Permission grantToUser(
      UUID userId, String resourceId, ResourceType resourceType, Action action) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    return grantPermission(
        user.getUserId().toString(), SubjectType.USER, resourceId, resourceType, action);
  }

  /**
   * Grants a permission to a group.
   *
   * @param groupId the group UUID
   * @param resourceId the resource identifier
   * @param resourceType the resource type
   * @param action the action
   * @return the created permission
   */
  public Permission grantToGroup(
      UUID groupId, String resourceId, ResourceType resourceType, Action action) {
    Group group =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    return grantPermission(
        group.getGroupId().toString(), SubjectType.GROUP, resourceId, resourceType, action);
  }

  /**
   * Grants a permission to everyone (public access).
   *
   * @param resourceId the resource identifier
   * @param resourceType the resource type
   * @param action the action
   * @return the created permission
   */
  public Permission grantToEveryone(String resourceId, ResourceType resourceType, Action action) {
    return grantPermission("everyone", SubjectType.EVERYONE, resourceId, resourceType, action);
  }

  /**
   * Revokes a permission from a user.
   *
   * @param userId the user UUID
   * @param resourceId the resource identifier
   * @param action the action
   */
  public void revokeFromUser(UUID userId, String resourceId, Action action) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    revokePermission(user.getUserId().toString(), resourceId, action);
  }

  /**
   * Revokes a permission from a group.
   *
   * @param groupId the group UUID
   * @param resourceId the resource identifier
   * @param action the action
   */
  public void revokeFromGroup(UUID groupId, String resourceId, Action action) {
    Group group =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    revokePermission(group.getGroupId().toString(), resourceId, action);
  }

  /**
   * Revokes a permission from everyone.
   *
   * @param resourceId the resource identifier
   * @param action the action
   */
  public void revokeFromEveryone(String resourceId, Action action) {
    revokePermission("everyone", resourceId, action);
  }

  /**
   * Counts permissions for a subject.
   *
   * @param subjectId the subject identifier
   * @return count of permissions
   */
  @Transactional(readOnly = true)
  public long countBySubjectId(String subjectId) {
    return permissionRepository.countBySubjectId(subjectId);
  }

  /**
   * Counts permissions for a resource.
   *
   * @param resourceId the resource identifier
   * @return count of permissions
   */
  @Transactional(readOnly = true)
  public long countByResourceId(String resourceId) {
    return permissionRepository.countByResourceId(resourceId);
  }

  /**
   * Checks if a user has permission on a collection considering the collection hierarchy.
   *
   * <p>This method evaluates permissions across the collection inheritance chain. If the collection
   * has inheritPermissions=true, it checks permissions on the collection and all its parent
   * collections up to the root. This allows child collections to inherit permissions from their
   * parents.
   *
   * <p>The collectionHierarchy parameter should be obtained from {@link
   * CollectionService#getCollectionHierarchyForPermissions(UUID)}.
   *
   * @param userId the user UUID
   * @param collectionHierarchy list of collection UUIDs (current → parent → ... → root)
   * @param action the action
   * @return true if user has permission on any collection in the hierarchy
   */
  @Transactional(readOnly = true)
  public boolean hasPermissionOnCollectionHierarchy(
      UUID userId, List<String> collectionHierarchy, Action action) {
    if (collectionHierarchy == null || collectionHierarchy.isEmpty()) {
      return false;
    }

    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    String userUuid = user.getUserId().toString();
    List<String> groupUuids =
        user.getGroups().stream().map(g -> g.getGroupId().toString()).collect(Collectors.toList());

    // Handle empty group list for query
    if (groupUuids.isEmpty()) {
      groupUuids.add("__no_groups__"); // Placeholder to prevent SQL errors
    }

    return permissionRepository.hasPermissionOnCollectionHierarchy(
        userUuid, groupUuids, collectionHierarchy, action);
  }

  /**
   * Resolves the collection hierarchy for permission inheritance.
   *
   * @param collectionId starting collection UUID
   * @return list of collection UUID strings from current collection up to root
   */
  @Transactional(readOnly = true)
  public List<String> getCollectionHierarchy(UUID collectionId) {
    Optional<Collection> optionalCollection = collectionRepository.findByCollectionId(collectionId);
    if (optionalCollection.isEmpty()) {
      return List.of(collectionId.toString());
    }

    Collection collection = optionalCollection.get();
    List<String> hierarchy = new java.util.ArrayList<>();
    hierarchy.add(collection.getCollectionId().toString());

    if (!collection.isInheritPermissions()) {
      return hierarchy;
    }

    Collection current = collection;
    int maxDepth = 100;
    int depth = 0;

    while (current.getParentCollection() != null && depth < maxDepth) {
      current = current.getParentCollection();
      if (current.isDeleted()) {
        break;
      }
      hierarchy.add(current.getCollectionId().toString());

      if (!current.isInheritPermissions()) {
        break;
      }

      depth++;
    }

    return hierarchy;
  }

  private UUID tryParseUuid(String string) {
    if (string == null || string.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(string);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
