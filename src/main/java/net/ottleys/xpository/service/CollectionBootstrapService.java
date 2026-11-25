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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.repository.CollectionRepository;

/**
 * Bootstrap service that creates the System Root collection at application startup.
 *
 * <p>Ensures a single system-level root collection exists to serve as the top-level container for
 * all user-created collections. The System Root is a TREE-type collection that does not inherit
 * permissions and is marked as a system collection for special handling.
 */
@Service
public class CollectionBootstrapService {

  private static final Logger logger = LoggerFactory.getLogger(CollectionBootstrapService.class);
  private static final String SYSTEM_ROOT_NAME = "System Root";
  private static final String SYSTEM_USER = "system";

  private final CollectionRepository collectionRepository;

  public CollectionBootstrapService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

  @Value("${xpository.collection.systemRoot.type}")
  private CollectionType systemRootType = CollectionType.LIST;

  /**
   * Creates the System Root collection if it doesn't already exist.
   *
   * <p>Runs after the application context is fully initialized. The System Root is a TREE-type
   * collection that serves as the top-level container for all user collections.
   */
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void createSystemRootCollection() {
    logger.info("Checking for system root collection...");

    // Check if system root already exists
    if (collectionRepository.findSystemRootCollection().isPresent()) {
      logger.debug("System root collection already exists, skipping creation");
      return;
    }

    // Create system root collection
    Collection systemRoot = new Collection();
    systemRoot.setName(SYSTEM_ROOT_NAME);
    systemRoot.setDescription(
        "System root collection. All top-level collections are contained within this collection.");

    if (systemRootType.equals(CollectionType.LIST) || systemRootType.equals(CollectionType.TREE) || systemRootType.equals(CollectionType.GRAPH)) {
      systemRoot.setType(systemRootType);
      logger.info("Creating system root collection with type: {}", systemRootType);
    }
    else {
      logger.warn("Invalid system root collection type: {}. Defaulting to LIST.", systemRootType);
      systemRoot.setType(CollectionType.LIST);
    }


    systemRoot.setSystemCollection(true);
    systemRoot.setCreatedBy(SYSTEM_USER);
    systemRoot.setModifiedBy(SYSTEM_USER);
    systemRoot.setInheritPermissions(false); // System root doesn't inherit permissions

    logger.info("Saving system root collection...");

    systemRoot = collectionRepository.save(systemRoot);

    logger.info(
        "Created system root collection with ID: {} (UUID: {})",
        systemRoot.getId(),
        systemRoot.getCollectionId());
  }
}
