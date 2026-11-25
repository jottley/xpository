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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.ottleys.xpository.domain.User;

/**
 * Repository for User operations.
 *
 * <p>Provides CRUD operations and custom queries for user management, including lookups by UUID,
 * username, email, and group membership queries.
 */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by their public UUID.
   *
   * @param userId the user UUID
   * @return Optional containing the user if found
   */
  Optional<User> findByUserId(UUID userId);

  /**
   * Finds a user by username.
   *
   * @param username the username
   * @return Optional containing the user if found
   */
  Optional<User> findByUsername(String username);

  /**
   * Finds a user by email address.
   *
   * @param email the email address
   * @return Optional containing the user if found
   */
  Optional<User> findByEmail(String email);

  /**
   * Checks if a user exists with the given username.
   *
   * TODO: This method needs to check for case-insensitive username existence as well.
   *
   * @param username the username
   * @return true if user exists
   */
  boolean existsByUsername(String username);

  /**
   * Checks if a user exists with the given email.
   *
   * @param email the email address
   * @return true if user exists
   */
  boolean existsByEmail(String email);

  /**
   * Finds all enabled users.
   *
   * @return list of enabled users
   */
  List<User> findByEnabledTrue();

  /**
   * Finds all disabled users.
   *
   * @return list of disabled users
   */
  List<User> findByEnabledFalse();

  /**
   * Finds all users who are members of a specific group.
   *
   * @param groupId the group ID
   * @return list of users in the group
   */
  @Query("SELECT u FROM User u JOIN u.groups g WHERE g.id = :groupId")
  List<User> findByGroupId(@Param("groupId") Long groupId);

  /**
   * Finds all users who are members of a specific group by group UUID.
   *
   * @param groupId the group UUID
   * @return list of users in the group
   */
  @Query("SELECT u FROM User u JOIN u.groups g WHERE g.groupId = :groupId")
  List<User> findByGroupUuid(@Param("groupId") UUID groupId);

  /**
   * Finds users by username containing the search term (case-insensitive).
   *
   * @param username the search term
   * @return list of matching users
   */
  List<User> findByUsernameContainingIgnoreCase(String username);

  /**
   * Finds users by email containing the search term (case-insensitive).
   *
   * @param email the search term
   * @return list of matching users
   */
  List<User> findByEmailContainingIgnoreCase(String email);

  /**
   * Finds users by display name containing the search term (case-insensitive).
   *
   * @param displayName the search term
   * @return list of matching users
   */
  List<User> findByDisplayNameContainingIgnoreCase(String displayName);

  /**
   * Finds a user by username with groups eagerly loaded.
   *
   * @param username the username
   * @return Optional containing the user with groups
   */
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.groups WHERE u.username = :username")
  Optional<User> findByUsernameWithGroups(@Param("username") String username);

  /**
   * Finds a user by UUID with groups eagerly loaded.
   *
   * @param userId the user UUID
   * @return Optional containing the user with groups
   */
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.groups WHERE u.userId = :userId")
  Optional<User> findByUserIdWithGroups(@Param("userId") UUID userId);

  /**
   * Counts the number of enabled users.
   *
   * @return count of enabled users
   */
  long countByEnabledTrue();

  /**
   * Counts the number of users in a specific group.
   *
   * @param groupId the group ID
   * @return count of users
   */
  @Query("SELECT COUNT(u) FROM User u JOIN u.groups g WHERE g.id = :groupId")
  long countByGroupId(@Param("groupId") Long groupId);
}
