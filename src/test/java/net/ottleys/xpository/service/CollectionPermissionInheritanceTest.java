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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.Permission;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.GroupRepository;
import net.ottleys.xpository.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for collection permission inheritance.
 *
 * <p>Tests the functionality of permissions being inherited from parent collections to child
 * collections when inheritPermissions=true.
 */
@SpringBootTest
@Transactional
class CollectionPermissionInheritanceTest {

  @Autowired private CollectionService collectionService;

  @Autowired private PermissionService permissionService;

  @Autowired private CollectionRepository collectionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private GroupRepository groupRepository;

  private User testUser;
  private Collection rootCollection;
  private Collection childCollection;
  private Collection grandchildCollection;

  @BeforeEach
  void setUp() {
    // Create test user
    testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPasswordHash("hash");
    testUser.setEnabled(true);
    testUser = userRepository.save(testUser);

    // Create collection hierarchy: root → child → grandchild
    rootCollection = new Collection();
    rootCollection.setName("Root Collection " + UUID.randomUUID());
    rootCollection.setType(CollectionType.LIST);
    rootCollection.setInheritPermissions(true);
    rootCollection.setCreatedBy("test");
    rootCollection.setModifiedBy("test");
    rootCollection = collectionRepository.save(rootCollection);

    childCollection = new Collection();
    childCollection.setName("Child Collection " + UUID.randomUUID());
    childCollection.setType(CollectionType.LIST);
    childCollection.setParentCollection(rootCollection);
    childCollection.setInheritPermissions(true);
    childCollection.setCreatedBy("test");
    childCollection.setModifiedBy("test");
    childCollection = collectionRepository.save(childCollection);

    grandchildCollection = new Collection();
    grandchildCollection.setName("Grandchild Collection " + UUID.randomUUID());
    grandchildCollection.setType(CollectionType.LIST);
    grandchildCollection.setParentCollection(childCollection);
    grandchildCollection.setInheritPermissions(true);
    grandchildCollection.setCreatedBy("test");
    grandchildCollection.setModifiedBy("test");
    grandchildCollection = collectionRepository.save(grandchildCollection);
  }

  @Test
  void getCollectionHierarchy_shouldReturnCollectionOnly_whenNoParent() {
    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(rootCollection.getCollectionId());

    assertThat(hierarchy).hasSize(1);
    assertThat(hierarchy.get(0)).isEqualTo(rootCollection.getCollectionId().toString());
  }

  @Test
  void getCollectionHierarchy_shouldReturnFullChain_withParents() {
    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    assertThat(hierarchy).hasSize(3);
    assertThat(hierarchy.get(0))
        .isEqualTo(grandchildCollection.getCollectionId().toString()); // Current
    assertThat(hierarchy.get(1)).isEqualTo(childCollection.getCollectionId().toString()); // Parent
    assertThat(hierarchy.get(2))
        .isEqualTo(rootCollection.getCollectionId().toString()); // Grandparent
  }

  @Test
  void getCollectionHierarchy_shouldStopAtNonInheritingParent() {
    // Set child to not inherit
    childCollection.setInheritPermissions(false);
    collectionRepository.save(childCollection);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // Should include grandchild and child, but stop at child (which doesn't inherit)
    assertThat(hierarchy).hasSize(2);
    assertThat(hierarchy.get(0)).isEqualTo(grandchildCollection.getCollectionId().toString());
    assertThat(hierarchy.get(1)).isEqualTo(childCollection.getCollectionId().toString());
  }

  @Test
  void getCollectionHierarchy_shouldReturnOnlyItself_whenInheritPermissionsIsFalse() {
    grandchildCollection.setInheritPermissions(false);
    collectionRepository.save(grandchildCollection);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    assertThat(hierarchy).hasSize(1);
    assertThat(hierarchy.get(0)).isEqualTo(grandchildCollection.getCollectionId().toString());
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldInheritFromParent() {
    // Grant READ permission on root collection to user
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    // Get hierarchy for grandchild (should include grandchild → child → root)
    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // User should have READ on grandchild through inheritance from root
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.READ);

    assertThat(hasPermission).isTrue();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldNotInherit_whenPermissionNotGranted() {
    // No permission granted

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.READ);

    assertThat(hasPermission).isFalse();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldNotInherit_whenActionDifferent() {
    // Grant READ permission on root
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // Check for WRITE (not granted)
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.WRITE);

    assertThat(hasPermission).isFalse();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldWorkWithDirectPermission() {
    // Grant permission directly on the grandchild
    permissionService.grantToUser(
        testUser.getUserId(),
        grandchildCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.WRITE);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.WRITE);

    assertThat(hasPermission).isTrue();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldWorkWithGroupPermission() {
    // Create group and add user
    Group group = new Group();
    group.setName("Test Group");
    group = groupRepository.save(group);

    testUser.addGroup(group);
    userRepository.save(testUser);

    // Grant permission to group on root collection
    permissionService.grantToGroup(
        group.getGroupId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // User should have permission through group membership
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.READ);

    assertThat(hasPermission).isTrue();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldWorkWithEveryonePermission() {
    // Grant permission to everyone on root collection
    permissionService.grantToEveryone(
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // Any user should have permission through "everyone"
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.READ);

    assertThat(hasPermission).isTrue();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldStopAtNonInheritingCollection() {
    // Grant permission on root
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    // Set child to not inherit
    childCollection.setInheritPermissions(false);
    collectionRepository.save(childCollection);

    // Hierarchy should stop at child (not include root)
    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // Should NOT have permission because child blocks inheritance
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.READ);

    assertThat(hasPermission).isFalse();
  }

  @Test
  void hasPermissionOnCollectionHierarchy_shouldWorkWithPermissionOnMiddleCollection() {
    // Grant permission on middle (child) collection
    permissionService.grantToUser(
        testUser.getUserId(),
        childCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.WRITE);

    List<String> hierarchy =
        collectionService.getCollectionHierarchyForPermissions(
            grandchildCollection.getCollectionId());

    // Should have permission through parent (child)
    boolean hasPermission =
        permissionService.hasPermissionOnCollectionHierarchy(
            testUser.getUserId(), hierarchy, Permission.Action.WRITE);

    assertThat(hasPermission).isTrue();
  }

  // ─────────────────────────────────────────────────────────────
  // hasCollectionPermission (UUID collectionId overload)
  // ─────────────────────────────────────────────────────────────

  @Test
  void hasCollectionPermission_shouldReturnTrue_whenPermissionGrantedDirectly() {
    permissionService.grantToUser(
        testUser.getUserId(),
        grandchildCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    boolean result =
        permissionService.hasCollectionPermission(
            testUser.getUserId(),
            grandchildCollection.getCollectionId(),
            Permission.Action.READ);

    assertThat(result).isTrue();
  }

  @Test
  void hasCollectionPermission_shouldReturnTrue_whenPermissionGrantedOnAncestor() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    boolean result =
        permissionService.hasCollectionPermission(
            testUser.getUserId(),
            grandchildCollection.getCollectionId(),
            Permission.Action.READ);

    assertThat(result).isTrue();
  }

  @Test
  void hasCollectionPermission_shouldReturnFalse_whenNoPermission() {
    boolean result =
        permissionService.hasCollectionPermission(
            testUser.getUserId(),
            grandchildCollection.getCollectionId(),
            Permission.Action.WRITE);

    assertThat(result).isFalse();
  }

  @Test
  void hasCollectionPermission_shouldReturnFalse_whenInheritanceBlocked() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    // block inheritance at child
    childCollection.setInheritPermissions(false);
    collectionRepository.save(childCollection);

    boolean result =
        permissionService.hasCollectionPermission(
            testUser.getUserId(),
            grandchildCollection.getCollectionId(),
            Permission.Action.READ);

    assertThat(result).isFalse();
  }

  // ─────────────────────────────────────────────────────────────
  // hasPermission (String resourceId overload) – auto collection detection
  // ─────────────────────────────────────────────────────────────

  @Test
  void hasPermission_withCollectionUuidString_shouldUseHierarchy() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    boolean result =
        permissionService.hasPermission(
            testUser.getUserId(),
            grandchildCollection.getCollectionId().toString(),
            Permission.Action.READ);

    assertThat(result).isTrue();
  }

  @Test
  void hasPermission_withNonUuidString_shouldFallBackToDirectCheck() {
    // "system" is not a UUID and no UUID parse → direct permission check
    boolean result =
        permissionService.hasPermission(
            testUser.getUserId(), "system", Permission.Action.ADMIN);

    assertThat(result).isFalse();
  }

  @Test
  void hasPermission_withUnknownUuid_shouldFallBackToDirectCheck() {
    // Valid UUID format but no matching collection in DB → direct permission check
    String unknownUuid = UUID.randomUUID().toString();

    boolean result =
        permissionService.hasPermission(
            testUser.getUserId(), unknownUuid, Permission.Action.READ);

    assertThat(result).isFalse();
  }

  // ─────────────────────────────────────────────────────────────
  // findEffectivePermissionsOnCollection
  // ─────────────────────────────────────────────────────────────

  @Test
  void findEffectivePermissionsOnCollection_shouldMarkDirectPermissionAsNotInherited() {
    permissionService.grantToUser(
        testUser.getUserId(),
        grandchildCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<Permission> permissions =
        permissionService.findEffectivePermissionsOnCollection(
            testUser.getUserId(), grandchildCollection.getCollectionId());

    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0).isInherited()).isFalse();
    assertThat(permissions.get(0).getResourceId())
        .isEqualTo(grandchildCollection.getCollectionId().toString());
  }

  @Test
  void findEffectivePermissionsOnCollection_shouldMarkAncestorPermissionAsInherited() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<Permission> permissions =
        permissionService.findEffectivePermissionsOnCollection(
            testUser.getUserId(), grandchildCollection.getCollectionId());

    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0).isInherited()).isTrue();
    assertThat(permissions.get(0).getResourceId())
        .isEqualTo(rootCollection.getCollectionId().toString());
  }

  @Test
  void findEffectivePermissionsOnCollection_shouldReturnBothDirectAndInheritedPermissions() {
    permissionService.grantToUser(
        testUser.getUserId(),
        grandchildCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.WRITE);

    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<Permission> permissions =
        permissionService.findEffectivePermissionsOnCollection(
            testUser.getUserId(), grandchildCollection.getCollectionId());

    assertThat(permissions).hasSize(2);

    // The directly granted WRITE should appear first (hierarchy position 0 = grandchild)
    Permission direct =
        permissions.stream()
            .filter(p -> p.getAction() == Permission.Action.WRITE)
            .findFirst()
            .orElseThrow();
    assertThat(direct.isInherited()).isFalse();

    Permission inherited =
        permissions.stream()
            .filter(p -> p.getAction() == Permission.Action.READ)
            .findFirst()
            .orElseThrow();
    assertThat(inherited.isInherited()).isTrue();
  }

  @Test
  void findEffectivePermissionsOnCollection_shouldReturnEmpty_whenNoPermissionGranted() {
    List<Permission> permissions =
        permissionService.findEffectivePermissionsOnCollection(
            testUser.getUserId(), grandchildCollection.getCollectionId());

    assertThat(permissions).isEmpty();
  }

  @Test
  void findEffectivePermissionsOnCollection_shouldNotCrossInheritanceBarrier() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    // Block inheritance at child
    childCollection.setInheritPermissions(false);
    collectionRepository.save(childCollection);

    List<Permission> permissions =
        permissionService.findEffectivePermissionsOnCollection(
            testUser.getUserId(), grandchildCollection.getCollectionId());

    // grandchild → child (stops; child.inheritPermissions=false).
    // Root permission is not reachable so list must be empty.
    assertThat(permissions).isEmpty();
  }

  // ─────────────────────────────────────────────────────────────
  // findPermissionsOnResource – delegates to effective collection query
  // ─────────────────────────────────────────────────────────────

  @Test
  void findPermissionsOnResource_withCollectionId_shouldReturnInheritedPermissions() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<Permission> permissions =
        permissionService.findPermissionsOnResource(
            testUser.getUserId(), grandchildCollection.getCollectionId().toString());

    assertThat(permissions).hasSize(1);
    assertThat(permissions.get(0).isInherited()).isTrue();
  }

  // ─────────────────────────────────────────────────────────────
  // findResourcesWithPermission – collection inheritance fan-out
  // ─────────────────────────────────────────────────────────────

  @Test
  void findResourcesWithPermission_shouldIncludeInheritingDescendants() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    List<String> resources =
        permissionService.findResourcesWithPermission(
            testUser.getUserId(),
            Permission.Action.READ,
            Permission.ResourceType.COLLECTION);

    assertThat(resources).contains(rootCollection.getCollectionId().toString());
    assertThat(resources).contains(childCollection.getCollectionId().toString());
    assertThat(resources).contains(grandchildCollection.getCollectionId().toString());
  }

  @Test
  void findResourcesWithPermission_shouldNotIncludeNonInheritingChild() {
    permissionService.grantToUser(
        testUser.getUserId(),
        rootCollection.getCollectionId().toString(),
        Permission.ResourceType.COLLECTION,
        Permission.Action.READ);

    // grandchild does not inherit from child
    grandchildCollection.setInheritPermissions(false);
    collectionRepository.save(grandchildCollection);

    List<String> resources =
        permissionService.findResourcesWithPermission(
            testUser.getUserId(),
            Permission.Action.READ,
            Permission.ResourceType.COLLECTION);

    assertThat(resources).contains(rootCollection.getCollectionId().toString());
    assertThat(resources).contains(childCollection.getCollectionId().toString());
    assertThat(resources).doesNotContain(grandchildCollection.getCollectionId().toString());
  }

  @Test
  void findResourcesWithPermission_forNonCollectionType_shouldNotFanOut() {
    // Returns only direct matches for non-COLLECTION types
    List<String> resources =
        permissionService.findResourcesWithPermission(
            testUser.getUserId(),
            Permission.Action.READ,
            Permission.ResourceType.ELEMENT);

    // No element permissions granted → empty
    assertThat(resources).isEmpty();
  }

  // ─────────────────────────────────────────────────────────────
  // getCollectionHierarchy (PermissionService's own hierarchy resolver)
  // ─────────────────────────────────────────────────────────────

  @Test
  void getCollectionHierarchy_shouldReturnFullChainForGrandchild() {
    List<String> hierarchy =
        permissionService.getCollectionHierarchy(grandchildCollection.getCollectionId());

    assertThat(hierarchy).hasSize(3);
    assertThat(hierarchy.get(0)).isEqualTo(grandchildCollection.getCollectionId().toString());
    assertThat(hierarchy.get(1)).isEqualTo(childCollection.getCollectionId().toString());
    assertThat(hierarchy.get(2)).isEqualTo(rootCollection.getCollectionId().toString());
  }

  @Test
  void getCollectionHierarchy_shouldReturnSelf_whenInheritPermissionsIsFalse() {
    grandchildCollection.setInheritPermissions(false);
    collectionRepository.save(grandchildCollection);

    List<String> hierarchy =
        permissionService.getCollectionHierarchy(grandchildCollection.getCollectionId());

    assertThat(hierarchy).hasSize(1);
    assertThat(hierarchy.get(0)).isEqualTo(grandchildCollection.getCollectionId().toString());
  }

  @Test
  void getCollectionHierarchy_shouldReturnSingletonForUnknownUuid() {
    UUID unknownId = UUID.randomUUID();
    List<String> hierarchy = permissionService.getCollectionHierarchy(unknownId);

    assertThat(hierarchy).hasSize(1);
    assertThat(hierarchy.get(0)).isEqualTo(unknownId.toString());
  }
}
