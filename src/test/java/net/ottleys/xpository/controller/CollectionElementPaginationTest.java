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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
import net.ottleys.xpository.repository.ElementRepository;

/** Integration test for pagination of elements within a specific collection. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CollectionElementPaginationTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private CollectionRepository collectionRepository;

  @Autowired private ElementRepository elementRepository;

  private UUID testCollectionId;

  @BeforeEach
  void setup() {
    // Create a collection for the test
    CreateCollectionRequestDto collectionRequest =
        new CreateCollectionRequestDto(
            "Pagination Test Collection",
            "Collection used for pagination integration test",
            "LIST",
            null,
            null,
            null,
            null,
            null,
            true);
    EntityExchangeResult<CollectionResponseDto> collectionResponse =
        restTestClient.post().uri("/api/v1/collections").body(collectionRequest).exchange().expectBody(CollectionResponseDto.class).returnResult();
    testCollectionId = collectionResponse.getResponseBody().collectionId();

    // Create 55 elements belonging to the collection
    for (int i = 1; i <= 55; i++) {
      CreateElementRequestDto elementRequest =
          new CreateElementRequestDto(
              "xpository:core:standard", "{\"title\": \"Element " + i + "\"}");
      restTestClient.post().uri(
          "/api/v1/collections/" + testCollectionId + "/elements").body(elementRequest).exchange().expectBody(ElementResponseDto.class).returnResult();
    }
  }

  @AfterEach
  void cleanup() {
    // Remove all elements and the collection
    elementRepository.deleteAll();
    collectionRepository.deleteBySystemCollectionFalse();
  }

  @Test
  void shouldPaginateElementsWithinCollection() {
    // Request first page (size 20)
    EntityExchangeResult<Map> page1 =
        restTestClient.get().uri(
            "/api/v1/collections/" + testCollectionId + "/elements?page=0&size=20").exchange().expectBody(Map.class).returnResult();
    assertThat(page1.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat((Map<String, Object>) page1.getResponseBody()).containsKey("content").containsKey("page");
    Map<String, Object> pageInfo1 = (Map<String, Object>) page1.getResponseBody().get("page");
    assertThat(pageInfo1)
        .containsEntry("size", 20)
        .containsEntry("number", 0)
        .containsEntry("totalElements", 55)
        .containsEntry("totalPages", 3);
    // Request second page (size 20)
    EntityExchangeResult<Map> page2 =
        restTestClient.get().uri(
            "/api/v1/collections/" + testCollectionId + "/elements?page=1&size=20").exchange().expectBody(Map.class).returnResult();
    assertThat(page2.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    Map<String, Object> pageInfo2 = (Map<String, Object>) page2.getResponseBody().get("page");
    assertThat(pageInfo2).containsEntry("size", 20).containsEntry("number", 1);
    // Request third page (size 20) – should contain remaining 15 elements
    EntityExchangeResult<Map> page3 =
        restTestClient.get().uri(
            "/api/v1/collections/" + testCollectionId + "/elements?page=2&size=20").exchange().expectBody(Map.class).returnResult();
    assertThat(page3.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    Map<String, Object> pageInfo3 = (Map<String, Object>) page3.getResponseBody().get("page");
    assertThat(pageInfo3).containsEntry("size", 20).containsEntry("number", 2);
    // Verify total elements remain consistent across pages
    assertThat(pageInfo3.get("totalElements")).isEqualTo(55);
    assertThat(pageInfo3.get("totalPages")).isEqualTo(3);
  }
}
