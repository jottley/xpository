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
 * Event published before an element is created.
 *
 * <p>This event is published before the element is persisted to the database. Listeners can:
 *
 * <ul>
 *   <li>Validate the element data
 *   <li>Modify element properties
 *   <li>Enrich element data
 *   <li>Throw exceptions to prevent creation
 * </ul>
 *
 * <p>Note: The element does not yet have an ID assigned at this point.
 */
public class BeforeElementCreateEvent extends ElementEvent {

  /**
   * Constructs a BeforeElementCreateEvent.
   *
   * @param source the service that is creating the element
   * @param element the element being created
   * @param username the username of the user creating the element
   */
  public BeforeElementCreateEvent(Object source, Element element, String username) {
    super(source, element, username);
  }

  @Override
  public boolean isBefore() {
    return true;
  }
}
