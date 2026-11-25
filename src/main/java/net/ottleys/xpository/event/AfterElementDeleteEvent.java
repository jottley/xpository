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
 * Event published after an element is successfully deleted.
 *
 * <p>This event is published after the element has been removed from the database. Listeners can:
 *
 * <ul>
 *   <li>Send notifications about deletion
 *   <li>Update caches
 *   <li>Clean up related data
 *   <li>Integrate with external systems
 *   <li>Log deletion audit trails
 * </ul>
 *
 * <p>Note: The element no longer exists in the database, but the event contains the deleted
 * element's data for reference.
 */
public class AfterElementDeleteEvent extends ElementEvent {

  /**
   * Constructs an AfterElementDeleteEvent.
   *
   * @param source the service that deleted the element
   * @param element the deleted element (data for reference)
   * @param username the username of the user who deleted the element
   */
  public AfterElementDeleteEvent(Object source, Element element, String username) {
    super(source, element, username);
  }

  @Override
  public boolean isBefore() {
    return false;
  }
}
