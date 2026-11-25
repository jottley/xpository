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
 * Event published before an element is deleted.
 *
 * <p>This event is published before the element is removed from the database. Listeners can:
 *
 * <ul>
 *   <li>Validate that deletion is allowed
 *   <li>Check for dependencies
 *   <li>Archive element data
 *   <li>Throw exceptions to prevent deletion
 * </ul>
 *
 * <p>The element still exists in the database at this point.
 */
public class BeforeElementDeleteEvent extends ElementEvent {

  /**
   * Constructs a BeforeElementDeleteEvent.
   *
   * @param source the service that is deleting the element
   * @param element the element being deleted
   * @param username the username of the user deleting the element
   */
  public BeforeElementDeleteEvent(Object source, Element element, String username) {
    super(source, element, username);
  }

  @Override
  public boolean isBefore() {
    return true;
  }
}
