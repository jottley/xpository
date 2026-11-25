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
package net.ottleys.xpository.event.listener;

import net.ottleys.xpository.event.AfterElementCreateEvent;
import net.ottleys.xpository.event.AfterElementDeleteEvent;
import net.ottleys.xpository.event.AfterElementUpdateEvent;
import net.ottleys.xpository.event.BeforeElementCreateEvent;
import net.ottleys.xpository.event.BeforeElementDeleteEvent;
import net.ottleys.xpository.event.BeforeElementUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sample event listener that logs element lifecycle events.
 *
 * <p>This listener demonstrates how to subscribe to element events for auditing and logging
 * purposes. In production, you might log to a database, send to a SIEM, or trigger external
 * workflows.
 *
 * <p>Uses @Order(100) to execute after validation listeners (which typically use lower order
 * numbers) but before notification listeners (which typically use higher order numbers).
 */
@Component
@Order(100)
public class ElementEventLoggingListener {

  private static final Logger logger = LoggerFactory.getLogger(ElementEventLoggingListener.class);

  /**
   * Handles before-create events.
   *
   * @param event the before-create event
   */
  @EventListener
  public void handleBeforeCreate(BeforeElementCreateEvent event) {
    logger.info(
        "BEFORE CREATE: Element type={}, user={}, properties={}",
        event.getElement().getType().getName(),
        event.getUsername(),
        event.getElement().getPropertiesJson() != null
            ? event.getElement().getPropertiesJson().length() + " chars"
            : "null");
  }

  /**
   * Handles after-create events.
   *
   * @param event the after-create event
   */
  @EventListener
  public void handleAfterCreate(AfterElementCreateEvent event) {
    logger.info(
        "AFTER CREATE: Element id={}, type={}, user={}",
        event.getElementId(),
        event.getElement().getType().getName(),
        event.getUsername());
  }

  /**
   * Handles before-update events.
   *
   * @param event the before-update event
   */
  @EventListener
  public void handleBeforeUpdate(BeforeElementUpdateEvent event) {
    logger.info(
        "BEFORE UPDATE: Element id={}, type={}, user={}, oldVersion={}, newVersion={}",
        event.getElementId(),
        event.getElement().getType().getName(),
        event.getUsername(),
        event.getOldElement().getMajorVersion() + "." + event.getOldElement().getMinorVersion(),
        event.getNewElement().getMajorVersion() + "." + event.getNewElement().getMinorVersion());
  }

  /**
   * Handles after-update events.
   *
   * @param event the after-update event
   */
  @EventListener
  public void handleAfterUpdate(AfterElementUpdateEvent event) {
    logger.info(
        "AFTER UPDATE: Element id={}, type={}, user={}",
        event.getElementId(),
        event.getElement().getType().getName(),
        event.getUsername());
  }

  /**
   * Handles before-delete events.
   *
   * @param event the before-delete event
   */
  @EventListener
  public void handleBeforeDelete(BeforeElementDeleteEvent event) {
    logger.info(
        "BEFORE DELETE: Element id={}, type={}, user={}",
        event.getElementId(),
        event.getElement().getType().getName(),
        event.getUsername());
  }

  /**
   * Handles after-delete events.
   *
   * @param event the after-delete event
   */
  @EventListener
  public void handleAfterDelete(AfterElementDeleteEvent event) {
    logger.info(
        "AFTER DELETE: Element id={}, type={}, user={}",
        event.getElementId(),
        event.getElement().getType().getName(),
        event.getUsername());
  }
}
