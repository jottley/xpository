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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End‑to‑end tests that verify the {@link SearchController} correctly delegates to {@link
 * net.ottleys.xpository.service.SearchService} and that Hibernate Search (Lucene) indexes and
 * returns results.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementTypeRepository elementTypeRepository;

  private final String keyword = "uniqueKeyword";
  private Collection testCollection;
  private Element testElement;

  @BeforeEach
  void setUp() {
    // Clean DB (repositories are using an in‑memory SQLite for tests)
    elementRepository.deleteAll();
    collectionRepository.deleteBySystemCollectionFalse();
    elementTypeRepository.deleteElementTypes();

    // Get ElementType (core standard)
    Optional<ElementType> type = elementTypeRepository.findByName("xpository:core:standard");

    // Create a collection that contains the keyword in its name/description
    testCollection =
        new Collection("Test " + keyword, net.ottleys.xpository.domain.CollectionType.LIST);
    testCollection.setDescription("A collection used for integration testing " + keyword);
    testCollection.setCreatedBy("tester");
    testCollection.setModifiedBy("tester");
    testCollection = collectionRepository.save(testCollection);

    // Create an element that contains the keyword in its propertiesJson
    testElement = new Element();
    testElement.setType(type.get());
    testElement.setPropertiesJson(
        "{\"title\": \"Test Title\", \"info\": \"contains " + keyword + " inside\"}");
    testElement.setCreatedBy("tester");
    testElement.setModifiedBy("tester");
    testElement = elementRepository.save(testElement);
  }

  @Test
  void searchCollections_returnsMatchingCollection() throws Exception {
    var result =
        mockMvc
            .perform(
                get("/api/v1/search")
                    .param("q", keyword)
                    .param("type", "collections")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String json = result.getResponse().getContentAsString();
    assertThat(json).contains(testCollection.getName());
  }

  @Test
  void searchCollectionsWithCollectionsId_returnsMatchingCollection() throws Exception {
    var result =
        mockMvc
            .perform(
                get("/api/v1/search")
                    .param("q", keyword)
                    .param("type", "collections")
                    .param("collectionId", testCollection.getCollectionId().toString())
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String json = result.getResponse().getContentAsString();
    assertThat(json)
        .contains(testCollection.getName())
        .contains(testCollection.getCollectionId().toString());
  }

  @Test
  void searchElements_returnsMatchingElement() throws Exception {
    var result =
        mockMvc
            .perform(
                get("/api/v1/search")
                    .param("q", keyword)
                    .param("type", "elements")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    String json = result.getResponse().getContentAsString();
    assertThat(json).contains(testElement.getElementId().toString());
  }
}
