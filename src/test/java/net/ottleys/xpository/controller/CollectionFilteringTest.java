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
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.repository.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/**
 * Integration tests for collection filtering and search functionality.
 *
 * <p>Tests the GET /api/v1/collections endpoint with various query parameters for filtering by
 * parent, type, system flag, and name search.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CollectionFilteringTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private CollectionRepository collectionRepository;

  private Collection parentCollection;
  private Collection childCollection1;
  private Collection childCollection2;

  @BeforeEach
  void setUp() {
    // Clean up non-system test data
    collectionRepository.findAll().stream()
        .filter(c -> !c.isSystemCollection())
        .forEach(collectionRepository::delete);

    // Create parent collection (TREE type)
    parentCollection = new Collection("Parent Project", CollectionType.TREE);
    parentCollection.setDescription("Parent collection for testing");
    parentCollection.setCreatedBy("test");
    parentCollection.setModifiedBy("test");
    parentCollection = collectionRepository.save(parentCollection);

    // Create first child collection (LIST type)
    childCollection1 = new Collection("Child Alpha", CollectionType.LIST);
    childCollection1.setDescription("First child collection");
    childCollection1.setParentCollection(parentCollection);
    childCollection1.setCreatedBy("test");
    childCollection1.setModifiedBy("test");
    childCollection1 = collectionRepository.save(childCollection1);

    // Create second child collection (LIST type)
    childCollection2 = new Collection("Child Beta", CollectionType.LIST);
    childCollection2.setDescription("Second child collection");
    childCollection2.setParentCollection(parentCollection);
    childCollection2.setCreatedBy("test");
    childCollection2.setModifiedBy("test");
    childCollection2 = collectionRepository.save(childCollection2);
  }

  @Test
  void shouldFilterByParentCollection() {
    // When: Filtering by parent UUID
    String url = "/api/v1/collections?parentId=" + parentCollection.getCollectionId();
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns only children of that parent
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(2);
    assertThat(content)
        .extracting(c -> c.get("name"))
        .containsExactlyInAnyOrder("Child Alpha", "Child Beta");
  }

  @Test
  void shouldFilterByCollectionType() {
    // When: Filtering by type LIST
    String url = "/api/v1/collections?type=LIST";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns only LIST collections
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(3);
    assertThat(content)
        .extracting(c -> c.get("name"))
        .containsExactlyInAnyOrder("Child Alpha", "Child Beta", "System Root");
  }

  @Test
  void shouldSearchByName() {
    // When: Searching for collections with "child" in name
    String url = "/api/v1/collections?name=child";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns matching collections (case-insensitive)
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(2);
    assertThat(content)
        .extracting(c -> c.get("name"))
        .containsExactlyInAnyOrder("Child Alpha", "Child Beta");
  }

  @Test
  void shouldSearchByNameCaseInsensitive() {
    // When: Searching with different case
    String url = "/api/v1/collections?name=ALPHA";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns matches regardless of case
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(1);
    assertThat(content.get(0).get("name")).isEqualTo("Child Alpha");
  }

  @Test
  void shouldCombineMultipleFilters() {
    // When: Combining type and name search
    String url = "/api/v1/collections?type=LIST&name=alpha";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns collections matching both criteria
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSize(1);
    assertThat(content.get(0).get("name")).isEqualTo("Child Alpha");
  }

  @Test
  void shouldReturnEmptyWhenNoMatchesFound() {
    // When: Filtering with criteria that matches nothing
    String url = "/api/v1/collections?type=STACK";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns empty page
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).isEmpty();
  }

  @Test
  void shouldReturnAllWhenNoFiltersProvided() {
    // When: No filters provided
    String url = "/api/v1/collections";
    EntityExchangeResult<Map> response = restTestClient.get().uri(url).exchange().expectBody(Map.class).returnResult();

    // Then: Returns all non-deleted collections (our test data + System Root)
    assertThat(response.getStatus().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getResponseBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) response.getResponseBody().get("content");
    assertThat(content).hasSizeGreaterThanOrEqualTo(3); // 3 test collections + System Root
  }
}
