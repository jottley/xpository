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
import java.util.Map;
import java.util.UUID;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.UpdateElementRequestDto;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.ElementVersionRepository;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Integration tests for ElementController REST API.
 *
 * <p>Tests all CRUD endpoints end-to-end using TestRestTemplate with real database. Uses test
 * profile with in-memory SQLite database that is cleaned after each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ElementControllerTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private ElementRepository elementRepository;

  @Autowired private ElementVersionRepository versionRepository;

  @Autowired private ElementTraitRepository elementTraitRepository;

  @Autowired private CollectionRepository collectionRepository;

  @Autowired private ElementCollectionMembershipRepository membershipRepository;

  private UUID testCollectionId;

  @BeforeEach
  void setup() {
    cleanup();

    // Create a test collection that all elements will belong to
    CreateCollectionRequestDto collectionRequest =
        new CreateCollectionRequestDto(
            "Test Collection",
            "Collection for element tests",
            "LIST",
            null,
            null,
            null,
            null,
            null,
            true);
    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post()
            .uri("/api/v1/collections")
            .body(collectionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CollectionResponseDto.class).returnResult();
    testCollectionId = collectionResponse.getResponseBody().collectionId();
  }

  @AfterEach
  void cleanup() {
    // Clean up test data after each test
    membershipRepository.deleteAll();
    versionRepository.deleteAll();
    elementTraitRepository.deleteAll();
    elementRepository.deleteAll();
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);
  }

  // ==================== GET /api/v1/elements Tests ====================

  @SuppressWarnings("unchecked")
  @Test
  void shouldListElementsWithPagination() {
    // Given: An element exists
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Test Element\"}");
    restTestClient.post()
        .uri("/api/v1/collections/" + testCollectionId + "/elements")
        .body(createRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementResponseDto.class);

    // When: Requesting list of elements
    EntityExchangeResult<Map> response =
        restTestClient.get()
            .uri("/api/v1/elements?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns paginated response
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat((Map<String, Object>) response.getResponseBody())
        .containsKeys("content", "page")
        .extractingByKey("page", InstanceOfAssertFactories.MAP)
        .containsKeys("size", "number", "totalElements", "totalPages");
  }

  @Test
  void shouldFilterElementsByTypeName() {
    // Given: Multiple elements with different types
    CreateElementRequestDto request1 =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Element 1\"}");
    CreateElementRequestDto request2 =
        new CreateElementRequestDto("xpository:document:generic", "{\"title\": \"Document 1\"}");
    restTestClient.post()
        .uri("/api/v1/collections/" + testCollectionId + "/elements")
        .body(request1)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementResponseDto.class);
    restTestClient.post()
        .uri("/api/v1/collections/" + testCollectionId + "/elements")
        .body(request2)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementResponseDto.class);

    // When: Filtering by element type
    EntityExchangeResult<Map> response =
        restTestClient.get()
            .uri("/api/v1/elements?elementTypeName=xpository:core:standard")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns only matching elements
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).containsKey("content");
  }

  // ==================== GET /api/v1/elements/{id} Tests ====================

  @Test
  void shouldGetElementByUuid() {
    // Given: An element exists
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto(
            "xpository:core:standard",
            "{\"title\": \"Test Element\", \"description\": \"Test description\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();

    // When: Requesting element by UUID
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Returns the element
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().elementId()).isEqualTo(elementId);
    assertThat(response.getResponseBody().elementTypeName()).isEqualTo("xpository:core:standard");
    assertThat(response.getResponseBody().propertiesJson()).contains("Test Element", "Test description");
  }

  @Test
  void shouldReturn404WhenElementNotFound() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();

    // When: Requesting non-existent element
    EntityExchangeResult<String> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + unknownUuid)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn400ForInvalidUuidFormat() {
    // When: Requesting with invalid UUID format
    EntityExchangeResult<Map> response =
        restTestClient.get()
            .uri("/api/v1/elements/invalid-uuid")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 400 with error details
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKey("message");
  }

  // ==================== POST /api/v1/elements Tests ====================

  @Test
  void shouldCreateNewElement() {
    // Given: Valid create request
    CreateElementRequestDto request =
        new CreateElementRequestDto(
            "xpository:core:standard",
            "{\"title\": \"New Element\", \"description\": \"New description\", \"status\": \"draft\"}");

    // When: Creating element in collection
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Returns 201 with created element
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().elementId()).isNotNull();
    assertThat(response.getResponseBody().version()).isEqualTo(0L);
    assertThat(response.getResponseBody().elementTypeName()).isEqualTo("xpository:core:standard");
    assertThat(response.getResponseBody().propertiesJson()).contains("New Element");
    assertThat(response.getResponseBody().createdBy()).isEqualTo("system");
    assertThat(response.getResponseBody().createdDate()).isNotNull();
    assertThat(response.getResponseHeaders().getLocation()).isNotNull();
    assertThat(response.getResponseHeaders().getLocation().toString())
        .contains("/api/v1/elements/" + response.getResponseBody().elementId());
  }

  @Test
  void shouldCreateElementWithDocumentType() {
    // Given: Valid document element request
    CreateElementRequestDto request =
        new CreateElementRequestDto(
            "xpository:document:generic",
            "{\"title\": \"Project Report\", \"author\": \"John Doe\", \"publishDate\": \"2026-06-05\"}");

    // When: Creating document element in collection
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Returns 201 with created element
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().elementTypeName()).isEqualTo("xpository:document:generic");
    assertThat(response.getResponseBody().propertiesJson()).contains("Project Report", "John Doe");
  }

  @Test
  void shouldReturn400WhenCreateRequestMissingElementType() {
    // Given: Invalid request missing required element type
    CreateElementRequestDto request = new CreateElementRequestDto(null, "{\"title\": \"Test\"}");

    // When: Creating element
    EntityExchangeResult<Map> response =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 400 with validation errors
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKeys("status", "message", "errors");
  }

  @Test
  void shouldReturn422WhenElementTypeDoesNotExist() {
    // Given: Request with non-existent element type
    CreateElementRequestDto request =
        new CreateElementRequestDto("non:existent:type", "{\"title\": \"Test\"}");

    // When: Creating element
    EntityExchangeResult<Map> response =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(request)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 422 Unprocessable Entity
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(response.getResponseBody()).containsKey("message");
    assertThat(response.getResponseBody().get("message").toString())
        .contains("Element type with name non:existent:type does not exist");
  }

  @Test
  void shouldReturn400WhenPropertiesJsonIsInvalid() {
    // Given: Request with invalid JSON
    CreateElementRequestDto request =
        new CreateElementRequestDto("xpository:core:standard", "{invalid json}");

    // When: Creating element
    EntityExchangeResult<Map> response =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 400 with validation error
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKey("errors");
  }

  // ==================== PATCH /api/v1/elements/{id} Tests ====================

  @Test
  void shouldPartiallyUpdateElement() {
    // Given: An existing element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto(
            "xpository:core:standard",
            "{\"title\": \"Original\", \"description\": \"Original desc\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();
    Long version = createResponse.getResponseBody().version();

    // And: A partial update request (only properties)
    UpdateElementRequestDto updateRequest =
        new UpdateElementRequestDto(
            version, null, "{\"title\": \"Updated\", \"description\": \"Updated desc\"}");

    // When: Patching element
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(updateRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Only specified field is updated
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().version()).isEqualTo(1L); // Version incremented
    assertThat(response.getResponseBody().majorVersion()).isEqualTo(1);
    assertThat(response.getResponseBody().minorVersion()).isEqualTo(1);
    assertThat(response.getResponseBody().versionString()).isEqualTo("1.1");
    assertThat(response.getResponseBody().propertiesJson()).contains("Updated");
    assertThat(response.getResponseBody().elementTypeName())
        .isEqualTo("xpository:core:standard"); // Unchanged
  }

  @Test
  void shouldIncrementMajorVersionWhenMajorFlagIsTrue() {
    // Given: An existing element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto(
            "xpository:core:standard",
            "{\"title\": \"Original\", \"description\": \"Original desc\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();
    Long version = createResponse.getResponseBody().version();

    // And: A major update request
    UpdateElementRequestDto majorUpdateRequest =
        new UpdateElementRequestDto(version, null, "{\"title\": \"Breaking Change\"}", true);

    // When: Patching element with majorVersion = true
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(majorUpdateRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Major version incremented, minor version reset to 0
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().majorVersion()).isEqualTo(2);
    assertThat(response.getResponseBody().minorVersion()).isZero();
    assertThat(response.getResponseBody().versionString()).isEqualTo("2.0");

    // And: Subsequent minor update increments minor version from 2.0 to 2.1
    UpdateElementRequestDto subsequentUpdate =
        new UpdateElementRequestDto(
            response.getResponseBody().version(), null, "{\"title\": \"Subsequent Minor Change\"}", false);
    EntityExchangeResult<ElementResponseDto> subsequentResponse =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(subsequentUpdate)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    assertThat(subsequentResponse.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(subsequentResponse.getResponseBody()).isNotNull();
    assertThat(subsequentResponse.getResponseBody().majorVersion()).isEqualTo(2);
    assertThat(subsequentResponse.getResponseBody().minorVersion()).isEqualTo(1);
    assertThat(subsequentResponse.getResponseBody().versionString()).isEqualTo("2.1");
  }

  @Test
  void shouldUpdateElementType() {
    // Given: An existing standard element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Test\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();
    Long version = createResponse.getResponseBody().version();

    // And: Update request changing type to document
    UpdateElementRequestDto updateRequest =
        new UpdateElementRequestDto(
            version,
            "xpository:document:generic",
            "{\"title\": \"Test Document\", \"author\": \"Jane\"}");

    // When: Patching element
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(updateRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    // Then: Element type is updated
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody().elementTypeName()).isEqualTo("xpository:document:generic");
  }

  @Test
  void shouldReturn404WhenPatchingNonExistentElement() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();
    UpdateElementRequestDto request =
        new UpdateElementRequestDto(0L, null, "{\"title\": \"Test\"}");

    // When: Attempting to patch
    EntityExchangeResult<String> response =
        restTestClient.patch()
            .uri("/api/v1/elements/" + unknownUuid)
            .body(request)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn400WhenUpdateRequestMissingVersion() {
    // Given: An existing element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Test\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();

    // And: Update request without version (required for optimistic locking)
    UpdateElementRequestDto updateRequest =
        new UpdateElementRequestDto(null, null, "{\"title\": \"Updated\"}");

    // When: Patching element
    EntityExchangeResult<Map> response =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(updateRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 400 with validation error
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKey("errors");
  }

  @Test
  void shouldReturn409OnVersionConflict() {
    // Given: An existing element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Test\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();
    Long version = createResponse.getResponseBody().version();

    // When: User 1 updates the element with correct version
    UpdateElementRequestDto update1 =
        new UpdateElementRequestDto(version, null, "{\"title\": \"Updated by User 1\"}");
    EntityExchangeResult<ElementResponseDto> response1 =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(update1)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK);

    // And: User 2 tries to update with the old version (stale version)
    UpdateElementRequestDto update2 =
        new UpdateElementRequestDto(version, null, "{\"title\": \"Updated by User 2\"}");
    EntityExchangeResult<Map> response2 =
        restTestClient.patch()
            .uri("/api/v1/elements/" + elementId)
            .body(update2)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 409 Conflict
    assertThat(response2.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response2.getResponseBody()).containsKey("message");
    assertThat(response2.getResponseBody().get("message").toString()).contains("modified by another user");
  }

  // ==================== DELETE /api/v1/elements/{id} Tests ====================

  @Test
  void shouldDeleteElement() {
    // Given: An existing element
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"To Delete\"}");
    EntityExchangeResult<ElementResponseDto> createResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = createResponse.getResponseBody().elementId();

    // When: Deleting element
    EntityExchangeResult<Void> response =
        restTestClient.delete()
            .uri("/api/v1/elements/" + elementId)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 204 No Content
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: Element is permanently deleted (no longer found)
    EntityExchangeResult<String> getResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class)
            .returnResult();
    assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenDeletingNonExistentElement() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();

    // When: Attempting to delete
    EntityExchangeResult<Void> response =
        restTestClient.delete()
            .uri("/api/v1/elements/" + unknownUuid)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== GET /api/v1/elements/{elementId}/collections Tests ====================

  @SuppressWarnings("unchecked")
  @Test
  void shouldListCollectionsContainingElement() {
    // Given: An element exists in multiple collections
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Shared Element\"}");
    EntityExchangeResult<ElementResponseDto> elementResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = elementResponse.getResponseBody().elementId();

    // And: Create a second collection and add the same element to it
    CreateCollectionRequestDto collection2Request =
        new CreateCollectionRequestDto(
            "Second Collection",
            "Second test collection",
            "LIST",
            null,
            null,
            null,
            null,
            null,
            true);
    EntityExchangeResult<CollectionResponseDto> collection2Response =
        restTestClient.post()
            .uri("/api/v1/collections")
            .body(collection2Request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID collection2Id = collection2Response.getResponseBody().collectionId();

    restTestClient.post()
        .uri("/api/v1/collections/" + collection2Id + "/elements/" + elementId)
        .exchange()
        .expectStatus().isNoContent()
        .expectBody(Void.class)
        .returnResult();

    // When: Requesting collections for the element
    EntityExchangeResult<Map> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/collections?page=0&size=20")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns paginated list with both collections
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat((Map<String, Object>) response.getResponseBody())
        .containsKeys("content", "page")
        .extractingByKey("page", InstanceOfAssertFactories.MAP)
        .containsKeys("size", "number", "totalElements", "totalPages");

    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(2);
    assertThat(content.get(0)).containsKey("collectionId");
    assertThat(content.get(1)).containsKey("collectionId");
  }

  @Test
  void shouldDeleteElementWhenRemovedFromLastCollection() {
    // Given: An element exists in one collection
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Temporary Element\"}");
    EntityExchangeResult<ElementResponseDto> elementResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = elementResponse.getResponseBody().elementId();

    // When: The element is removed from its only collection
    EntityExchangeResult<Void> deleteResponse =
        restTestClient.delete()
            .uri("/api/v1/collections/" + testCollectionId + "/elements/" + elementId)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 204 No Content
    assertThat(deleteResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: The element no longer exists (was deleted because it had no collections left)
    EntityExchangeResult<Map> getResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(Map.class)
            .returnResult();
    assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldKeepElementAliveWhenRemovedFromOneOfMultipleCollections() {
    // Given: An element exists in multiple collections
    CreateElementRequestDto createRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Multi-Collection Element\"}");
    EntityExchangeResult<ElementResponseDto> elementResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + testCollectionId + "/elements")
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = elementResponse.getResponseBody().elementId();

    // And: Create a second collection and add the element to it
    CreateCollectionRequestDto collection2Request =
        new CreateCollectionRequestDto(
            "Second Collection",
            "Another test collection",
            "LIST",
            null,
            null,
            null,
            null,
            null,
            true);
    EntityExchangeResult<CollectionResponseDto> collection2Response =
        restTestClient.post()
            .uri("/api/v1/collections")
            .body(collection2Request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID collection2Id = collection2Response.getResponseBody().collectionId();

    restTestClient.post()
        .uri("/api/v1/collections/" + collection2Id + "/elements/" + elementId)
        .exchange()
        .expectStatus().isNoContent()
        .expectBody(Void.class)
        .returnResult();

    // When: Removing element from the first collection (but still in second)
    EntityExchangeResult<Void> deleteResponse =
        restTestClient.delete()
            .uri("/api/v1/collections/" + testCollectionId + "/elements/" + elementId)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 204 No Content
    assertThat(deleteResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: The element still exists (because it's still in collection2)
    EntityExchangeResult<ElementResponseDto> getResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.OK);

    // And: Element is only in collection2 now
    EntityExchangeResult<Map> collectionsResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/collections")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> collections =
        (List<Map<String, Object>>) collectionsResponse.getResponseBody().get("content");
    assertThat(collections).hasSize(1);
    assertThat(collections.get(0).get("collectionId")).isEqualTo(collection2Id.toString());
  }

  @Test
  void shouldReturn404WhenListingCollectionsForNonExistentElement() {
    // Given: A non-existent element UUID
    UUID unknownElementId = UUID.randomUUID();

    // When: Requesting collections for non-existent element
    EntityExchangeResult<Map> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + unknownElementId + "/collections")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
