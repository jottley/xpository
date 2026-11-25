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
import net.ottleys.xpository.dto.CreateRelationshipRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.RelationshipResponseDto;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRelationshipRepository;
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
 * Integration tests for GRAPH collection functionality.
 *
 * <p>Tests directed relationships between elements, including creation, retrieval (outgoing and
 * incoming), and deletion. Uses test profile with in-memory SQLite database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class GraphCollectionTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementVersionRepository versionRepository;
  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementCollectionMembershipRepository membershipRepository;
  @Autowired private ElementRelationshipRepository relationshipRepository;

  private UUID graphCollectionId;

  @BeforeEach
  void setup() {
    // Create a GRAPH collection
    CreateCollectionRequestDto graphCollection = new CreateCollectionRequestDto();
    graphCollection.setName("Test Graph");
    graphCollection.setDescription("Graph collection for testing");
    graphCollection.setType("GRAPH");
    graphCollection.setInheritPermissions(true);

    ResponseEntity<CollectionResponseDto> collectionResponse =
        restTemplate.postForEntity(
            "/api/v1/collections", graphCollection, CollectionResponseDto.class);
    graphCollectionId = collectionResponse.getBody().collectionId();
  }

  @AfterEach
  void cleanup() {
    relationshipRepository.deleteAll();
    membershipRepository.deleteAll();
    versionRepository.deleteAll();
    elementRepository.deleteAll();
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);
  }

  @Test
  void shouldCreateRelationshipBetweenElements() {
    // Given: Two elements in the graph collection
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");

    // When: Creating a relationship from A to B
    CreateRelationshipRequestDto request =
        new CreateRelationshipRequestDto(elementA, elementB, "depends-on");
    ResponseEntity<RelationshipResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request,
            RelationshipResponseDto.class);

    // Then: Relationship is created successfully
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sourceElementId()).isEqualTo(elementA);
    assertThat(response.getBody().targetElementId()).isEqualTo(elementB);
    assertThat(response.getBody().relationshipType()).isEqualTo("depends-on");
    assertThat(response.getBody().collectionId()).isEqualTo(graphCollectionId);
    assertThat(response.getBody().createdBy()).isEqualTo("system");
  }

  @Test
  void shouldCreateRelationshipWithMetadata() {
    // Given: Two elements in the graph collection
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");

    // When: Creating a relationship with metadata
    CreateRelationshipRequestDto request =
        new CreateRelationshipRequestDto(
            elementA,
            elementB,
            "references",
            "{\"strength\": \"strong\", \"notes\": \"Critical\"}");
    ResponseEntity<RelationshipResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request,
            RelationshipResponseDto.class);

    // Then: Relationship includes metadata
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().relationshipMetadata())
        .isEqualTo("{\"strength\": \"strong\", \"notes\": \"Critical\"}");
  }

  @Test
  void shouldAllowMultipleRelationshipTypesBetweenSameElements() {
    // Given: Two elements in the graph collection
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");

    // When: Creating multiple relationships with different types
    CreateRelationshipRequestDto request1 =
        new CreateRelationshipRequestDto(elementA, elementB, "depends-on");
    CreateRelationshipRequestDto request2 =
        new CreateRelationshipRequestDto(elementA, elementB, "references");

    ResponseEntity<RelationshipResponseDto> response1 =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request1,
            RelationshipResponseDto.class);
    ResponseEntity<RelationshipResponseDto> response2 =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request2,
            RelationshipResponseDto.class);

    // Then: Both relationships are created
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response1.getBody().relationshipType()).isEqualTo("depends-on");
    assertThat(response2.getBody().relationshipType()).isEqualTo("references");
  }

  @Test
  void shouldPreventDuplicateRelationships() {
    // Given: A relationship already exists
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");
    CreateRelationshipRequestDto request =
        new CreateRelationshipRequestDto(elementA, elementB, "depends-on");
    restTemplate.postForEntity(
        "/api/v1/collections/" + graphCollectionId + "/relationships",
        request,
        RelationshipResponseDto.class);

    // When: Trying to create the same relationship again
    ResponseEntity<RelationshipResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request,
            RelationshipResponseDto.class);

    // Then: Request fails with 422
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  void shouldPreventSelfReferentialRelationships() {
    // Given: An element in the graph collection
    UUID elementA = createElementInGraph("Element A");

    // When: Trying to create a relationship to itself
    CreateRelationshipRequestDto request =
        new CreateRelationshipRequestDto(elementA, elementA, "depends-on");
    ResponseEntity<RelationshipResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/relationships",
            request,
            RelationshipResponseDto.class);

    // Then: Request fails with 422
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  void shouldGetOutgoingRelationships() {
    // Given: Element A has relationships to B and C
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");
    UUID elementC = createElementInGraph("Element C");

    createRelationship(elementA, elementB, "depends-on");
    createRelationship(elementA, elementC, "references");

    // When: Getting outgoing relationships from A
    ResponseEntity<List<RelationshipResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/elements/"
                + elementA
                + "/relationships/outgoing",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RelationshipResponseDto>>() {});

    // Then: Returns 2 outgoing relationships
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()).allMatch(r -> r.sourceElementId().equals(elementA));
  }

  @Test
  void shouldGetIncomingRelationships() {
    // Given: Elements A and B both have relationships to C
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");
    UUID elementC = createElementInGraph("Element C");

    createRelationship(elementA, elementC, "depends-on");
    createRelationship(elementB, elementC, "references");

    // When: Getting incoming relationships to C
    ResponseEntity<List<RelationshipResponseDto>> response =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/elements/"
                + elementC
                + "/relationships/incoming",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RelationshipResponseDto>>() {});

    // Then: Returns 2 incoming relationships
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()).allMatch(r -> r.targetElementId().equals(elementC));
  }

  @Test
  void shouldDeleteRelationship() {
    // Given: A relationship exists
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");
    createRelationship(elementA, elementB, "depends-on");

    // When: Deleting the relationship
    ResponseEntity<Void> response =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/relationships?source="
                + elementA
                + "&target="
                + elementB
                + "&type=depends-on",
            HttpMethod.DELETE,
            null,
            Void.class);

    // Then: Relationship is deleted
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: Relationship no longer exists
    ResponseEntity<List<RelationshipResponseDto>> outgoingResponse =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/elements/"
                + elementA
                + "/relationships/outgoing",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RelationshipResponseDto>>() {});
    assertThat(outgoingResponse.getBody()).isEmpty();
  }

  @Test
  void shouldDeleteAllRelationshipsWhenElementRemovedFromGraph() {
    // Given: Element A has relationships to B and C, and D has relationship to A
    UUID elementA = createElementInGraph("Element A");
    UUID elementB = createElementInGraph("Element B");
    UUID elementC = createElementInGraph("Element C");
    UUID elementD = createElementInGraph("Element D");

    createRelationship(elementA, elementB, "depends-on");
    createRelationship(elementA, elementC, "references");
    createRelationship(elementD, elementA, "depends-on");

    // When: Removing element A from the graph
    restTemplate.delete("/api/v1/collections/" + graphCollectionId + "/elements/" + elementA);

    // Then: All relationships involving A are deleted
    ResponseEntity<List<RelationshipResponseDto>> dOutgoing =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/elements/"
                + elementD
                + "/relationships/outgoing",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RelationshipResponseDto>>() {});
    assertThat(dOutgoing.getBody()).isEmpty();

    ResponseEntity<List<RelationshipResponseDto>> bIncoming =
        restTemplate.exchange(
            "/api/v1/collections/"
                + graphCollectionId
                + "/elements/"
                + elementB
                + "/relationships/incoming",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RelationshipResponseDto>>() {});
    assertThat(bIncoming.getBody()).isEmpty();
  }

  // Helper methods

  private UUID createElementInGraph(String title) {
    CreateElementRequestDto request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"" + title + "\"}");
    ResponseEntity<ElementResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + graphCollectionId + "/elements",
            request,
            ElementResponseDto.class);
    return response.getBody().elementId();
  }

  private void createRelationship(UUID source, UUID target, String type) {
    CreateRelationshipRequestDto request = new CreateRelationshipRequestDto(source, target, type);
    restTemplate.postForEntity(
        "/api/v1/collections/" + graphCollectionId + "/relationships",
        request,
        RelationshipResponseDto.class);
  }
}
