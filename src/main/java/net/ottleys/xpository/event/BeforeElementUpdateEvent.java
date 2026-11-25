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
 * Event published before an element is updated.
 *
 * <p>This event is published before the element changes are persisted to the database. Listeners
 * can:
 *
 * <ul>
 *   <li>Validate the updated element data
 *   <li>Modify element properties
 *   <li>Compare old vs new values
 *   <li>Throw exceptions to prevent update
 * </ul>
 *
 * <p>The element contains the new values that will be saved.
 */
public class BeforeElementUpdateEvent extends ElementEvent {

  private final Element oldElement;

  /**
   * Constructs a BeforeElementUpdateEvent.
   *
   * @param source the service that is updating the element
   * @param oldElement the element before update (original state)
   * @param newElement the element after update (new state)
   * @param username the username of the user updating the element
   */
  public BeforeElementUpdateEvent(
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
    return true;
  }
}
