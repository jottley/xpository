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
package net.ottleys.xpository.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Group operations.
 *
 * <p>Provides CRUD operations and custom queries for group management, including lookups by UUID,
 * name, parent group queries, and member management.
 */
public interface GroupRepository extends JpaRepository<Group, Long> {

  /**
   * Finds a group by its public UUID.
   *
   * @param groupId the group UUID
   * @return Optional containing the group if found
   */
  Optional<Group> findByGroupId(UUID groupId);

  /**
   * Finds a group by name.
   *
   * @param name the group name
   * @return Optional containing the group if found
   */
  Optional<Group> findByName(String name);

  /**
   * Checks if a group exists with the given name.
   *
   * @param name the group name
   * @return true if group exists
   */
  boolean existsByName(String name);

  /**
   * Finds all system groups.
   *
   * @return list of system groups
   */
  List<Group> findBySystemGroupTrue();

  /**
   * Finds all non-system groups.
   *
   * @return list of non-system groups
   */
  List<Group> findBySystemGroupFalse();

  /**
   * Finds all top-level groups (no parent).
   *
   * @return list of top-level groups
   */
  List<Group> findByParentGroupIsNull();

  /**
   * Finds all child groups of a specific parent.
   *
   * @param parentGroup the parent group
   * @return list of child groups
   */
  List<Group> findByParentGroup(Group parentGroup);

  /**
   * Finds all child groups by parent UUID.
   *
   * @param parentGroupId the parent group UUID
   * @return list of child groups
   */
  @Query("SELECT g FROM Group g WHERE g.parentGroup.groupId = :parentGroupId")
  List<Group> findByParentGroupUuid(@Param("parentGroupId") UUID parentGroupId);

  /**
   * Finds a group by UUID with members eagerly loaded.
   *
   * @param groupId the group UUID
   * @return Optional containing the group with members
   */
  @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.groupId = :groupId")
  Optional<Group> findByGroupIdWithMembers(@Param("groupId") UUID groupId);

  /**
   * Finds a group by name with members eagerly loaded.
   *
   * @param name the group name
   * @return Optional containing the group with members
   */
  @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.name = :name")
  Optional<Group> findByNameWithMembers(@Param("name") String name);

  /**
   * Finds all groups a user belongs to by user ID.
   *
   * @param userId the user ID
   * @return list of groups
   */
  @Query("SELECT g FROM Group g JOIN g.members u WHERE u.id = :userId")
  List<Group> findByUserId(@Param("userId") Long userId);

  /**
   * Finds all groups a user belongs to by user UUID.
   *
   * @param userId the user UUID
   * @return list of groups
   */
  @Query("SELECT g FROM Group g JOIN g.members u WHERE u.userId = :userId")
  List<Group> findByUserUuid(@Param("userId") UUID userId);

  /**
   * Finds groups by name containing the search term (case-insensitive).
   *
   * @param name the search term
   * @return list of matching groups
   */
  List<Group> findByNameContainingIgnoreCase(String name);

  /**
   * Finds the "everyone" system group.
   *
   * @return Optional containing the everyone group if found
   */
  @Query("SELECT g FROM Group g WHERE g.systemGroup = true AND g.name = 'everyone'")
  Optional<Group> findEveryoneGroup();

  /**
   * Checks if a user is a member of a specific group.
   *
   * @param groupId the group ID
   * @param userId the user ID
   * @return true if user is a member
   */
  @Query(
      "SELECT COUNT(g) > 0 FROM Group g JOIN g.members u WHERE g.id = :groupId AND u.id = :userId")
  boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

  /**
   * Counts the number of members in a group.
   *
   * @param groupId the group ID
   * @return count of members
   */
  @Query("SELECT COUNT(u) FROM Group g JOIN g.members u WHERE g.id = :groupId")
  long countMembers(@Param("groupId") Long groupId);

  /**
   * Finds all groups that inherit permissions.
   *
   * @return list of groups with inheritance enabled
   */
  List<Group> findByInheritPermissionsTrue();

  /**
   * Finds all descendant groups (recursive).
   *
   * <p>Returns all groups that are children, grandchildren, etc. of the specified parent.
   *
   * @param parentGroupId the parent group ID
   * @return list of all descendant groups
   */
  @Query(
      value =
          "WITH RECURSIVE group_tree AS ("
              + "  SELECT id, group_id, name, parent_group_id FROM groups WHERE parent_group_id = :parentGroupId"
              + "  UNION ALL"
              + "  SELECT g.id, g.group_id, g.name, g.parent_group_id FROM groups g"
              + "  INNER JOIN group_tree gt ON g.parent_group_id = gt.id"
              + ") SELECT * FROM group_tree",
      nativeQuery = true)
  List<Group> findDescendants(@Param("parentGroupId") Long parentGroupId);
}
