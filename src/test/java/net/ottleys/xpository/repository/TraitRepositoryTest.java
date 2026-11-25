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
package net.ottleys.xpository.repository;

import static org.assertj.core.api.Assertions.assertThat;

import net.ottleys.xpository.domain.Trait;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Basic repository test to verify Trait entity persistence. */
@SpringBootTest
class TraitRepositoryTest {

  @Autowired private TraitRepository traitRepository;

  @Test
  void saveTrait_shouldPersist() {
    Trait trait = new Trait();
    trait.setName("Test Trait " + java.util.UUID.randomUUID());
    trait.setDescription("Test");
    trait.setTraitType(Trait.TraitType.STRING);

    Trait saved = traitRepository.save(trait);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTraitId()).isNotNull();
  }
}
