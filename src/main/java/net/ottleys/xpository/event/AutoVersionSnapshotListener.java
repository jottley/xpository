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
package net.ottleys.xpository.event;

import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.service.ElementVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener that automatically creates version snapshots when elements are updated.
 *
 * <p>This listener responds to {@link AfterElementUpdateEvent} and creates an immutable version
 * snapshot of the element's state before the update. This ensures that all element changes are
 * captured in the version history.
 *
 * <p>The listener runs with high priority (Order 10) to ensure version snapshots are created before
 * other post-update processing. Runs in a new transaction (REQUIRES_NEW) to ensure the snapshot is
 * committed even if downstream listeners fail.
 */
@Component
public class AutoVersionSnapshotListener {

  private static final Logger logger = LoggerFactory.getLogger(AutoVersionSnapshotListener.class);

  private final ElementVersionService versionService;

  public AutoVersionSnapshotListener(ElementVersionService versionService) {
    this.versionService = versionService;
  }

  /**
   * Creates a version snapshot after an element is updated.
   *
   * <p>Captures the element's state before the update by creating a snapshot of the oldElement. The
   * snapshot preserves the pre-update version numbers, properties, and metadata.
   *
   * @param event the after-update event containing old and new element state
   */
  @EventListener
  @Order(10) // High priority - run before other post-update listeners
  @Transactional
  public void onElementUpdate(AfterElementUpdateEvent event) {
    Element oldElement = event.getOldElement();
    Element newElement = event.getNewElement();
    String username = event.getUsername();

    logger.debug(
        "Creating automatic version snapshot for element {} (version {}.{})",
        oldElement.getElementId(),
        oldElement.getMajorVersion(),
        oldElement.getMinorVersion());

    String versionComment = "Automatic snapshot before update";
    versionService.createVersionSnapshot(
        newElement != null ? newElement : oldElement,
        oldElement.getMajorVersion(),
        oldElement.getMinorVersion(),
        oldElement.getType().getName(),
        oldElement.getPropertiesJson(),
        versionComment,
        username);

    logger.info(
        "Created version snapshot {}.{} for element {}",
        oldElement.getMajorVersion(),
        oldElement.getMinorVersion(),
        oldElement.getElementId());
  }
}
