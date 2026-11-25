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

import java.util.List;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.event.AfterElementDeleteEvent;
import net.ottleys.xpository.repository.ElementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup service to clean up any orphaned elements that have no collection memberships.
 *
 * <p>Uses {@link ApplicationReadyEvent} to ensure the application context and database session
 * are fully initialised. Soft-deletes any non-deleted elements found with zero collection
 * memberships.
 */
@Component
public class ElementSoftDeleteCleaner {

  private static final Logger logger = LoggerFactory.getLogger(ElementSoftDeleteCleaner.class);

  private final ElementRepository elementRepository;
  private final ApplicationEventPublisher eventPublisher;

  public ElementSoftDeleteCleaner(
      ElementRepository elementRepository, ApplicationEventPublisher eventPublisher) {
    this.elementRepository = elementRepository;
    this.eventPublisher = eventPublisher;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void cleanOrphanElements() {
    List<Element> orphans = elementRepository.findOrphanedNotDeleted();
    if (!orphans.isEmpty()) {
      logger.info(
          "Found {} orphaned non-deleted element(s); marking as soft-deleted", orphans.size());
      for (Element orphan : orphans) {
        orphan.softDelete("system-cleanup");
        elementRepository.save(orphan);
        eventPublisher.publishEvent(new AfterElementDeleteEvent(this, orphan, "system-cleanup"));
      }
    }
  }
}
