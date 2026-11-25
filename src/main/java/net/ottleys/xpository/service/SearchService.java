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
package net.ottleys.xpository.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import java.util.function.Function;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.mapper.CollectionMapper;
import net.ottleys.xpository.mapper.ElementMapper;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that performs full‑text search over {@link Collection} and {@link Element} entities using
 * Hibernate Search (Lucene backend).
 *
 * <p>The service is deliberately lightweight – it builds a simple match query across the most
 * useful searchable fields and returns the top hits.
 */
@Service
public class SearchService {

  @PersistenceContext private EntityManager entityManager;

  private final CollectionMapper collectionMapper;
  private final ElementMapper elementMapper;

  public SearchService(CollectionMapper collectionMapper, ElementMapper elementMapper) {
    this.collectionMapper = collectionMapper;
    this.elementMapper = elementMapper;
  }

  private SearchSession getSession() {
    return Search.session(entityManager);
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  @ConditionalOnProperty(name = "xpository.search.reindex-on-startup", havingValue = "true")
  public void onApplicationReady() {
    SearchSession session = Search.session(entityManager);
    session.massIndexer().threadsToLoadObjects(4).start();
  }

  /**
   * Full‑text search for collections.
   *
   * @param query the user supplied search string (must not be null)
   * @return a list of matching {@link Collection} entities, ordered by relevance.
   */
  @Transactional(readOnly = true)
  public Page<CollectionResponseDto> searchCollections(String query, Pageable pageable) {
    return executeSearch(
            Collection.class,
            query,
            pageable,
            f ->
                f.bool()
                    .with(b -> {
                      b.filter(f.match().field("deleted").matching(false));
                      b.must(
                          f.match()
                              .field("name")
                              .boost(3.0f)
                              .field("description")
                              .boost(1.5f)
                              .field("metadataJson")
                              .matching(query.trim()));
                    }))
        .map(collectionMapper::toResponseDto);
  }

  /**
   * Full-text search for elements, optionally scoped to a collection.
   *
   * @param query the user-supplied search string
   * @param collectionId optional collection ID to restrict results (nullable)
   * @param pageable pagination parameters
   * @return a page of matching {@link Element} entities ordered by relevance
   */
  @Transactional(readOnly = true)
  public Page<ElementResponseDto> searchElements(
      String query, UUID collectionId, Pageable pageable) {
    return executeSearch(
            Element.class,
            query,
            pageable,
            f ->
                f.bool()
                    .with(b -> {
                      b.filter(f.match().field("deleted").matching(false));
                      if (collectionId != null) {
                        b.filter(f.match().field("collection.id").matching(collectionId));
                      }
                      b.must(
                          f.bool()
                              .with(inner -> {
                                inner.should(
                                    f.match()
                                        .field("propertiesJson")
                                        .matching(query.trim())
                                        .boost(2.0f));
                                inner.should(
                                    f.match()
                                        .fields("createdBy", "modifiedBy")
                                        .matching(query.trim()));
                              }));
                    }))
        .map(elementMapper::toResponseDto);
  }

  private <T> Page<T> executeSearch(
      Class<T> entityClass,
      String query,
      Pageable pageable,
      Function<SearchPredicateFactory, PredicateFinalStep> predicateBuilder) {
    Pageable effectivePageable =
        (pageable == null || pageable.isUnpaged()) ? PageRequest.of(0, 50) : pageable;

    if (query == null || query.isBlank()) {
      return Page.empty(effectivePageable);
    }

    SearchResult<T> result =
        getSession()
            .search(entityClass)
            .where(predicateBuilder)
            .fetch((int) effectivePageable.getOffset(), effectivePageable.getPageSize());

    return new PageImpl<>(result.hits(), effectivePageable, result.total().hitCount());
  }
}
