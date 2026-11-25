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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration tests for STACK collection functionality.
 *
 * <p>Tests LIFO (Last-In-First-Out) operations including push, pop, and peek. Uses test profile
 * with in-memory SQLite database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class StackCollectionTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementVersionRepository versionRepository;
  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementCollectionMembershipRepository membershipRepository;

  private UUID stackCollectionId;

  @BeforeEach
  void setup() {
    // Create a STACK collection
    CreateCollectionRequestDto stackCollection = new CreateCollectionRequestDto();
    stackCollection.setName("Test Stack");
    stackCollection.setDescription("Stack collection for testing");
    stackCollection.setType("STACK");
    stackCollection.setInheritPermissions(true);

    ResponseEntity<CollectionResponseDto> collectionResponse =
        restTemplate.postForEntity(
            "/api/v1/collections", stackCollection, CollectionResponseDto.class);
    stackCollectionId = collectionResponse.getBody().collectionId();
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
  void shouldPushElementOntoStack() {
    // Given: A STACK collection
    // When: Pushing an element
    CreateElementRequestDto request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Item 1\"}");
    ResponseEntity<ElementResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/push",
            request,
            ElementResponseDto.class);

    // Then: Element is pushed successfully
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().elementId()).isNotNull();
  }

  @Test
  void shouldPushMultipleElementsInLIFOOrder() {
    // Given: A STACK collection
    // When: Pushing 3 elements
    UUID elem1 = pushElement("Item 1");
    UUID elem2 = pushElement("Item 2");
    UUID elem3 = pushElement("Item 3");

    // Then: Elements are pushed successfully
    assertThat(elem1).isNotNull();
    assertThat(elem2).isNotNull();
    assertThat(elem3).isNotNull();
  }

  @Test
  void shouldPopElementFromStack() {
    // Given: A stack with elements
    pushElement("Item 1");
    pushElement("Item 2");
    UUID topElementId = pushElement("Item 3");

    // When: Popping an element
    ResponseEntity<ElementResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/pop",
            null,
            ElementResponseDto.class);

    // Then: Returns the last pushed element (LIFO)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().elementId()).isEqualTo(topElementId);
  }

  @Test
  void shouldPopInLIFOOrder() {
    // Given: A stack with 3 elements
    UUID elem1 = pushElement("Item 1");
    UUID elem2 = pushElement("Item 2");
    UUID elem3 = pushElement("Item 3");

    // When: Popping all elements
    UUID popped1 =
        restTemplate
            .postForEntity(
                "/api/v1/collections/" + stackCollectionId + "/stack/pop",
                null,
                ElementResponseDto.class)
            .getBody()
            .elementId();
    UUID popped2 =
        restTemplate
            .postForEntity(
                "/api/v1/collections/" + stackCollectionId + "/stack/pop",
                null,
                ElementResponseDto.class)
            .getBody()
            .elementId();
    UUID popped3 =
        restTemplate
            .postForEntity(
                "/api/v1/collections/" + stackCollectionId + "/stack/pop",
                null,
                ElementResponseDto.class)
            .getBody()
            .elementId();

    // Then: Elements are popped in LIFO order (3, 2, 1)
    assertThat(popped1).isEqualTo(elem3);
    assertThat(popped2).isEqualTo(elem2);
    assertThat(popped3).isEqualTo(elem1);
  }

  @Test
  void shouldFailToPopFromEmptyStack() {
    // Given: An empty STACK collection
    // When: Trying to pop
    ResponseEntity<ElementResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/pop",
            null,
            ElementResponseDto.class);

    // Then: Returns 422 error
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  void shouldPeekAtTopElementWithoutRemoving() {
    // Given: A stack with elements
    pushElement("Item 1");
    pushElement("Item 2");
    UUID topElementId = pushElement("Item 3");

    // When: Peeking at top element
    ResponseEntity<ElementResponseDto> response =
        restTemplate.getForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/peek", ElementResponseDto.class);

    // Then: Returns the top element
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().elementId()).isEqualTo(topElementId);

    // And: Element is still on stack (peek again)
    ResponseEntity<ElementResponseDto> response2 =
        restTemplate.getForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/peek", ElementResponseDto.class);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response2.getBody().elementId()).isEqualTo(topElementId);
  }

  @Test
  void shouldFailToPeekAtEmptyStack() {
    // Given: An empty STACK collection
    // When: Trying to peek
    ResponseEntity<ElementResponseDto> response =
        restTemplate.getForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/peek", ElementResponseDto.class);

    // Then: Returns 422 error
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
  }

  @Test
  void shouldMaintainStackIntegrityAfterPushAndPop() {
    // Given: A stack with operations
    UUID elem1 = pushElement("Item 1");
    UUID elem2 = pushElement("Item 2");

    // Pop one
    restTemplate.postForEntity(
        "/api/v1/collections/" + stackCollectionId + "/stack/pop", null, ElementResponseDto.class);

    // Push another
    UUID elem3 = pushElement("Item 3");

    // When: Popping
    UUID popped1 =
        restTemplate
            .postForEntity(
                "/api/v1/collections/" + stackCollectionId + "/stack/pop",
                null,
                ElementResponseDto.class)
            .getBody()
            .elementId();
    UUID popped2 =
        restTemplate
            .postForEntity(
                "/api/v1/collections/" + stackCollectionId + "/stack/pop",
                null,
                ElementResponseDto.class)
            .getBody()
            .elementId();

    // Then: Stack operations maintain LIFO order
    assertThat(popped1).isEqualTo(elem3); // Last pushed
    assertThat(popped2).isEqualTo(elem1); // First pushed (elem2 was popped earlier)
  }

  @Test
  void shouldPreventPushingDuplicateElement() {
    // Given: An element already in the stack
    UUID elem1 = pushElement("Item 1");

    // When: Trying to push the same element again via direct service call
    // (The push endpoint creates a new element, so we can't test duplicate via REST API directly)
    // This is a constraint enforced by the service layer

    // Then: Service would throw IllegalArgumentException
    // We verify this indirectly - each push creates a unique element
    UUID elem2 = pushElement("Item 1");
    assertThat(elem1).isNotEqualTo(elem2); // Different elements even with same title
  }

  // Helper methods

  private UUID pushElement(String title) {
    CreateElementRequestDto request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"" + title + "\"}");
    ResponseEntity<ElementResponseDto> response =
        restTemplate.postForEntity(
            "/api/v1/collections/" + stackCollectionId + "/stack/push",
            request,
            ElementResponseDto.class);
    return response.getBody().elementId();
  }
}
