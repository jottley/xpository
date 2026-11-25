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
import net.ottleys.xpository.domain.Permission;
import net.ottleys.xpository.domain.Permission.Action;
import net.ottleys.xpository.domain.Permission.ResourceType;
import net.ottleys.xpository.domain.Permission.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Permission operations.
 *
 * <p>Provides CRUD operations and custom queries for permission management, including permission
 * checks, inheritance queries, and subject/resource lookups.
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  /**
   * Finds a permission by subject, resource, and action.
   *
   * @param subjectId the subject identifier
   * @param resourceId the resource identifier
   * @param action the action
   * @return Optional containing the permission if found
   */
  Optional<Permission> findBySubjectIdAndResourceIdAndAction(
      String subjectId, String resourceId, Action action);

  /**
   * Checks if a permission exists.
   *
   * @param subjectId the subject identifier
   * @param resourceId the resource identifier
   * @param action the action
   * @return true if permission exists
   */
  boolean existsBySubjectIdAndResourceIdAndAction(
      String subjectId, String resourceId, Action action);

  /**
   * Finds all permissions for a subject.
   *
   * @param subjectId the subject identifier
   * @return list of permissions
   */
  List<Permission> findBySubjectId(String subjectId);

  /**
   * Finds all permissions for a subject and subject type.
   *
   * @param subjectId the subject identifier
   * @param subjectType the subject type
   * @return list of permissions
   */
  List<Permission> findBySubjectIdAndSubjectType(String subjectId, SubjectType subjectType);

  /**
   * Finds all permissions for a resource.
   *
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  List<Permission> findByResourceId(String resourceId);

  /**
   * Finds all permissions for a resource and resource type.
   *
   * @param resourceId the resource identifier
   * @param resourceType the resource type
   * @return list of permissions
   */
  List<Permission> findByResourceIdAndResourceType(String resourceId, ResourceType resourceType);

  /**
   * Finds all permissions for a specific action.
   *
   * @param action the action
   * @return list of permissions
   */
  List<Permission> findByAction(Action action);

  /**
   * Finds all inherited permissions.
   *
   * @return list of inherited permissions
   */
  List<Permission> findByInheritedTrue();

  /**
   * Finds all direct (non-inherited) permissions.
   *
   * @return list of direct permissions
   */
  List<Permission> findByInheritedFalse();

  /**
   * Finds all permissions for a user (direct user permissions + group permissions).
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @return list of permissions
   */
  @Query(
      "SELECT p FROM Permission p WHERE "
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE')")
  List<Permission> findEffectivePermissionsForUser(
      @Param("userUuid") String userUuid, @Param("groupUuids") List<String> groupUuids);

  /**
   * Checks if a user has a specific permission on a resource.
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @param resourceId the resource identifier
   * @param action the action
   * @return true if user has permission
   */
  @Query(
      "SELECT COUNT(p) > 0 FROM Permission p WHERE "
          + "p.resourceId = :resourceId AND p.action = :action AND ("
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE'))")
  boolean hasPermission(
      @Param("userUuid") String userUuid,
      @Param("groupUuids") List<String> groupUuids,
      @Param("resourceId") String resourceId,
      @Param("action") Action action);

  /**
   * Finds all permissions on a resource for a user.
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @param resourceId the resource identifier
   * @return list of permissions
   */
  @Query(
      "SELECT p FROM Permission p WHERE "
          + "p.resourceId = :resourceId AND ("
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE'))")
  List<Permission> findPermissionsOnResource(
      @Param("userUuid") String userUuid,
      @Param("groupUuids") List<String> groupUuids,
      @Param("resourceId") String resourceId);

  /**
   * Checks if a user has permission on a collection or any of its parent collections.
   *
   * <p>This method supports permission inheritance by checking permissions on the given collection
   * and all its parent collections up to the root. Used when a collection has
   * inheritPermissions=true.
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @param collectionIds list of collection UUIDs (current collection + all parents)
   * @param action the action
   * @return true if user has permission on any collection in the hierarchy
   */
  @Query(
      "SELECT COUNT(p) > 0 FROM Permission p WHERE "
          + "p.resourceId IN :collectionIds AND p.action = :action AND p.resourceType = 'COLLECTION' AND ("
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE'))")
  boolean hasPermissionOnCollectionHierarchy(
      @Param("userUuid") String userUuid,
      @Param("groupUuids") List<String> groupUuids,
      @Param("collectionIds") List<String> collectionIds,
      @Param("action") Action action);

  /**
   * Finds all permissions on a collection hierarchy for a user.
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @param collectionIds list of collection UUIDs (current collection + all parents)
   * @return list of permissions
   */
  @Query(
      "SELECT p FROM Permission p WHERE "
          + "p.resourceId IN :collectionIds AND p.resourceType = 'COLLECTION' AND ("
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE'))")
  List<Permission> findPermissionsOnCollectionHierarchy(
      @Param("userUuid") String userUuid,
      @Param("groupUuids") List<String> groupUuids,
      @Param("collectionIds") List<String> collectionIds);

  /**
   * Finds all resources a user has a specific permission on.
   *
   * @param userUuid the user UUID as string
   * @param groupUuids list of group UUIDs as strings
   * @param action the action
   * @param resourceType the resource type
   * @return list of resource IDs
   */
  @Query(
      "SELECT DISTINCT p.resourceId FROM Permission p WHERE "
          + "p.action = :action AND p.resourceType = :resourceType AND ("
          + "(p.subjectId = :userUuid AND p.subjectType = 'USER') OR "
          + "(p.subjectId IN :groupUuids AND p.subjectType = 'GROUP') OR "
          + "(p.subjectType = 'EVERYONE'))")
  List<String> findResourceIdsWithPermission(
      @Param("userUuid") String userUuid,
      @Param("groupUuids") List<String> groupUuids,
      @Param("action") Action action,
      @Param("resourceType") ResourceType resourceType);

  /**
   * Deletes all permissions for a subject.
   *
   * @param subjectId the subject identifier
   */
  void deleteBySubjectId(String subjectId);

  /**
   * Deletes all permissions for a resource.
   *
   * @param resourceId the resource identifier
   */
  void deleteByResourceId(String resourceId);

  /**
   * Deletes all permissions for a subject on a specific resource.
   *
   * @param subjectId the subject identifier
   * @param resourceId the resource identifier
   */
  void deleteBySubjectIdAndResourceId(String subjectId, String resourceId);

  /**
   * Counts permissions for a subject.
   *
   * @param subjectId the subject identifier
   * @return count of permissions
   */
  long countBySubjectId(String subjectId);

  /**
   * Counts permissions for a resource.
   *
   * @param resourceId the resource identifier
   * @return count of permissions
   */
  long countByResourceId(String resourceId);

  /**
   * Finds all "everyone" permissions (public access).
   *
   * @return list of permissions granted to everyone
   */
  @Query("SELECT p FROM Permission p WHERE p.subjectType = 'EVERYONE'")
  List<Permission> findEveryonePermissions();

  /**
   * Finds all "everyone" permissions on a specific resource.
   *
   * @param resourceId the resource identifier
   * @return list of everyone permissions
   */
  @Query(
      "SELECT p FROM Permission p WHERE p.subjectType = 'EVERYONE' AND p.resourceId = :resourceId")
  List<Permission> findEveryonePermissionsOnResource(@Param("resourceId") String resourceId);
}
