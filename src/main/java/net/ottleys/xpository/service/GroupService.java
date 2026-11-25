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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.GroupRepository;
import net.ottleys.xpository.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Group management operations.
 *
 * <p>Provides business logic for group CRUD operations, membership management, hierarchy
 * management, and permission inheritance. All operations are transactional and include validation.
 */
@Service
@Transactional
public class GroupService {

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;

  /**
   * Constructs a GroupService with required dependencies.
   *
   * @param groupRepository repository for group data access
   * @param userRepository repository for user data access
   */
  public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
  }

  /**
   * Creates a new group.
   *
   * @param group the group to create
   * @return the created group
   * @throws BusinessValidationException if group name already exists or validation fails
   */
  public Group createGroup(Group group) {
    if (groupRepository.existsByName(group.getName())) {
      throw new BusinessValidationException("Group name already exists: " + group.getName());
    }

    if (group.getParentGroup() != null) {
      Group parent =
          groupRepository
              .findByGroupId(group.getParentGroup().getGroupId())
              .orElseThrow(() ->
                  new BusinessValidationException(
                      "Parent group not found: " + group.getParentGroup().getGroupId()));
      group.setParentGroup(parent);
    }

    return groupRepository.save(group);
  }

  /**
   * Updates an existing group.
   *
   * @param groupId the group UUID
   * @param updatedGroup the updated group data
   * @return the updated group
   * @throws BusinessValidationException if group not found or validation fails
   */
  public Group updateGroup(UUID groupId, Group updatedGroup) {
    Group existingGroup =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    if (existingGroup.isSystemGroup()) {
      throw new BusinessValidationException(
          "Cannot modify system group: " + existingGroup.getName());
    }

    // Check name uniqueness if changed
    if (!existingGroup.getName().equals(updatedGroup.getName())
        && groupRepository.existsByName(updatedGroup.getName())) {
      throw new BusinessValidationException("Group name already exists: " + updatedGroup.getName());
    }

    existingGroup.setName(updatedGroup.getName());
    existingGroup.setDescription(updatedGroup.getDescription());
    existingGroup.setInheritPermissions(updatedGroup.isInheritPermissions());

    return groupRepository.save(existingGroup);
  }

  /**
   * Deletes a group by UUID.
   *
   * @param groupId the group UUID
   * @throws BusinessValidationException if group not found or is a system group
   */
  public void deleteGroup(UUID groupId) {
    Group group =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    if (group.isSystemGroup()) {
      throw new BusinessValidationException("Cannot delete system group: " + group.getName());
    }

    groupRepository.delete(group);
  }

  /**
   * Finds a group by UUID.
   *
   * @param groupId the group UUID
   * @return Optional containing the group if found
   */
  @Transactional(readOnly = true)
  public Optional<Group> findByGroupId(UUID groupId) {
    return groupRepository.findByGroupId(groupId);
  }

  /**
   * Finds a group by name.
   *
   * @param name the group name
   * @return Optional containing the group if found
   */
  @Transactional(readOnly = true)
  public Optional<Group> findByName(String name) {
    return groupRepository.findByName(name);
  }

  /**
   * Finds a group by UUID with members eagerly loaded.
   *
   * @param groupId the group UUID
   * @return Optional containing the group with members
   */
  @Transactional(readOnly = true)
  public Optional<Group> findByGroupIdWithMembers(UUID groupId) {
    return groupRepository.findByGroupIdWithMembers(groupId);
  }

  /**
   * Lists all groups.
   *
   * @return list of all groups
   */
  @Transactional(readOnly = true)
  public List<Group> findAll() {
    return groupRepository.findAll();
  }

  /**
   * Lists all system groups.
   *
   * @return list of system groups
   */
  @Transactional(readOnly = true)
  public List<Group> findSystemGroups() {
    return groupRepository.findBySystemGroupTrue();
  }

  /**
   * Lists all user-created groups.
   *
   * @return list of non-system groups
   */
  @Transactional(readOnly = true)
  public List<Group> findUserGroups() {
    return groupRepository.findBySystemGroupFalse();
  }

  /**
   * Lists all top-level groups (no parent).
   *
   * @return list of top-level groups
   */
  @Transactional(readOnly = true)
  public List<Group> findTopLevelGroups() {
    return groupRepository.findByParentGroupIsNull();
  }

  /**
   * Lists all child groups of a parent.
   *
   * @param parentGroupId the parent group UUID
   * @return list of child groups
   */
  @Transactional(readOnly = true)
  public List<Group> findChildGroups(UUID parentGroupId) {
    return groupRepository.findByParentGroupUuid(parentGroupId);
  }

  /**
   * Finds the "everyone" system group.
   *
   * @return Optional containing the everyone group
   */
  @Transactional(readOnly = true)
  public Optional<Group> findEveryoneGroup() {
    return groupRepository.findEveryoneGroup();
  }

  /**
   * Adds a user to a group.
   *
   * @param groupId the group UUID
   * @param userId the user UUID
   * @throws BusinessValidationException if group or user not found
   */
  public void addMember(UUID groupId, UUID userId) {
    Group group =
        groupRepository
            .findByGroupIdWithMembers(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    group.addMember(user);
    groupRepository.save(group);
  }

  /**
   * Removes a user from a group.
   *
   * @param groupId the group UUID
   * @param userId the user UUID
   * @throws BusinessValidationException if group or user not found
   */
  public void removeMember(UUID groupId, UUID userId) {
    Group group =
        groupRepository
            .findByGroupIdWithMembers(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    group.removeMember(user);
    groupRepository.save(group);
  }

  /**
   * Lists all members of a group.
   *
   * @param groupId the group UUID
   * @return list of users in the group
   * @throws BusinessValidationException if group not found
   */
  @Transactional(readOnly = true)
  public List<User> findGroupMembers(UUID groupId) {
    groupRepository
        .findByGroupId(groupId)
        .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    return userRepository.findByGroupUuid(groupId);
  }

  /**
   * Checks if a user is a member of a group.
   *
   * @param groupId the group UUID
   * @param userId the user UUID
   * @return true if user is a member
   * @throws BusinessValidationException if group or user not found
   */
  @Transactional(readOnly = true)
  public boolean isMember(UUID groupId, UUID userId) {
    Group group =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    return groupRepository.isMember(group.getId(), user.getId());
  }

  /**
   * Counts members in a group.
   *
   * @param groupId the group UUID
   * @return count of members
   * @throws BusinessValidationException if group not found
   */
  @Transactional(readOnly = true)
  public long countMembers(UUID groupId) {
    Group group =
        groupRepository
            .findByGroupId(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    return groupRepository.countMembers(group.getId());
  }

  /**
   * Searches groups by name.
   *
   * @param searchTerm the search term
   * @return list of matching groups
   */
  @Transactional(readOnly = true)
  public List<Group> searchGroups(String searchTerm) {
    return groupRepository.findByNameContainingIgnoreCase(searchTerm);
  }
}
