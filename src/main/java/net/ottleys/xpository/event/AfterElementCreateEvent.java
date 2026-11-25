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

/**
 * Event published after an element is successfully created.
 *
 * <p>This event is published after the element has been persisted to the database. Listeners can:
 *
 * <ul>
 *   <li>Send notifications
 *   <li>Update caches
 *   <li>Trigger workflows
 *   <li>Integrate with external systems
 *   <li>Log audit trails
 * </ul>
 *
 * <p>Note: The element has been assigned an ID and saved to the database.
 */
public class AfterElementCreateEvent extends ElementEvent {

  /**
   * Constructs an AfterElementCreateEvent.
   *
   * @param source the service that created the element
   * @param element the created element (with ID assigned)
   * @param username the username of the user who created the element
   */
  public AfterElementCreateEvent(Object source, Element element, String username) {
    super(source, element, username);
  }

  @Override
  public boolean isBefore() {
    return false;
  }
}
