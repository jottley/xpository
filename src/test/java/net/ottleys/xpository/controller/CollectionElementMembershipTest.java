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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementVersionRepository;

/**
 * Integration tests for Collection-Element membership REST API endpoints.
 *
 * <p>Tests the 3 membership endpoints at /api/v1/collections/{collectionId}/elements and
 * /api/v1/collections/{collectionId}/elements/{elementId}. Uses test profile with in-memory SQLite
 * database that is cleaned after each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CollectionElementMembershipTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private ElementRepository elementRepository;

  @Autowired private ElementVersionRepository versionRepository;

  @Autowired private CollectionRepository collectionRepository;

  @Autowired private ElementCollectionMembershipRepository membershipRepository;

  private UUID collection1Id;
  private UUID collection2Id;
  private UUID element1Id;
  private UUID element2Id;

  @BeforeEach
  void setup() {
    // Create test collections
    CreateCollectionRequestDto collection1 =
        new CreateCollectionRequestDto(
            "Collection 1", "First test collection", "LIST", null, null, null, null, null, true);
    EntityExchangeResult<CollectionResponseDto> response1 =
        restTestClient.post().uri("/api/v1/collections").body(collection1).exchange().expectBody(CollectionResponseDto.class).returnResult();
    collection1Id = response1.getResponseBody().collectionId();

    CreateCollectionRequestDto collection2 =
        new CreateCollectionRequestDto(
            "Collection 2", "Second test collection", "LIST", null, null, null, null, null, true);
    EntityExchangeResult<CollectionResponseDto> response2 =
        restTestClient.post().uri("/api/v1/collections").body(collection2).exchange().expectBody(CollectionResponseDto.class).returnResult();
    collection2Id = response2.getResponseBody().collectionId();

    // Create test elements in collection 1
    CreateElementRequestDto elementRequest1 =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Element 1\"}");
    EntityExchangeResult<ElementResponseDto> elementResponse1 =
        restTestClient.post().uri("/api/v1/collections/" + collection1Id + "/elements").body(elementRequest1).exchange().expectBody(ElementResponseDto.class).returnResult();
    element1Id = elementResponse1.getResponseBody().elementId();

    CreateElementRequestDto elementRequest2 =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Element 2\"}");
    EntityExchangeResult<ElementResponseDto> elementResponse2 =
        restTestClient.post().uri("/api/v1/collections/" + collection1Id + "/elements").body(elementRequest2).exchange().expectBody(ElementResponseDto.class).returnResult();
    element2Id = elementResponse2.getResponseBody().elementId();
  }

  @AfterEach
  void cleanup() {
    // Clean up test data after each test
    membershipRepository.deleteAll();
    versionRepository.deleteAll();
    elementRepository.deleteAll();
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);
  }

  // ==================== GET /api/v1/collections/{collectionId}/elements Tests ====================

  @SuppressWarnings("unchecked")
  @Test
  void shouldListElementsInCollection() {
    // Given: Elements exist in collection1
    // (setup already created 2 elements in collection1)

    // When: Requesting elements in collection1
    EntityExchangeResult<Map> response =
        restTestClient.get().uri("/api/v1/collections/" + collection1Id + "/elements?page=0&size=20").exchange().expectBody(Map.class).returnResult();

    // Then: Returns paginated list of elements
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat((Map<String, Object>) response.getResponseBody())
        .containsKeys("content", "page")
        .extractingByKey("page", InstanceOfAssertFactories.MAP)
        .containsKeys("size", "number", "totalElements", "totalPages");

    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(2);
    assertThat(content.get(0)).containsKey("elementId");
    assertThat(content.get(0)).containsKey("elementTypeName");
  }

  @Test
  void shouldReturnEmptyListForCollectionWithNoElements() {
    // Given: collection2 has no elements
    // When: Requesting elements in collection2
    EntityExchangeResult<Map> response =
        restTestClient.get().uri("/api/v1/collections/" + collection2Id + "/elements?page=0&size=20").exchange().expectBody(Map.class).returnResult();

    // Then: Returns empty list
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).isEmpty();
  }

  @Test
  void shouldReturn404WhenListingElementsForNonExistentCollection() {
    // Given: A non-existent collection UUID
    UUID unknownCollectionId = UUID.randomUUID();

    // When: Requesting elements for non-existent collection
    EntityExchangeResult<Map> response =
        restTestClient.get().uri("/api/v1/collections/" + unknownCollectionId + "/elements").exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== POST /api/v1/collections/{collectionId}/elements/{elementId} Tests
  // ====================

  @Test
  void shouldAddExistingElementToCollection() {
    // Given: element1 exists in collection1, collection2 exists
    // When: Adding element1 to collection2
    EntityExchangeResult<Void> response =
        restTestClient.post().uri("/api/v1/collections/" + collection2Id + "/elements/" + element1Id).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 204 No Content
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: element1 is now in both collections
    EntityExchangeResult<Map> collection1Elements =
        restTestClient.get().uri("/api/v1/collections/" + collection1Id + "/elements").exchange().expectBody(Map.class).returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> collection1Content =
        (List<Map<String, Object>>) collection1Elements.getResponseBody().get("content");
    assertThat(collection1Content).hasSize(2); // Still has element1 and element2

    EntityExchangeResult<Map> collection2Elements =
        restTestClient.get().uri("/api/v1/collections/" + collection2Id + "/elements").exchange().expectBody(Map.class).returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> collection2Content =
        (List<Map<String, Object>>) collection2Elements.getResponseBody().get("content");
    assertThat(collection2Content).hasSize(1); // Now has element1
    assertThat(collection2Content.get(0).get("elementId")).isEqualTo(element1Id.toString());
  }

  @Test
  void shouldBeIdempotentWhenAddingElementAlreadyInCollection() {
    // Given: element1 already exists in collection1
    // When: Adding element1 to collection1 again
    EntityExchangeResult<Void> response =
        restTestClient.post().uri("/api/v1/collections/" + collection1Id + "/elements/" + element1Id).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 204 No Content (idempotent)
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: element1 is still in collection1 (no duplicate memberships)
    EntityExchangeResult<Map> collection1Elements =
        restTestClient.get().uri("/api/v1/collections/" + collection1Id + "/elements").exchange().expectBody(Map.class).returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) collection1Elements.getResponseBody().get("content");
    assertThat(content).hasSize(2); // Still just 2 elements, not 3
  }

  @Test
  void shouldReturn404WhenAddingNonExistentElement() {
    // Given: A non-existent element UUID
    UUID unknownElementId = UUID.randomUUID();

    // When: Attempting to add non-existent element to collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri("/api/v1/collections/" + collection1Id + "/elements/" + unknownElementId).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenAddingElementToNonExistentCollection() {
    // Given: A non-existent collection UUID
    UUID unknownCollectionId = UUID.randomUUID();

    // When: Attempting to add element to non-existent collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri("/api/v1/collections/" + unknownCollectionId + "/elements/" + element1Id).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== DELETE /api/v1/collections/{collectionId}/elements/{elementId} Tests
  // ====================

  @Test
  void shouldRemoveElementFromCollection() {
    // Given: element1 exists in both collection1 and collection2
    restTestClient.post().uri("/api/v1/collections/" + collection2Id + "/elements/" + element1Id).exchange().expectBody(Void.class).returnResult();

    // When: Removing element1 from collection1
    EntityExchangeResult<Void> response =
        restTestClient.delete().uri("/api/v1/collections/" + collection1Id + "/elements/" + element1Id).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 204 No Content
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: element1 is no longer in collection1
    EntityExchangeResult<Map> collection1Elements =
        restTestClient.get().uri("/api/v1/collections/" + collection1Id + "/elements").exchange().expectBody(Map.class).returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) collection1Elements.getResponseBody().get("content");
    assertThat(content).hasSize(1); // Only element2 remains

    // But: element1 still exists because it's still in collection2
    EntityExchangeResult<ElementResponseDto> element1Response =
        restTestClient.get().uri("/api/v1/elements/" + element1Id).exchange().expectBody(ElementResponseDto.class).returnResult();
    assertThat(element1Response.getStatus()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void shouldDeleteElementWhenRemovedFromLastCollection() {
    // Given: element2 exists only in collection1
    // When: Removing element2 from its only collection
    EntityExchangeResult<Void> response =
        restTestClient.delete().uri("/api/v1/collections/" + collection1Id + "/elements/" + element2Id).exchange().expectBody(Void.class).returnResult();

    // Then: Returns 204 No Content
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // And: element2 is no longer in collection1
    EntityExchangeResult<Map> collection1Elements =
        restTestClient.get().uri("/api/v1/collections/" + collection1Id + "/elements").exchange().expectBody(Map.class).returnResult();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) collection1Elements.getResponseBody().get("content");
    assertThat(content).hasSize(1); // Only element1 remains

    // And: element2 no longer exists (deleted because it had no collections left)
    EntityExchangeResult<Map> element2Response =
        restTestClient.get().uri("/api/v1/elements/" + element2Id).exchange().expectBody(Map.class).returnResult();
    assertThat(element2Response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenRemovingElementNotInCollection() {
    // Given: element1 exists but is not in collection2
    // When: Attempting to remove element1 from collection2
    EntityExchangeResult<Map> response =
        restTestClient.delete().uri("/api/v1/collections/" + collection2Id + "/elements/" + element1Id).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenRemovingFromNonExistentCollection() {
    // Given: A non-existent collection UUID
    UUID unknownCollectionId = UUID.randomUUID();

    // When: Attempting to remove element from non-existent collection
    EntityExchangeResult<Map> response =
        restTestClient.delete().uri("/api/v1/collections/" + unknownCollectionId + "/elements/" + element1Id).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void shouldReturn404WhenRemovingNonExistentElement() {
    // Given: A non-existent element UUID
    UUID unknownElementId = UUID.randomUUID();

    // When: Attempting to remove non-existent element
    EntityExchangeResult<Map> response =
        restTestClient.delete().uri("/api/v1/collections/" + collection1Id + "/elements/" + unknownElementId).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 404
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ==================== Element Type Restriction Tests ====================

  @Test
  void shouldReturn422WhenCreatingElementWithDisallowedType() {
    // Given: A collection that only allows "xpository:document:generic" type
    CreateCollectionRequestDto restrictedCollection = new CreateCollectionRequestDto();
    restrictedCollection.setName("Documents Only");
    restrictedCollection.setDescription("Collection that only accepts documents");
    restrictedCollection.setType("LIST");
    restrictedCollection.setAllowedElementTypes(Set.of("xpository:document:generic"));
    restrictedCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(restrictedCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID restrictedCollectionId = collectionResponse.getResponseBody().collectionId();

    // And: A request to create a "xpository:core:standard" element (not allowed)
    CreateElementRequestDto elementRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Standard Element\"}");

    // When: Attempting to create element in restricted collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + restrictedCollectionId + "/elements").body(elementRequest).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 422 Unprocessable Entity
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(response.getResponseBody()).containsKey("message");
    assertThat(response.getResponseBody().get("message").toString())
        .contains("xpository:core:standard")
        .contains("not allowed");
  }

  @Test
  void shouldAllowCreatingElementWithAllowedType() {
    // Given: A collection that only allows "xpository:document:generic" type
    CreateCollectionRequestDto restrictedCollection = new CreateCollectionRequestDto();
    restrictedCollection.setName("Documents Only");
    restrictedCollection.setDescription("Collection that only accepts documents");
    restrictedCollection.setType("LIST");
    restrictedCollection.setAllowedElementTypes(Set.of("xpository:document:generic"));
    restrictedCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(restrictedCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID restrictedCollectionId = collectionResponse.getResponseBody().collectionId();

    // And: A request to create a "xpository:document:generic" element (allowed)
    CreateElementRequestDto elementRequest =
        new CreateElementRequestDto(
            "xpository:document:generic",
            "{\"title\": \"Document Element\", \"author\": \"John Doe\"}");

    // When: Creating element in restricted collection
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + restrictedCollectionId + "/elements").body(elementRequest).exchange().expectBody(ElementResponseDto.class).returnResult();

    // Then: Returns 201 Created
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().elementTypeName()).isEqualTo("xpository:document:generic");
  }

  @Test
  void shouldReturn422WhenAddingExistingElementWithDisallowedType() {
    // Given: A collection that only allows "xpository:document:generic" type
    CreateCollectionRequestDto restrictedCollection = new CreateCollectionRequestDto();
    restrictedCollection.setName("Documents Only");
    restrictedCollection.setDescription("Collection that only accepts documents");
    restrictedCollection.setType("LIST");
    restrictedCollection.setAllowedElementTypes(Set.of("xpository:document:generic"));
    restrictedCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(restrictedCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID restrictedCollectionId = collectionResponse.getResponseBody().collectionId();

    // And: element1 exists with type "xpository:core:standard" (not allowed)
    // When: Attempting to add element1 to restricted collection
    EntityExchangeResult<Map> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + restrictedCollectionId + "/elements/" + element1Id).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 422 Unprocessable Entity
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(response.getResponseBody()).containsKey("message");
    assertThat(response.getResponseBody().get("message").toString())
        .contains("xpository:core:standard")
        .contains("not allowed");
  }

  @Test
  void shouldAllowAnyTypeWhenNoRestrictions() {
    // Given: collection1 has no element type restrictions (setup created it without restrictions)
    // And: A request to create any element type
    CreateElementRequestDto elementRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Any Type\"}");

    // When: Creating element in unrestricted collection
    EntityExchangeResult<ElementResponseDto> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + collection1Id + "/elements").body(elementRequest).exchange().expectBody(ElementResponseDto.class).returnResult();

    // Then: Returns 201 Created
    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
  }

  @Test
  void shouldAssignSequentialOrderPositionsInListCollection() {
    // Given: A LIST collection
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setDescription("List with ordered elements");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(listCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // When: Adding 3 elements to the LIST collection
    CreateElementRequestDto element1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"First\"}");
    CreateElementRequestDto element2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Second\"}");
    CreateElementRequestDto element3Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Third\"}");

    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements").body(element1Request).exchange().expectBody(ElementResponseDto.class).returnResult();
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements").body(element2Request).exchange().expectBody(ElementResponseDto.class).returnResult();
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements").body(element3Request).exchange().expectBody(ElementResponseDto.class).returnResult();

    // Then: Elements should have sequential order positions 0, 1, 2
    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);

    assertThat(memberships).hasSize(3);
    assertThat(memberships).extracting("orderPosition").containsExactlyInAnyOrder(0, 1, 2);
    assertThat(memberships).extracting("depth").containsOnly(0); // All at root level (depth 0)
  }

  @Test
  void shouldCalculateDepthForNestedListElements() {
    // Given: A LIST collection
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Nested List");
    listCollection.setDescription("List with nested elements");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri(
            "/api/v1/collections").body(listCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // When: Creating a hierarchy of elements
    // Root element (depth 0)
    CreateElementRequestDto rootRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Root\"}");
    EntityExchangeResult<ElementResponseDto> rootResponse =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements").body(rootRequest).exchange().expectBody(ElementResponseDto.class).returnResult();
    UUID rootId = rootResponse.getResponseBody().elementId();

    // Child element under root (depth 1)
    CreateElementRequestDto child1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 1\"}", rootId);
    EntityExchangeResult<ElementResponseDto> child1Response =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements").body(child1Request).exchange().expectBody(ElementResponseDto.class).returnResult();
    UUID child1Id = child1Response.getResponseBody().elementId();

    // Grandchild element under child1 (depth 2)
    CreateElementRequestDto grandchildRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Grandchild\"}", child1Id);
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements").body(grandchildRequest).exchange().expectBody(ElementResponseDto.class).returnResult();

    // Another child under root (depth 1)
    CreateElementRequestDto child2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Child 2\"}", rootId);
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements").body(child2Request).exchange().expectBody(ElementResponseDto.class).returnResult();

    // Then: Elements should have correct depth values
    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);

    assertThat(memberships).hasSize(4);

    // Root element has depth 0
    var rootMembership =
        memberships.stream()
            .filter(m -> m.getElement().getElementId().equals(rootId))
            .findFirst()
            .orElseThrow();
    assertThat(rootMembership.getDepth()).isEqualTo(0);
    assertThat(rootMembership.getParentElement()).isNull();

    // Child elements have depth 1
    var child1Membership =
        memberships.stream()
            .filter(m -> m.getElement().getElementId().equals(child1Id))
            .findFirst()
            .orElseThrow();
    assertThat(child1Membership.getDepth()).isEqualTo(1);
    assertThat(child1Membership.getParentElement().getElementId()).isEqualTo(rootId);

    // Count elements at each depth level
    long depth0Count = memberships.stream().filter(m -> m.getDepth() == 0).count();
    long depth1Count = memberships.stream().filter(m -> m.getDepth() == 1).count();
    long depth2Count = memberships.stream().filter(m -> m.getDepth() == 2).count();

    assertThat(depth0Count).isEqualTo(1); // 1 root
    assertThat(depth1Count).isEqualTo(2); // 2 children
    assertThat(depth2Count).isEqualTo(1); // 1 grandchild
  }

  @Test
  void shouldReturn422WhenParentElementNotInSameCollection() {
    // Given: Two separate LIST collections
    CreateCollectionRequestDto collection1Request = new CreateCollectionRequestDto();
    collection1Request.setName("List 1");
    collection1Request.setDescription("First list");
    collection1Request.setType("LIST");
    collection1Request.setInheritPermissions(true);

    CreateCollectionRequestDto collection2Request = new CreateCollectionRequestDto();
    collection2Request.setName("List 2");
    collection2Request.setDescription("Second list");
    collection2Request.setType("LIST");
    collection2Request.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collection1Response =
        restTestClient.post().uri("/api/v1/collections").body(collection1Request).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID listCollection1Id = collection1Response.getResponseBody().collectionId();

    EntityExchangeResult<CollectionResponseDto> collection2Response =
        restTestClient.post().uri("/api/v1/collections").body(collection2Request).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID listCollection2Id = collection2Response.getResponseBody().collectionId();

    // And: An element exists in collection1
    CreateElementRequestDto parentRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Parent in List 1\"}");
    EntityExchangeResult<ElementResponseDto> parentResponse =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollection1Id + "/elements").body(parentRequest).exchange().expectBody(ElementResponseDto.class).returnResult();
    UUID parentId = parentResponse.getResponseBody().elementId();

    // When: Attempting to create an element in collection2 with parent from collection1
    CreateElementRequestDto childRequest =
        new CreateElementRequestDto(
            "xpository:core:standard", "{\"title\": \"Child in List 2\"}", parentId);
    @SuppressWarnings("rawtypes")
    EntityExchangeResult<Map> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollection2Id + "/elements").body(childRequest).exchange().expectBody(Map.class).returnResult();

    // Then: Returns 422 Unprocessable Entity
    assertThat(response.getStatus().value()).isEqualTo(422);
    assertThat(response.getResponseBody()).containsKey("message");
    assertThat(response.getResponseBody().get("message").toString())
        .contains("Parent element must be in the same collection");
  }

  @Test
  void shouldMoveElementUpInListCollection() {
    // Given: A LIST collection with 3 elements
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections").body(listCollection).exchange().expectBody(CollectionResponseDto.class).returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // Create 3 elements
    CreateElementRequestDto element1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"First\"}");
    CreateElementRequestDto element2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Second\"}");
    CreateElementRequestDto element3Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Third\"}");

    // Create elements in the collection
    // Create element 1
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements").body(element1Request).exchange().expectBody(ElementResponseDto.class).returnResult();
    // Create element 2
    EntityExchangeResult<ElementResponseDto> el2 =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements").body(element2Request).exchange().expectBody(ElementResponseDto.class).returnResult();
    // Create element 3
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements").body(element3Request).exchange().expectBody(ElementResponseDto.class).returnResult();

    UUID _element2Id = el2.getResponseBody().elementId();

    // When: Moving element 2 (position 1) up
    EntityExchangeResult<Void> response =
        restTestClient.post().uri(
            "/api/v1/collections/" + listCollectionId + "/elements/" + _element2Id + "/move-up")
            .exchange()
            .expectStatus().isNoContent()
            .returnResult(Void.class);

    // Then: Returns 204 and element 2 is now at position 0
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    assertThat(memberships).hasSize(3);
    assertThat(memberships.get(0).getElement().getElementId()).isEqualTo(_element2Id); // Moved up
    assertThat(memberships.get(0).getOrderPosition()).isEqualTo(0);
    assertThat(memberships.get(1).getOrderPosition()).isEqualTo(1);
    assertThat(memberships.get(2).getOrderPosition()).isEqualTo(2);
  }

  @Test
  void shouldReturn200WhenMovingElementUpAtTop() {
    // Given: A LIST collection with 2 elements
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(listCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    CreateElementRequestDto element1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"First\"}");
    EntityExchangeResult<ElementResponseDto> el1 =
        restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
            .body(element1Request)
            .exchange()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    UUID _element1Id = el1.getResponseBody().elementId();

    // When: Trying to move the first element (position 0) up
    // Then: Returns 200 (already at top)
    restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements/" + _element1Id + "/move-up")
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();
  }

  @Test
  void shouldMoveElementDownInListCollection() {
    // Given: A LIST collection with 3 elements
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(listCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    CreateElementRequestDto element1Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"First\"}");
    CreateElementRequestDto element2Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Second\"}");
    CreateElementRequestDto element3Request =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Third\"}");

        restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
            .body(element1Request)
            .exchange()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    EntityExchangeResult<ElementResponseDto> el2 =
        restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
            .body(element2Request)
            .exchange()
            .expectBody(ElementResponseDto.class)
            .returnResult();
        restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
            .body(element3Request)
            .exchange()
            .expectBody(ElementResponseDto.class)
            .returnResult();

    UUID _element2Id = el2.getResponseBody().elementId();

    // When: Moving element 2 (position 1) down
    EntityExchangeResult<Void> response =
        restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements/" + _element2Id + "/move-down")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class)
            .returnResult();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    // Then: Returns 204 and element 2 is now at position 2
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    assertThat(memberships).hasSize(3);
    assertThat(memberships.get(2).getElement().getElementId()).isEqualTo(_element2Id); // Moved down
    assertThat(memberships.get(0).getOrderPosition()).isEqualTo(0);
    assertThat(memberships.get(1).getOrderPosition()).isEqualTo(1);
    assertThat(memberships.get(2).getOrderPosition()).isEqualTo(2);
  }

  @Test
  void shouldReorderElementToSpecificPosition() {
    // Given: A LIST collection with 5 elements
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(listCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // Create 5 elements
    UUID[] elementIds = new UUID[5];
    for (int i = 0; i < 5; i++) {
      CreateElementRequestDto elementRequest =
          new CreateElementRequestDto(
              "xpository:core:standard", "{\"title\": \"Element " + i + "\"}");
      EntityExchangeResult<ElementResponseDto> el =
          restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
              .body(elementRequest)
              .exchange()
              .expectBody(ElementResponseDto.class)
              .returnResult();
      elementIds[i] = el.getResponseBody().elementId();
    }

    // Element at position 1 (second element)
    UUID _element1Id = elementIds[1];

    // When: Reordering element from position 1 to position 3
    EntityExchangeResult<Void> response =
        restTestClient.post().uri(
            "/api/v1/collections/"
                + listCollectionId
                + "/elements/"
                + _element1Id
                + "/reorder?position=3")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 204 and positions are correct
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    assertThat(memberships).hasSize(5);
    // Element that was at position 1 should now be at position 3
    assertThat(memberships.get(3).getElement().getElementId()).isEqualTo(_element1Id);
    // All positions should be sequential
    for (int i = 0; i < 5; i++) {
      assertThat(memberships.get(i).getOrderPosition()).isEqualTo(i);
    }
  }

  @Test
  void shouldReturn422WhenReorderingInNonListCollection() {
    // Given: A TREE collection
    CreateCollectionRequestDto treeCollection = new CreateCollectionRequestDto();
    treeCollection.setName("Tree Collection");
    treeCollection.setType("TREE");
    treeCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(treeCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID treeCollectionId = collectionResponse.getResponseBody().collectionId();

    CreateElementRequestDto elementRequest =
        new CreateElementRequestDto("xpository:core:standard", "{\"title\": \"Element\"}");
    EntityExchangeResult<ElementResponseDto> el =
        restTestClient.post().uri("/api/v1/collections/" + treeCollectionId + "/elements")
            .body(elementRequest)
            .exchange()
            .expectBody(ElementResponseDto.class)
            .returnResult();
    UUID elementId = el.getResponseBody().elementId();

    // When: Trying to move element up in TREE collection
    @SuppressWarnings("rawtypes")
    EntityExchangeResult<Map> response =
        restTestClient.post().uri("/api/v1/collections/" + treeCollectionId + "/elements/" + elementId + "/move-up")
            .exchange()
            .expectBody(Map.class)
            .returnResult();

    // Then: Returns 422
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    assertThat(response.getResponseBody().get("message").toString())
        .contains("Reordering is only supported for LIST collections");
  }

  @Test
  void shouldCloseGapWhenRemovingElementFromList() {
    // Given: A LIST collection with 5 elements
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(listCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // Create 5 elements (positions 0, 1, 2, 3, 4)
    UUID[] elementIds = new UUID[5];
    for (int i = 0; i < 5; i++) {
      CreateElementRequestDto elementRequest =
          new CreateElementRequestDto(
              "xpository:core:standard", "{\"title\": \"Element " + i + "\"}");
      EntityExchangeResult<ElementResponseDto> el =
          restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
              .body(elementRequest)
              .exchange()
              .expectBody(ElementResponseDto.class)
              .returnResult();
      elementIds[i] = el.getResponseBody().elementId();
    }

    // When: Removing element at position 2 (middle element)
    // First add element to another collection so it doesn't get deleted
    CreateCollectionRequestDto collection2Request = new CreateCollectionRequestDto();
    collection2Request.setName("Collection 2");
    collection2Request.setType("LIST");
    collection2Request.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collection2Response =
        restTestClient.post().uri("/api/v1/collections")
            .body(collection2Request)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID _collection2Id = collection2Response.getResponseBody().collectionId();

    // Add element at position 2 to collection2 so it won't be deleted
    restTestClient.post().uri("/api/v1/collections/" + _collection2Id + "/elements/" + elementIds[2])
        .exchange()
        .expectStatus().isNoContent();

    // Now remove it from the first collection
    EntityExchangeResult<Void> removeResponse =
        restTestClient.delete().uri("/api/v1/collections/" + listCollectionId + "/elements/" + elementIds[2])
            .exchange()
            .expectBody(Void.class)
            .returnResult();

    // Then: Returns 204 and gap is closed
    assertThat(removeResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    // Should have 4 elements now (removed 1)
    assertThat(memberships).hasSize(4);

    // Positions should be sequential 0, 1, 2, 3 (no gap)
    for (int i = 0; i < 4; i++) {
      assertThat(memberships.get(i).getOrderPosition()).isEqualTo(i);
    }

    // Element 0 should still be at position 0
    assertThat(memberships.get(0).getElement().getElementId()).isEqualTo(elementIds[0]);
    // Element 1 should still be at position 1
    assertThat(memberships.get(1).getElement().getElementId()).isEqualTo(elementIds[1]);
    // Element 3 should now be at position 2 (shifted up from 3)
    assertThat(memberships.get(2).getElement().getElementId()).isEqualTo(elementIds[3]);
    // Element 4 should now be at position 3 (shifted up from 4)
    assertThat(memberships.get(3).getElement().getElementId()).isEqualTo(elementIds[4]);
  }

  @Test
  void shouldMaintainSequentialOrderAfterMultipleOperations() {
    // Given: A LIST collection
    CreateCollectionRequestDto listCollection = new CreateCollectionRequestDto();
    listCollection.setName("Ordered List");
    listCollection.setType("LIST");
    listCollection.setInheritPermissions(true);

    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections")
            .body(listCollection)
            .exchange()
            .expectBody(CollectionResponseDto.class)
            .returnResult();
    UUID listCollectionId = collectionResponse.getResponseBody().collectionId();

    // Create 5 elements
    UUID[] elementIds = new UUID[5];
    for (int i = 0; i < 5; i++) {
      CreateElementRequestDto elementRequest =
          new CreateElementRequestDto(
              "xpository:core:standard", "{\"title\": \"Element " + i + "\"}");
      EntityExchangeResult<ElementResponseDto> el =
          restTestClient.post().uri("/api/v1/collections/" + listCollectionId + "/elements")
              .body(elementRequest)
              .exchange()
              .expectBody(ElementResponseDto.class)
              .returnResult();
      elementIds[i] = el.getResponseBody().elementId();
    }

    // When: Performing multiple operations
    // 1. Move element 1 down (0,1,2,3,4 -> 0,2,1,3,4)
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements/" + elementIds[1] + "/move-down")
        .exchange()
        .expectStatus().isNoContent();

    // 2. Move element 4 up (0,2,1,3,4 -> 0,2,1,4,3)
    restTestClient.post().uri(
        "/api/v1/collections/" + listCollectionId + "/elements/" + elementIds[4] + "/move-up")
        .exchange()
        .expectStatus().isNoContent();

    // 3. Reorder element at current position 0 to position 2 (0,2,1,4,3 -> 2,1,0,4,3)
    restTestClient.post().uri(
        "/api/v1/collections/"
            + listCollectionId
            + "/elements/"
            + elementIds[0]
            + "/reorder?position=2")
        .exchange()
        .expectStatus().isNoContent()
        .expectBody().isEmpty();

    // Then: All positions should still be sequential 0, 1, 2, 3, 4
    var collection = collectionRepository.findByCollectionId(listCollectionId).orElseThrow();
    var memberships = membershipRepository.findByCollection(collection);
    memberships.sort((a, b) -> Integer.compare(a.getOrderPosition(), b.getOrderPosition()));

    assertThat(memberships).hasSize(5);
    for (int i = 0; i < 5; i++) {
      assertThat(memberships.get(i).getOrderPosition()).isEqualTo(i);
    }
  }
}
