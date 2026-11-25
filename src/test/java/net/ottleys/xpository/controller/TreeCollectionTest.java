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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for TREE collection functionality.
 *
 * <p>Tests materialized paths, tree traversal queries (subtree, ancestors, children), and depth
 * tracking for TREE collections. Uses test profile with in-memory SQLite database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TreeCollectionTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementVersionRepository versionRepository;
  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementCollectionMembershipRepository membershipRepository;

  private UUID treeCollectionId;

  @BeforeEach
  void setup() {
    // Create a TREE collection
    CreateCollectionRequestDto treeCollection = new CreateCollectionRequestDto();
    treeCollection.setName("Test Tree");
    treeCollection.setDescription("Tree collection for testing");
    treeCollection.setType("TREE");
    treeCollection.setInheritPermissions(true);

    ResponseEntity<CollectionResponseDto> collectionResponse =
        restTemplate.postForEntity(
            "/api/v1/collections", treeCollection, CollectionResponseDto.class);
    treeCollectionId = collectionResponse.getBody().collectionId();
  }

  @AfterEach
  void cleanup() {
    membershipRepository.deleteAll();
    versionRepository.deleteAll();
    elementRepository.deleteAll();
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);
  }

  @Test
  void shouldSetMaterializedPathForRootElement() {
    // Given: A TREE collection
    // When: Creating a root element
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    ResponseEntity<ElementResponseDto> rootResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            rootRequest,
            ElementResponseDto.class);

    // Then: Element is created successfully
    assertThat(rootResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID rootId = rootResponse.getBody().elementId();

    // And: Materialized path is "/"
    var collection = collectionRepository.findByCollectionId(treeCollectionId).orElseThrow();
    var element = elementRepository.findByElementId(rootId).orElseThrow();
    var membership =
        membershipRepository.findByElementAndCollection(element, collection).orElseThrow();

    assertThat(membership.getMaterializedPath()).isEqualTo("/");
    assertThat(membership.getDepth()).isEqualTo(0);
  }

  @Test
  void shouldSetMaterializedPathForNestedElements() {
    // Given: A root element
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    ResponseEntity<ElementResponseDto> rootResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            rootRequest,
            ElementResponseDto.class);
    UUID rootId = rootResponse.getBody().elementId();

    // When: Creating a child element
    CreateElementRequestDto childRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child\"}", rootId);
    ResponseEntity<ElementResponseDto> childResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            childRequest,
            ElementResponseDto.class);
    UUID childId = childResponse.getBody().elementId();

    // And: Creating a grandchild element
    CreateElementRequestDto grandchildRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Grandchild\"}", childId);
    ResponseEntity<ElementResponseDto> grandchildResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            grandchildRequest,
            ElementResponseDto.class);
    UUID grandchildId = grandchildResponse.getBody().elementId();

    // Then: Materialized paths are correct
    var collection = collectionRepository.findByCollectionId(treeCollectionId).orElseThrow();
    var rootElement = elementRepository.findByElementId(rootId).orElseThrow();
    var childElement = elementRepository.findByElementId(childId).orElseThrow();
    var grandchildElement = elementRepository.findByElementId(grandchildId).orElseThrow();

    var rootMembership =
        membershipRepository.findByElementAndCollection(rootElement, collection).orElseThrow();
    var childMembership =
        membershipRepository.findByElementAndCollection(childElement, collection).orElseThrow();
    var grandchildMembership =
        membershipRepository
            .findByElementAndCollection(grandchildElement, collection)
            .orElseThrow();

    assertThat(rootMembership.getMaterializedPath()).isEqualTo("/");
    assertThat(rootMembership.getDepth()).isEqualTo(0);

    assertThat(childMembership.getMaterializedPath()).startsWith("/");
    assertThat(childMembership.getMaterializedPath()).isNotEqualTo("/");
    assertThat(childMembership.getDepth()).isEqualTo(1);

    assertThat(grandchildMembership.getMaterializedPath())
        .startsWith(childMembership.getMaterializedPath() + "/");
    assertThat(grandchildMembership.getDepth()).isEqualTo(2);
  }

  @Test
  void shouldGetSubtreeElements() {
    // Given: A tree with root -> child1, child2 -> grandchild
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    ResponseEntity<ElementResponseDto> rootResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            rootRequest,
            ElementResponseDto.class);
    UUID rootId = rootResponse.getBody().elementId();

    CreateElementRequestDto child1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 1\"}", rootId);
    ResponseEntity<ElementResponseDto> child1Response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            child1Request,
            ElementResponseDto.class);
    UUID child1Id = child1Response.getBody().elementId();

    CreateElementRequestDto child2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 2\"}", rootId);
    restTemplate.postForEntity(
        "/api/v1/collections/" + treeCollectionId + "/elements",
        child2Request,
        ElementResponseDto.class);

    CreateElementRequestDto grandchildRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Grandchild\"}", child1Id);
    ResponseEntity<ElementResponseDto> grandchildResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            grandchildRequest,
            ElementResponseDto.class);
    UUID grandchildId = grandchildResponse.getBody().elementId();

    // When: Getting subtree of root
    ResponseEntity<List<ElementResponseDto>> subtreeResponse =
        restTemplate.exchange(
            "/api/v1/collections/" + treeCollectionId + "/elements/" + rootId + "/subtree",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementResponseDto>>() {});

    // Then: Returns all 3 descendants
    assertThat(subtreeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(subtreeResponse.getBody()).hasSize(3);

    // When: Getting subtree of child1
    ResponseEntity<List<ElementResponseDto>> child1SubtreeResponse =
        restTemplate.exchange(
            "/api/v1/collections/" + treeCollectionId + "/elements/" + child1Id + "/subtree",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementResponseDto>>() {});

    // Then: Returns only 1 descendant (grandchild)
    assertThat(child1SubtreeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(child1SubtreeResponse.getBody()).hasSize(1);
    assertThat(child1SubtreeResponse.getBody().get(0).elementId()).isEqualTo(grandchildId);
  }

  @Test
  void shouldGetAncestorElements() {
    // Given: A tree with root -> child -> grandchild
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    ResponseEntity<ElementResponseDto> rootResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            rootRequest,
            ElementResponseDto.class);
    UUID rootId = rootResponse.getBody().elementId();

    CreateElementRequestDto childRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child\"}", rootId);
    ResponseEntity<ElementResponseDto> childResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            childRequest,
            ElementResponseDto.class);
    UUID childId = childResponse.getBody().elementId();

    CreateElementRequestDto grandchildRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Grandchild\"}", childId);
    ResponseEntity<ElementResponseDto> grandchildResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            grandchildRequest,
            ElementResponseDto.class);
    UUID grandchildId = grandchildResponse.getBody().elementId();

    // When: Getting ancestors of grandchild
    ResponseEntity<List<ElementResponseDto>> ancestorsResponse =
        restTemplate.exchange(
            "/api/v1/collections/" + treeCollectionId + "/elements/" + grandchildId + "/ancestors",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementResponseDto>>() {});

    // Then: Returns child and root in order
    assertThat(ancestorsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ancestorsResponse.getBody()).hasSize(2);
    assertThat(ancestorsResponse.getBody().get(0).elementId()).isEqualTo(childId); // Parent first
    assertThat(ancestorsResponse.getBody().get(1).elementId()).isEqualTo(rootId); // Root last
  }

  @Test
  void shouldGetImmediateChildren() {
    // Given: A root with 3 children, one child has a grandchild
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    ResponseEntity<ElementResponseDto> rootResponse =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            rootRequest,
            ElementResponseDto.class);
    UUID rootId = rootResponse.getBody().elementId();

    CreateElementRequestDto child1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 1\"}", rootId);
    ResponseEntity<ElementResponseDto> child1Response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            child1Request,
            ElementResponseDto.class);
    UUID child1Id = child1Response.getBody().elementId();

    CreateElementRequestDto child2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 2\"}", rootId);
    restTemplate.postForEntity(
        "/api/v1/collections/" + treeCollectionId + "/elements",
        child2Request,
        ElementResponseDto.class);

    CreateElementRequestDto child3Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 3\"}", rootId);
    restTemplate.postForEntity(
        "/api/v1/collections/" + treeCollectionId + "/elements",
        child3Request,
        ElementResponseDto.class);

    CreateElementRequestDto grandchildRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Grandchild\"}", child1Id);
    restTemplate.postForEntity(
        "/api/v1/collections/" + treeCollectionId + "/elements",
        grandchildRequest,
        ElementResponseDto.class);

    // When: Getting children of root
    ResponseEntity<List<ElementResponseDto>> childrenResponse =
        restTemplate.exchange(
            "/api/v1/collections/" + treeCollectionId + "/elements/" + rootId + "/children",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementResponseDto>>() {});

    // Then: Returns only 3 immediate children (not grandchild)
    assertThat(childrenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(childrenResponse.getBody()).hasSize(3);
  }

  @Test
  void shouldGetRootElements() {
    // Given: A tree with 2 roots, each with children
    CreateElementRequestDto root1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root 1\"}");
    ResponseEntity<ElementResponseDto> root1Response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            root1Request,
            ElementResponseDto.class);
    UUID root1Id = root1Response.getBody().elementId();

    CreateElementRequestDto root2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root 2\"}");
    ResponseEntity<ElementResponseDto> root2Response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + treeCollectionId + "/elements",
            root2Request,
            ElementResponseDto.class);
    UUID root2Id = root2Response.getBody().elementId();

    // Add children to root1
    CreateElementRequestDto childRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child\"}", root1Id);
    restTemplate.postForEntity(
        "/api/v1/collections/" + treeCollectionId + "/elements",
        childRequest,
        ElementResponseDto.class);

    // When: Getting root elements
    ResponseEntity<List<ElementResponseDto>> rootsResponse =
        restTemplate.exchange(
            "/api/v1/collections/" + treeCollectionId + "/roots",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ElementResponseDto>>() {});

    // Then: Returns only 2 root elements (not children)
    assertThat(rootsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(rootsResponse.getBody()).hasSize(2);
    List<UUID> rootIds =
        rootsResponse.getBody().stream().map(ElementResponseDto::elementId).toList();
    assertThat(rootIds).containsExactlyInAnyOrder(root1Id, root2Id);
  }
}
