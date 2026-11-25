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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.GroupRepository;
import net.ottleys.xpository.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for User management operations.
 *
 * <p>Provides business logic for user CRUD operations, account management, group membership, and
 * authentication support. All operations are transactional and include validation.
 */
@Service
@Transactional
public class UserService {

  private final UserRepository userRepository;
  private final GroupRepository groupRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Constructs a UserService with required dependencies.
   *
   * @param userRepository repository for user data access
   * @param groupRepository repository for group data access
   * @param passwordEncoder encoder for password hashing
   */
  public UserService(
      UserRepository userRepository,
      GroupRepository groupRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.groupRepository = groupRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Creates a new user.
   *
   * @param user the user to create
   * @param rawPassword the raw password (will be hashed)
   * @return the created user
   * @throws BusinessValidationException if username or email already exists
   */
  public User createUser(User user, String rawPassword) {
    if (userRepository.existsByUsername(user.getUsername())) {
      throw new BusinessValidationException("Username already exists: " + user.getUsername());
    }
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new BusinessValidationException("Email already exists: " + user.getEmail());
    }

    if (rawPassword != null && !rawPassword.isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(rawPassword));
    }

    return userRepository.save(user);
  }

  /**
   * Updates an existing user.
   *
   * @param userId the user UUID
   * @param updatedUser the updated user data
   * @return the updated user
   * @throws BusinessValidationException if user not found or validation fails
   */
  public User updateUser(UUID userId, User updatedUser) {
    User existingUser =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    // Check username uniqueness if changed
    if (!existingUser.getUsername().equals(updatedUser.getUsername())
        && userRepository.existsByUsername(updatedUser.getUsername())) {
      throw new BusinessValidationException(
          "Username already exists: " + updatedUser.getUsername());
    }

    // Check email uniqueness if changed
    if (!existingUser.getEmail().equals(updatedUser.getEmail())
        && userRepository.existsByEmail(updatedUser.getEmail())) {
      throw new BusinessValidationException("Email already exists: " + updatedUser.getEmail());
    }

    existingUser.setUsername(updatedUser.getUsername());
    existingUser.setEmail(updatedUser.getEmail());
    existingUser.setDisplayName(updatedUser.getDisplayName());
    existingUser.setEnabled(updatedUser.isEnabled());
    existingUser.setAccountLocked(updatedUser.isAccountLocked());
    existingUser.setAccountExpired(updatedUser.isAccountExpired());
    existingUser.setCredentialsExpired(updatedUser.isCredentialsExpired());

    return userRepository.save(existingUser);
  }

  /**
   * Updates a user's password.
   *
   * @param userId the user UUID
   * @param rawPassword the new raw password (will be hashed)
   * @throws BusinessValidationException if user not found
   */
  public void updatePassword(UUID userId, String rawPassword) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setCredentialsExpired(false);
    userRepository.save(user);
  }

  /**
   * Deletes a user by UUID.
   *
   * @param userId the user UUID
   * @throws BusinessValidationException if user not found
   */
  public void deleteUser(UUID userId) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    userRepository.delete(user);
  }

  /**
   * Finds a user by UUID.
   *
   * @param userId the user UUID
   * @return Optional containing the user if found
   */
  @Transactional(readOnly = true)
  public Optional<User> findByUserId(UUID userId) {
    return userRepository.findByUserId(userId);
  }

  /**
   * Finds a user by username.
   *
   * @param username the username
   * @return Optional containing the user if found
   */
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  /**
   * Finds a user by username with groups eagerly loaded.
   *
   * @param username the username
   * @return Optional containing the user with groups
   */
  @Transactional(readOnly = true)
  public Optional<User> findByUsernameWithGroups(String username) {
    return userRepository.findByUsernameWithGroups(username);
  }

  /**
   * Finds a user by email.
   *
   * @param email the email address
   * @return Optional containing the user if found
   */
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  /**
   * Lists all users.
   *
   * @return list of all users
   */
  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  /**
   * Lists all enabled users.
   *
   * @return list of enabled users
   */
  @Transactional(readOnly = true)
  public List<User> findEnabledUsers() {
    return userRepository.findByEnabledTrue();
  }

  /**
   * Enables a user account.
   *
   * @param userId the user UUID
   * @throws BusinessValidationException if user not found
   */
  public void enableUser(UUID userId) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    user.setEnabled(true);
    userRepository.save(user);
  }

  /**
   * Disables a user account.
   *
   * @param userId the user UUID
   * @throws BusinessValidationException if user not found
   */
  public void disableUser(UUID userId) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    user.setEnabled(false);
    userRepository.save(user);
  }

  /**
   * Locks a user account.
   *
   * @param userId the user UUID
   * @throws BusinessValidationException if user not found
   */
  public void lockUser(UUID userId) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    user.setAccountLocked(true);
    userRepository.save(user);
  }

  /**
   * Unlocks a user account.
   *
   * @param userId the user UUID
   * @throws BusinessValidationException if user not found
   */
  public void unlockUser(UUID userId) {
    User user =
        userRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    user.setAccountLocked(false);
    userRepository.save(user);
  }

  /**
   * Records a login timestamp for a user.
   *
   * @param username the username
   */
  public void recordLogin(String username) {
    userRepository
        .findByUsername(username)
        .ifPresent(user -> {
          user.setLastLoginDate(LocalDateTime.now());
          userRepository.save(user);
        });
  }

  /**
   * Adds a user to a group.
   *
   * @param userId the user UUID
   * @param groupId the group UUID
   * @throws BusinessValidationException if user or group not found
   */
  public void addToGroup(UUID userId, UUID groupId) {
    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    Group group =
        groupRepository
            .findByGroupIdWithMembers(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    user.addGroup(group);
    userRepository.save(user);
  }

  /**
   * Removes a user from a group.
   *
   * @param userId the user UUID
   * @param groupId the group UUID
   * @throws BusinessValidationException if user or group not found
   */
  public void removeFromGroup(UUID userId, UUID groupId) {
    User user =
        userRepository
            .findByUserIdWithGroups(userId)
            .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    Group group =
        groupRepository
            .findByGroupIdWithMembers(groupId)
            .orElseThrow(() -> new BusinessValidationException("Group not found: " + groupId));

    user.removeGroup(group);
    userRepository.save(user);
  }

  /**
   * Lists all groups a user belongs to.
   *
   * @param userId the user UUID
   * @return list of groups
   * @throws BusinessValidationException if user not found
   */
  @Transactional(readOnly = true)
  public List<Group> findUserGroups(UUID userId) {
    userRepository
        .findByUserId(userId)
        .orElseThrow(() -> new BusinessValidationException("User not found: " + userId));

    return groupRepository.findByUserUuid(userId);
  }

  /**
   * Searches users by username, email, or display name.
   *
   * @param searchTerm the search term
   * @return list of matching users
   */
  @Transactional(readOnly = true)
  public List<User> searchUsers(String searchTerm) {
    List<User> byUsername = userRepository.findByUsernameContainingIgnoreCase(searchTerm);
    List<User> byEmail = userRepository.findByEmailContainingIgnoreCase(searchTerm);
    List<User> byDisplayName = userRepository.findByDisplayNameContainingIgnoreCase(searchTerm);

    return Stream.of(byUsername, byEmail, byDisplayName).flatMap(List::stream).distinct().toList();
  }

  /**
   * Counts all enabled users.
   *
   * @return count of enabled users
   */
  @Transactional(readOnly = true)
  public long countEnabledUsers() {
    return userRepository.countByEnabledTrue();
  }
}
