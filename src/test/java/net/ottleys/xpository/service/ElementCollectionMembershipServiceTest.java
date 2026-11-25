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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;
import java.util.Iterator;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementCollectionMembership;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.CollectionRepository;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link ElementCollectionMembershipService} focusing on GRAPH and STACK
 * collection types.
 *
 * <p>These tests verify that the service correctly handles collection‑type‑specific fields when
 * adding an element to a collection.
 */
@SpringBootTest
@Transactional
class ElementCollectionMembershipServiceTest {

  @Autowired private ElementCollectionMembershipService membershipService;
  @Autowired private CollectionRepository collectionRepository;
  @Autowired private ElementRepository elementRepository;
  @Autowired private ElementTypeRepository elementTypeRepository;
  @Autowired private ElementCollectionMembershipRepository membershipRepository;

  /** Clean up any data created during a test. */
  @AfterEach
  void cleanup() {
    membershipRepository.deleteAll();
    elementRepository.deleteAll();
    collectionRepository.deleteBySystemCollectionFalse();
  }

  /** Helper to obtain a sample {@link ElementType} from the DB. */
  private ElementType getAnyElementType() {
    Iterator<ElementType> iter = elementTypeRepository.findAll().iterator();
    if (!iter.hasNext()) {
      throw new IllegalStateException("No ElementType fixtures loaded");
    }
    return iter.next();
  }

  /**
   * GRAPH collections do not use ordering or hierarchy fields.
   *
   * <p>The membership created for a GRAPH collection should have {@code null} values for {@code
   * orderPosition}, {@code depth}, and {@code materializedPath}.
   */
  @Test
  void shouldCreateMembershipForGraphCollectionWithoutExtraFields() {
    // --- arrange ---------------------------------------------------------
    Collection graph = new Collection("Graph Collection", CollectionType.GRAPH);
    graph.setCreatedBy("System");
    graph.setModifiedBy("System");
    graph = collectionRepository.save(graph);

    Element element = new Element();
    element.setType(getAnyElementType());
    element.setCreatedBy("System");
    element.setModifiedBy("System");
    element = elementRepository.save(element);

    // --- act -------------------------------------------------------------
    ElementCollectionMembership membership =
        membershipService.addElementToCollection(element, graph, "System");

    // --- assert -----------------------------------------------------------
    assertThat(membership).isNotNull();
    assertThat(membership.getCollection().getType()).isEqualTo(CollectionType.GRAPH);
    assertThat(membership.getOrderPosition()).isNull();
    assertThat(membership.getDepth()).isNull();
    assertThat(membership.getMaterializedPath()).isNull();
  }

  /**
   * STACK collections behave like a LIST but only the order position matters (LIFO). Depth is not
   * used and should be set to {@code 0} for consistency.
   *
   * <p>We add two elements to the same STACK collection and verify that the {@code orderPosition}
   * increments correctly.
   */
  @Test
  void shouldCreateMembershipForStackCollectionWithLifoOrder() {
    // --- arrange ---------------------------------------------------------
    Collection stack = new Collection("Stack Collection", CollectionType.STACK);
    stack.setCreatedBy("System");
    stack.setModifiedBy("System");
    stack = collectionRepository.save(stack);

    Element element1 = new Element();
    element1.setType(getAnyElementType());
    element1.setCreatedBy("System");
    element1.setModifiedBy("System");
    element1 = elementRepository.save(element1);

    Element element2 = new Element();
    element2.setType(getAnyElementType());
    element2.setCreatedBy("System");
    element2.setModifiedBy("System");
    element2 = elementRepository.save(element2);

    // --- act -------------------------------------------------------------
    ElementCollectionMembership m1 =
        membershipService.addElementToCollection(element1, stack, "tester");
    ElementCollectionMembership m2 =
        membershipService.addElementToCollection(element2, stack, "tester");

    // --- assert -----------------------------------------------------------
    assertThat(m1).isNotNull();
    assertThat(m2).isNotNull();

    // Both should have depth = 0 (not used for STACK)
    assertThat(m1.getDepth()).isEqualTo(0);
    assertThat(m2.getDepth()).isEqualTo(0);

    // Order positions should be 0 for the first element and 1 for the second
    assertThat(m1.getOrderPosition()).isEqualTo(0);
    assertThat(m2.getOrderPosition()).isEqualTo(1);

    // No materialized path for STACK
    assertThat(m1.getMaterializedPath()).isNull();
    assertThat(m2.getMaterializedPath()).isNull();
  }
}
