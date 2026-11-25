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

import java.util.UUID;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the full‑text search API.
 *
 * <p>GET /api/v1/search?q={query}&type=collections GET /api/v1/search?q={query}&type=elements
 */
@RestController
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * Search endpoint for collections and elements.
   *
   * @param query the search query string
   * @param type the type of entity to search for ("collections" or "elements")
   * @param collectionId optional collection ID to restrict element search (nullable)
   * @param pageable pagination parameters
   * @return a ResponseEntity containing the search results or an error message
   */
  @GetMapping("/api/v1/search")
  public ResponseEntity<?> search(
      @RequestParam(name = "q") String query,
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "collectionId", required = false) UUID collectionId,
      @PageableDefault(size = 50) Pageable pageable) {
    if (query == null || query.isBlank()) {
      return ResponseEntity.badRequest().body("Query parameter 'q' is required");
    }
    if (type == null) {
      return ResponseEntity.badRequest().body("Parameter 'type' is required");
    }
    switch (type.toLowerCase()) {
      case "collections" -> {
        Page<CollectionResponseDto> collections = searchService.searchCollections(query, pageable);
        return ResponseEntity.ok(collections);
      }
      case "elements" -> {
        Page<ElementResponseDto> elements =
            searchService.searchElements(query, collectionId, pageable);
        return ResponseEntity.ok(elements);
      }
      default -> {
        return ResponseEntity.badRequest()
            .body("Invalid type parameter. Expected 'collections' or 'elements'.");
      }
    }
  }
}
