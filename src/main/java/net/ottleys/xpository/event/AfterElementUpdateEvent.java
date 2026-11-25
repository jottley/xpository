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
 * Event published after an element is successfully updated.
 *
 * <p>This event is published after the element changes have been persisted to the database.
 * Listeners can:
 *
 * <ul>
 *   <li>Send notifications about changes
 *   <li>Update caches
 *   <li>Trigger workflows based on changes
 *   <li>Integrate with external systems
 *   <li>Log change audit trails
 *   <li>Create version snapshots
 * </ul>
 *
 * <p>The element contains the updated values that were saved.
 */
public class AfterElementUpdateEvent extends ElementEvent {

  private final Element oldElement;

  /**
   * Constructs an AfterElementUpdateEvent.
   *
   * @param source the service that updated the element
   * @param oldElement the element before update (original state)
   * @param newElement the element after update (new state)
   * @param username the username of the user who updated the element
   */
  public AfterElementUpdateEvent(
      Object source, Element oldElement, Element newElement, String username) {
    super(source, newElement, username);
    this.oldElement = oldElement;
  }

  /**
   * Returns the element before the update.
   *
   * @return the old element state
   */
  public Element getOldElement() {
    return oldElement;
  }

  /**
   * Returns the element after the update (new state).
   *
   * <p>This is the same as {@link #getElement()}.
   *
   * @return the new element state
   */
  public Element getNewElement() {
    return getElement();
  }

  @Override
  public boolean isBefore() {
    return false;
  }
}
