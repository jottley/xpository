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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.UpdateCollectionRequestDto;
import net.ottleys.xpository.repository.CollectionRepository;

/**
 * Integration tests for CollectionController REST API.
 *
 * <p>Tests all CRUD endpoints end-to-end using TestRestTemplate with real database. Uses test
 * profile with in-memory SQLite database that is cleaned after each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CollectionControllerTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private CollectionRepository collectionRepository;

  @AfterEach
  void cleanup() {
    // Clean up test data after each test (except system root)
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);
  }

  // ==================== GET /api/v1/collections Tests ====================

  @SuppressWarnings("unchecked")
  @Test
  void shouldListCollectionsWithPagination() {
    // Given: A collection exists
    CreateCollectionRequestDto createRequest =
        new CreateCollectionRequestDto(
            "Test Collection", "Test description", "LIST", null, null, null, null, null, true);
    restTestClient.post().uri("/api/v1/collections").body(createRequest).exchange();

    // When: Requesting list of collections
    EntityExchangeResult<Map> response =
        restTestClient.get().uri("/api/v1/collections?page=0&size=20").exchange().expectBody(Map.class).returnResult();

    // Then: Returns paginated response
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat((Map<String, Object>) response.getResponseBody())
        .containsKeys("content", "page")
        .extractingByKey("page", InstanceOfAssertFactories.MAP)
        .containsKeys("size", "number", "totalElements", "totalPages");
  }

  // ==================== GET /api/v1/collections/{id} Tests ====================

  @Test
  void shouldGetCollectionByUuid() {
    // Given: A collection exists
    CreateCollectionRequestDto createRequest =
        new CreateCollectionRequestDto(
            "Test Collection", "Test description", "TREE", null, null, null, null, null, true);
    EntityExchangeResult<CollectionResponseDto> createResponse =
        restTestClient.post().uri("/api/v1/collections").body(createRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID collectionId = createResponse.getResponseBody().collectionId();

    // When: Requesting collection by UUID
    EntityExchangeResult<CollectionResponseDto> response =
        restTestClient.get().uri("/api/v1/collections/" + collectionId).exchange().expectBody(CollectionResponseDto.class).returnResult();

    // Then: Returns the collection
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().collectionId()).isEqualTo(collectionId);
    assertThat(response.getResponseBody().name()).isEqualTo("Test Collection");
    assertThat(response.getResponseBody().description()).isEqualTo("Test description");
    assertThat(response.getResponseBody().type()).isEqualTo("TREE");
  }

  @Test
  void shouldReturn404WhenCollectionNotFound() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();

    // When: Requesting non-existent collection
    EntityExchangeResult<String> response =
        restTestClient.get().uri("/api/v1/collections/" + unknownUuid).exchange().expectBody(String.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn400ForInvalidUuidFormat() {
    // When: Requesting with invalid UUID format
    EntityExchangeResult<Map> response =
        restTestClient.get().uri("/api/v1/collections/invalid-uuid").exchange().expectBody(Map.class).returnResult();

    // Then: Returns 400 with error details
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKey("message");
  }

  // ==================== POST /api/v1/collections Tests ====================

  @Test
  void shouldCreateNewCollection() {
    // Given: Valid create request
    CreateCollectionRequestDto request =
        new CreateCollectionRequestDto(
            "New Collection",
            "New description",
            "LIST",
            null,
            Set.of("xpository:core:base"),
            null,
            null,
            null,
            true);

    // When: Creating collection
    EntityExchangeResult<CollectionResponseDto> response =
        restTestClient.post().uri("/api/v1/collections").body(request).exchange().expectBody(CollectionResponseDto.class).returnResult();

    // Then: Returns 201 with created collection
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().collectionId()).isNotNull();
    assertThat(response.getResponseBody().name()).isEqualTo("New Collection");
    assertThat(response.getResponseBody().description()).isEqualTo("New description");
    assertThat(response.getResponseBody().type()).isEqualTo("LIST");
    assertThat(response.getResponseBody().allowedElementTypes()).contains("xpository:core:base");
    assertThat(response.getResponseBody().createdBy()).isEqualTo("system");
    assertThat(response.getResponseBody().createdDate()).isNotNull();
  }

  @Test
  void shouldReturn400WhenCreateRequestMissingName() {
    // Given: Invalid request missing required name
    CreateCollectionRequestDto request =
        new CreateCollectionRequestDto(null, null, "LIST", null, null, null, null, null, null);

    // When: Creating collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri("/api/v1/collections").body(request).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 400 with validation errors
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getResponseBody()).containsKeys("status", "message", "errors");
  }

  @Test
  void shouldReturn400WhenCreateRequestMissingType() {
    // Given: Invalid request missing required type
    CreateCollectionRequestDto request =
        new CreateCollectionRequestDto("Test", null, null, null, null, null, null, null, null);

    // When: Creating collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri("/api/v1/collections").body(request).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 400 with validation errors
    assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  // ==================== PUT /api/v1/collections/{id} Tests ====================

  @Test
  void shouldReplaceExistingCollection() {
    // Given: An existing collection
    CreateCollectionRequestDto createRequest =
        new CreateCollectionRequestDto(
            "Original", "Original desc", "LIST", null, null, null, null, null, true);
    EntityExchangeResult<CollectionResponseDto> createResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(createRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID collectionId = createResponse.getResponseBody().collectionId();

    // And: A replacement request
    CreateCollectionRequestDto replaceRequest =
        new CreateCollectionRequestDto(
            "Replaced", "Replaced desc", "TREE", null, null, null, null, null, true);

    EntityExchangeResult<CollectionResponseDto> response =
        restTestClient.put().uri("/api/v1/collections/" + collectionId).body(replaceRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();

    // Then: Returns updated collection
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().collectionId()).isEqualTo(collectionId);
    assertThat(response.getResponseBody().name()).isEqualTo("Replaced");
    assertThat(response.getResponseBody().description()).isEqualTo("Replaced desc");
    assertThat(response.getResponseBody().type()).isEqualTo("TREE");
  }

  @Test
  void shouldReturn404WhenReplacingNonExistentCollection() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();
    CreateCollectionRequestDto request =
        new CreateCollectionRequestDto("Test", null, "LIST", null, null, null, null, null, null);

    // When: Attempting to replace
    EntityExchangeResult<String> response =
        restTestClient.put().uri("/api/v1/collections/" + unknownUuid).body(request).exchange().expectBody(String.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== PATCH /api/v1/collections/{id} Tests ====================

  @Test
  void shouldPartiallyUpdateCollection() {
    // Given: An existing collection
    CreateCollectionRequestDto createRequest =
        new CreateCollectionRequestDto(
            "Original", "Original desc", "LIST", null, null, null, null, null, true);
    EntityExchangeResult<CollectionResponseDto> createResponse =
        restTestClient.post().uri("/api/v1/collections").body(createRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID collectionId = createResponse.getResponseBody().collectionId();
    Long version = createResponse.getResponseBody().version();

    // And: A partial update request (only name)
    UpdateCollectionRequestDto updateRequest =
        new UpdateCollectionRequestDto(
            version, "Updated Name", null, null, null, null, null, null, null);

    // When: Patching collection
    EntityExchangeResult<CollectionResponseDto> response =
        restTestClient.patch().uri("/api/v1/collections/" + collectionId).body(updateRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();

    // Then: Only specified field is updated
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().name()).isEqualTo("Updated Name");
    assertThat(response.getResponseBody().description()).isEqualTo("Original desc"); // Unchanged
    assertThat(response.getResponseBody().type()).isEqualTo("LIST"); // Unchanged
  }

  @Test
  void shouldReturn404WhenPatchingNonExistentCollection() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();
    UpdateCollectionRequestDto request =
        new UpdateCollectionRequestDto(0L, "Updated", null, null, null, null, null, null, null);

    // When: Attempting to patch
    EntityExchangeResult<String> response =
        restTestClient.patch().uri("/api/v1/collections/" + unknownUuid).body(request).exchange().expectBody(String.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== DELETE /api/v1/collections/{id} Tests ====================

  @Test
  void shouldDeleteCollection() {
    // Given: An existing collection
    CreateCollectionRequestDto createRequest =
        new CreateCollectionRequestDto(
            "To Delete", null, "LIST", null, null, null, null, null, null);
    EntityExchangeResult<CollectionResponseDto> createResponse =
        restTestClient.post().uri("/api/v1/collections").body(createRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID collectionId = createResponse.getResponseBody().collectionId();

    // When: Deleting collection
    EntityExchangeResult<Void> response =
        restTestClient.delete().uri("/api/v1/collections/" + collectionId).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 204 No Content
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: Collection is soft deleted (no longer found)
    EntityExchangeResult<String> getResponse =
        restTestClient.get().uri("/api/v1/collections/" + collectionId).exchange().expectBody(String.class).returnResult();
    assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenDeletingNonExistentCollection() {
    // Given: A non-existent UUID
    UUID unknownUuid = UUID.randomUUID();

    // When: Attempting to delete
    EntityExchangeResult<Void> response =
        restTestClient.delete().uri("/api/v1/collections/" + unknownUuid).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
