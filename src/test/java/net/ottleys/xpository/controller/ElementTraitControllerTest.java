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

import jakarta.persistence.EntityManager;
import java.util.UUID;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.dto.AssignTraitRequestDto;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.CreateTraitRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.ElementTraitResponseDto;
import net.ottleys.xpository.dto.TraitResponseDto;
import net.ottleys.xpository.dto.UpdateTraitValueRequestDto;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for ElementTraitController. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ElementTraitControllerTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private ElementTypeRepository elementTypeRepository;
  @Autowired private EntityManager entityManager;

  @BeforeEach
  @Transactional
  void setUp() {
    // Manually create element type since schema loading from JSON files
    // doesn't work properly in test profile
    if (elementTypeRepository.findByName("xpository:core:standard").isEmpty()) {
      ElementType type = new ElementType();
      type.setName("xpository:core:standard");
      type.setDescription("Standard element type for testing");
      type.setSchemaJson("{}");
      elementTypeRepository.save(type);
      // Flush to ensure it's committed before HTTP requests
      entityManager.flush();
    }
  }

  /**
   * Helper method to create test data (collection, element, and trait).
   *
   * @return array containing [collectionId, elementId, traitId]
   */
  private UUID[] createTestData() {
    // Create a collection
    CreateCollectionRequestDto collectionRequest =
        new net.ottleys.xpository.dto.CreateCollectionRequestDto(
            "Test Collection " + UUID.randomUUID(),
            "Collection for trait tests",
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

    assertThat(collectionResponse.getStatus())
        .as(
            "Collection creation failed with status: %s, body: %s",
            collectionResponse.getStatus(), collectionResponse.getResponseBody())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(collectionResponse.getResponseBody()).isNotNull();
    UUID collectionId = collectionResponse.getResponseBody().collectionId();

    // Create an element in the collection
    CreateElementRequestDto elementRequest = new CreateElementRequestDto();
    elementRequest.setElementTypeName("xpository:core:standard");
    elementRequest.setPropertiesJson("{\"title\":\"Test Title\"}");

    EntityExchangeResult<ElementResponseDto> elementResponse =
        restTestClient.post()
            .uri("/api/v1/collections/" + collectionId + "/elements")
            .body(elementRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementResponseDto.class).returnResult();

    assertThat(elementResponse.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(elementResponse.getResponseBody()).isNotNull();
    UUID elementId = elementResponse.getResponseBody().elementId();
    assertThat(elementId).isNotNull();

    // Verify element exists by fetching it
    EntityExchangeResult<ElementResponseDto> verifyResponse =
        restTestClient.get()
        .uri("/api/v1/elements/" + elementId)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ElementResponseDto.class).returnResult();

    assertThat(verifyResponse.getStatus())
        .as("Element should exist immediately after creation")
        .isEqualTo(HttpStatus.OK);

    // Create a trait
    CreateTraitRequestDto traitRequest = new CreateTraitRequestDto();
    traitRequest.setName("Test Trait " + UUID.randomUUID());
    traitRequest.setDescription("Test trait");
    traitRequest.setTraitType(Trait.TraitType.STRING);
    traitRequest.setAllowedValuesJson("[\"Low\", \"Medium\", \"High\"]");

    EntityExchangeResult<TraitResponseDto> traitResponse =
        restTestClient.post()
        .uri("/api/v1/traits")
        .body(traitRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(TraitResponseDto.class).returnResult();

    assertThat(traitResponse.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(traitResponse.getResponseBody()).isNotNull();
    UUID traitId = traitResponse.getResponseBody().getTraitId();
    assertThat(traitId).isNotNull();

    return new UUID[] {collectionId, elementId, traitId};
  }

  @Test
  void assignTrait_shouldReturnCreated() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("Medium");

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.post()
            .uri("/api/v1/elements/" + elementId + "/traits")
            .body(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getValue()).isEqualTo("Medium");
    assertThat(response.getResponseBody().isInherited()).isFalse();
  }

  @Test
  void assignTrait_withInvalidValue_shouldReturnBadRequest() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("Invalid");

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(request)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
        .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(422);
  }

  @Test
  void assignTrait_withDuplicateTrait_shouldReturnBadRequest() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("Low");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class).returnResult();

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.post()
            .uri("/api/v1/elements/" + elementId + "/traits")
            .body(request)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(422);
  }

  @Test
  void getElementTraits_shouldReturnAllTraits() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("High");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    EntityExchangeResult<ElementTraitResponseDto[]> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto[].class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody()).hasSizeGreaterThanOrEqualTo(1);
  }

  @Test
  void getElementTrait_shouldReturnTrait() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("Low");

    restTestClient.post()
        .uri( "/api/v1/elements/" + elementId + "/traits")
        .body(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits/" + traitId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getValue()).isEqualTo("Low");
  }

  @Test
  void getElementTrait_withNonexistentTrait_shouldReturnNotFound() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID nonexistentTraitId = UUID.randomUUID();

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits/" + nonexistentTraitId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void updateTraitValue_shouldReturnUpdated() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto assignRequest = new AssignTraitRequestDto();
    assignRequest.setTraitId(traitId);
    assignRequest.setValue("Low");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(assignRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    UpdateTraitValueRequestDto updateRequest = new UpdateTraitValueRequestDto();
    updateRequest.setValue("High");

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.put()
            .uri("/api/v1/elements/" + elementId + "/traits/" + traitId)
            .body(updateRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody().getValue()).isEqualTo("High");
  }

  @Test
  void updateTraitValue_withInvalidValue_shouldReturnBadRequest() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto assignRequest = new AssignTraitRequestDto();
    assignRequest.setTraitId(traitId);
    assignRequest.setValue("Low");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(assignRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    UpdateTraitValueRequestDto updateRequest = new UpdateTraitValueRequestDto();
    updateRequest.setValue("Invalid");

    EntityExchangeResult<ElementTraitResponseDto> response =
        restTestClient.put()
            .uri("/api/v1/elements/" + elementId + "/traits/" + traitId)
            .body(updateRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(response.getStatus().value()).isEqualTo(422);
  }

  @Test
  void removeTrait_shouldReturnNoContent() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto assignRequest = new AssignTraitRequestDto();
    assignRequest.setTraitId(traitId);
    assignRequest.setValue("Medium");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(assignRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    EntityExchangeResult<Void> response =
        restTestClient.delete()
            .uri("/api/v1/elements/" + elementId + "/traits/" + traitId)
            .exchange()
            .expectStatus().isNoContent()
            .expectBody(Void.class).returnResult();


    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    EntityExchangeResult<ElementTraitResponseDto> getResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits/" + traitId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ElementTraitResponseDto.class).returnResult();

    assertThat(getResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void removeAllTraits_shouldReturnNoContent() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto assignRequest = new AssignTraitRequestDto();
    assignRequest.setTraitId(traitId);
    assignRequest.setValue("Low");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(assignRequest)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    EntityExchangeResult<Void> response =
        restTestClient.delete()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .exchange()
        .expectStatus().isNoContent()
        .expectBody(Void.class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

    EntityExchangeResult<ElementTraitResponseDto[]> getResponse =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto[].class).returnResult();
    assertThat(getResponse.getResponseBody()).isEmpty();
  }

  @Test
  void getDirectTraits_shouldReturnOnlyDirectTraits() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];
    UUID traitId = testData[2];

    AssignTraitRequestDto request = new AssignTraitRequestDto();
    request.setTraitId(traitId);
    request.setValue("Medium");

    restTestClient.post()
        .uri("/api/v1/elements/" + elementId + "/traits")
        .body(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(ElementTraitResponseDto.class);

    EntityExchangeResult<ElementTraitResponseDto[]> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits/direct")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto[].class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
    assertThat(response.getResponseBody()).hasSizeGreaterThanOrEqualTo(1);
    assertThat(response.getResponseBody()[0].isInherited()).isFalse();
  }

  @Test
  void getInheritedTraits_shouldReturnOnlyInheritedTraits() {
    UUID[] testData = createTestData();
    UUID elementId = testData[1];

    EntityExchangeResult<ElementTraitResponseDto[]> response =
        restTestClient.get()
            .uri("/api/v1/elements/" + elementId + "/traits/inherited")
            .exchange()
            .expectStatus().isOk()
            .expectBody(ElementTraitResponseDto[].class).returnResult();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    assertThat(response.getResponseBody()).isNotNull();
  }
}
