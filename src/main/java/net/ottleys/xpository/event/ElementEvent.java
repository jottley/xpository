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

import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import org.springframework.context.ApplicationEvent;

/**
 * Base class for all element lifecycle events.
 *
 * <p>Element events are published during element lifecycle operations (create, update, delete) and
 * provide hooks for custom business logic, validation, and integration with external systems.
 *
 * <p>Events are published both before and after operations, allowing for:
 *
 * <ul>
 *   <li>Pre-operation validation and modification (before events)
 *   <li>Post-operation notifications and side effects (after events)
 *   <li>Auditing and logging
 *   <li>Cache invalidation
 *   <li>External system integration
 * </ul>
 */
public abstract class ElementEvent extends ApplicationEvent {

  private final Element element;
  private final UUID elementId;
  private final String username;

  /**
   * Constructs an ElementEvent.
   *
   * @param source the object on which the event initially occurred
   * @param element the element involved in the event
   * @param username the username of the user who triggered the event
   */
  protected ElementEvent(Object source, Element element, String username) {
    super(source);
    this.element = element;
    this.elementId = element.getElementId();
    this.username = username;
  }

  /**
   * Returns the element involved in the event.
   *
   * <p>For before events, this is the element in its current state. For after events, this is the
   * element in its new state.
   *
   * @return the element
   */
  public Element getElement() {
    return element;
  }

  /**
   * Returns the element UUID.
   *
   * @return the element UUID
   */
  public UUID getElementId() {
    return elementId;
  }

  /**
   * Returns the username of the user who triggered the event.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Returns whether this is a "before" event.
   *
   * <p>Before events are published before the operation is executed. They can be used for
   * validation or modification.
   *
   * @return true if this is a before event
   */
  public abstract boolean isBefore();

  /**
   * Returns whether this is an "after" event.
   *
   * <p>After events are published after the operation has been successfully executed. They can be
   * used for notifications and side effects.
   *
   * @return true if this is an after event
   */
  public boolean isAfter() {
    return !isBefore();
  }
}
