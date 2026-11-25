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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.repository.GroupRepository;
import net.ottleys.xpository.repository.UserRepository;

/**
 * Bootstrap service for security system initialization.
 *
 * <p>Creates default system groups and admin user on application startup if they do not already
 * exist. Ensures that the security system has the minimum required configuration to function.
 *
 * <p>Default system groups: - "everyone" - Read-only group that conceptually includes all users -
 * "administrators" - Administrative group with full system access
 *
 * <p>Default admin user (dev/test profiles only): - username: admin - password: admin - member of
 * admin group
 */
@Service
public class SecurityBootstrapService {

  private static final Logger logger = LoggerFactory.getLogger(SecurityBootstrapService.class);

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  /**
   * Constructs a SecurityBootstrapService.
   *
   * @param groupRepository repository for group data access
   * @param userRepository repository for user data access
   * @param userService service for user operations
   */
  public SecurityBootstrapService(
      GroupRepository groupRepository, UserRepository userRepository, UserService userService) {
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.userService = userService;
  }

  /**
   * Initializes security system on application startup.
   *
   * <p>Creates system groups and default admin user if they don't exist. Runs after application is
   * fully initialized and ready to accept requests.
   */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeSecurity() {
    logger.info("Initializing security system...");

    createEveryoneGroup();
    createAdminGroup();
    createDefaultAdminUser();

    logger.info("Security system initialization complete");
  }

  /**
   * Creates the "everyone" system group if it doesn't exist.
   *
   * <p>The everyone group is a special read-only group that conceptually includes all users. It is
   * used for public access permissions.
   */
  private void createEveryoneGroup() {
    if (groupRepository.findByName("everyone").isEmpty()) {
      Group everyoneGroup = new Group();
      everyoneGroup.setName("everyone");
      everyoneGroup.setDescription(
          "Read-only system group that conceptually includes all users for public access");
      everyoneGroup.setSystemGroup(true);
      everyoneGroup.setInheritPermissions(false);
      groupRepository.save(everyoneGroup);
      logger.info("Created 'everyone' system group");
    } else {
      logger.debug("'everyone' system group already exists");
    }
  }

  /**
   * Creates the "administrators" system group if it doesn't exist.
   *
   * <p>The admin group is for administrative users with full system access.
   */
  private void createAdminGroup() {
    if (groupRepository.findByName("administrators").isEmpty()) {
      Group adminGroup = new Group();
      adminGroup.setName("administrators");
      adminGroup.setDescription("Administrative group with full system access");
      adminGroup.setSystemGroup(true);
      adminGroup.setInheritPermissions(false);
      groupRepository.save(adminGroup);
      logger.info("Created 'administrators' system group");
    } else {
      logger.debug("'administrators' system group already exists");
    }
  }

  /**
   * Creates a default admin user for development and testing.
   *
   * <p>Only creates the user if no users exist in the system. This is a convenience for initial
   * setup. In production, users should be created through proper administrative processes.
   *
   * <p>Default credentials: - username: admin - password: admin - email: admin@xpository.local
   */
  private void createDefaultAdminUser() {
    if (userRepository.count() == 0) {
      User adminUser = new User();
      adminUser.setUsername("admin");
      adminUser.setEmail("admin@xpository.local");
      adminUser.setDisplayName("System Administrator");
      adminUser.setEnabled(true);

      User createdAdminUser = userService.createUser(adminUser, "admin");

      // Add to admin group
      groupRepository
          .findByName("administrators")
          .ifPresent(adminGroup -> {
            createdAdminUser.addGroup(adminGroup);
            userRepository.save(createdAdminUser);
          });

      logger.warn(
          "Created default admin user (username: admin, password: admin) - CHANGE PASSWORD IN PRODUCTION");
    } else {
      logger.debug("Users already exist, skipping default admin user creation");
    }
  }
}
