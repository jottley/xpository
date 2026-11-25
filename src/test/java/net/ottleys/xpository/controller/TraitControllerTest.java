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
import net.ottleys.xpository.dto.CreateTraitRequestDto;
import net.ottleys.xpository.dto.TraitResponseDto;
import net.ottleys.xpository.dto.UpdateTraitRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Integration tests for TraitController. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TraitControllerTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void createTrait_shouldReturnCreated() {
    String uniqueName = "Test Classification " + UUID.randomUUID();
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName(uniqueName);
    request.setDescription("Test description");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);
    request.setAllowedValuesJson("[\"Low\", \"Medium\", \"High\"]");
    request.setDefaultValue("Low");
    request.setRequired(false);
    request.setInheritable(true);

    ResponseEntity<TraitResponseDto> response =
        restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getName()).isEqualTo(uniqueName);
    assertThat(response.getBody().getTraitId()).isNotNull();
  }

  @Test
  void createTrait_withDuplicateName_shouldReturnConflict() {
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    String dupName = "Duplicate Trait " + UUID.randomUUID();
    request.setName(dupName);
    request.setDescription("First");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);

    restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    ResponseEntity<TraitResponseDto> response =
        restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    assertThat(response.getStatusCode().value()).isEqualTo(422);
  }

  @Test
  void getTraitById_shouldReturnTrait() {
    String traitName = "Retrievable Trait " + UUID.randomUUID();
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName(traitName);
    request.setDescription("Test");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.NUMBER);

    ResponseEntity<TraitResponseDto> createResponse =
        restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);
    UUID traitId = createResponse.getBody().getTraitId();

    ResponseEntity<TraitResponseDto> response =
        restTemplate.getForEntity("/api/v1/traits/" + traitId, TraitResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getName()).isEqualTo(traitName);
  }

  @Test
  void getTraitById_withNonexistentId_shouldReturnNotFound() {
    UUID nonexistentId = UUID.randomUUID();

    ResponseEntity<TraitResponseDto> response =
        restTemplate.getForEntity("/api/v1/traits/" + nonexistentId, TraitResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void searchTraits_shouldReturnMatchingTraits() {
    CreateTraitRequestDto request1 = new CreateTraitRequestDto();
    request1.setName("Security Level " + UUID.randomUUID());
    request1.setDescription("Security classification");
    request1.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);

    CreateTraitRequestDto request2 = new CreateTraitRequestDto();
    request2.setName("Security Zone " + UUID.randomUUID());
    request2.setDescription("Physical security zone");
    request2.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);

    restTemplate.postForEntity("/api/v1/traits", request1, TraitResponseDto.class);
    restTemplate.postForEntity("/api/v1/traits", request2, TraitResponseDto.class);

    ResponseEntity<TraitResponseDto[]> response =
        restTemplate.getForEntity("/api/v1/traits/search?name=Security", TraitResponseDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(2);
  }

  @Test
  void getTraitsByType_shouldReturnTraitsOfType() {
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName("Boolean Trait " + UUID.randomUUID());
    request.setDescription("Test boolean");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.BOOLEAN);

    restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    ResponseEntity<TraitResponseDto[]> response =
        restTemplate.getForEntity("/api/v1/traits/type/BOOLEAN", TraitResponseDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(1);
  }

  @Test
  void updateTrait_shouldReturnUpdated() {
    CreateTraitRequestDto createRequest = new CreateTraitRequestDto();
    createRequest.setName("Updatable Trait " + UUID.randomUUID());
    createRequest.setDescription("Original description");
    createRequest.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);

    ResponseEntity<TraitResponseDto> createResponse =
        restTemplate.postForEntity("/api/v1/traits", createRequest, TraitResponseDto.class);
    UUID traitId = createResponse.getBody().getTraitId();

    UpdateTraitRequestDto updateRequest = new UpdateTraitRequestDto();
    updateRequest.setDescription("Updated description");

    ResponseEntity<TraitResponseDto> response =
        restTemplate.exchange(
            "/api/v1/traits/" + traitId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest),
            TraitResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getDescription()).isEqualTo("Updated description");
  }

  @Test
  void deleteTrait_shouldReturnNoContent() {
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName("Deletable Trait " + UUID.randomUUID());
    request.setDescription("Test");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);

    ResponseEntity<TraitResponseDto> createResponse =
        restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);
    UUID traitId = createResponse.getBody().getTraitId();

    ResponseEntity<Void> response =
        restTemplate.exchange("/api/v1/traits/" + traitId, HttpMethod.DELETE, null, Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<TraitResponseDto> getResponse =
        restTemplate.getForEntity("/api/v1/traits/" + traitId, TraitResponseDto.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void getRequiredTraits_shouldReturnRequiredTraits() {
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName("Required Trait " + UUID.randomUUID());
    request.setDescription("Required");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);
    request.setRequired(true);

    restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    ResponseEntity<TraitResponseDto[]> response =
        restTemplate.getForEntity("/api/v1/traits/required", TraitResponseDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(1);
  }

  @Test
  void getInheritableTraits_shouldReturnInheritableTraits() {
    CreateTraitRequestDto request = new CreateTraitRequestDto();
    request.setName("Inheritable Trait " + UUID.randomUUID());
    request.setDescription("Inheritable");
    request.setTraitType(net.ottleys.xpository.domain.Trait.TraitType.STRING);
    request.setInheritable(true);

    restTemplate.postForEntity("/api/v1/traits", request, TraitResponseDto.class);

    ResponseEntity<TraitResponseDto[]> response =
        restTemplate.getForEntity("/api/v1/traits/inheritable", TraitResponseDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(1);
  }
}
