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
package net.ottleys.xpository.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Junction entity associating elements with traits and their values.
 *
 * <p>This entity represents the many-to-many relationship between elements and traits, storing the
 * specific value of the trait for each element. An element can have multiple traits, and a trait
 * can be applied to multiple elements.
 *
 * <p>Example: Element "Project X" might have:
 *
 * <ul>
 *   <li>Security Classification trait with value "Confidential"
 *   <li>Lifecycle Stage trait with value "Active"
 *   <li>Priority trait with value "High"
 * </ul>
 *
 * <p>The unique constraint ensures an element can only have one value per trait.
 */
@Entity
@Table(
    name = "element_traits",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_element_trait",
          columnNames = {"element_id", "trait_id"})
    })
@EntityListeners(AuditingEntityListener.class)
public class ElementTrait {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull(message = "Element is required")
  @ManyToOne
  @JoinColumn(name = "element_id", nullable = false)
  private Element element;

  @NotNull(message = "Trait is required")
  @ManyToOne
  @JoinColumn(name = "trait_id", nullable = false)
  private Trait trait;

  @NotBlank(message = "Trait value is required")
  @Size(max = 500, message = "Trait value cannot exceed 500 characters")
  @Column(name = "value", nullable = false, length = 500)
  private String value;

  @Column(name = "inherited", nullable = false)
  private boolean inherited = false;

  @Column(name = "inherited_from_element_id")
  private Long inheritedFromElementId;

  @CreatedDate
  @Column(name = "created_date", nullable = false, updatable = false)
  private LocalDateTime createdDate;

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 100)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "modified_date")
  private LocalDateTime modifiedDate;

  @LastModifiedBy
  @Column(name = "modified_by", length = 100)
  private String modifiedBy;

  /** Default constructor required by JPA. */
  public ElementTrait() {}

  /**
   * Constructs an ElementTrait with element, trait, and value.
   *
   * @param element the element
   * @param trait the trait
   * @param value the trait value
   */
  public ElementTrait(Element element, Trait trait, String value) {
    this.element = element;
    this.trait = trait;
    this.value = value;
  }

  /**
   * Returns the internal database ID.
   *
   * @return the internal ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the internal database ID.
   *
   * @param id the internal ID
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the element.
   *
   * @return the element
   */
  public Element getElement() {
    return element;
  }

  /**
   * Sets the element.
   *
   * @param element the element
   */
  public void setElement(Element element) {
    this.element = element;
  }

  /**
   * Returns the trait.
   *
   * @return the trait
   */
  public Trait getTrait() {
    return trait;
  }

  /**
   * Sets the trait.
   *
   * @param trait the trait
   */
  public void setTrait(Trait trait) {
    this.trait = trait;
  }

  /**
   * Returns the trait value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the trait value.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns whether this trait value was inherited.
   *
   * @return true if inherited
   */
  public boolean isInherited() {
    return inherited;
  }

  /**
   * Sets whether this trait value was inherited.
   *
   * @param inherited true if inherited
   */
  public void setInherited(boolean inherited) {
    this.inherited = inherited;
  }

  /**
   * Returns the ID of the element this trait was inherited from.
   *
   * @return the inherited-from element ID, or null if not inherited
   */
  public Long getInheritedFromElementId() {
    return inheritedFromElementId;
  }

  /**
   * Sets the ID of the element this trait was inherited from.
   *
   * @param inheritedFromElementId the inherited-from element ID
   */
  public void setInheritedFromElementId(Long inheritedFromElementId) {
    this.inheritedFromElementId = inheritedFromElementId;
  }

  /**
   * Returns the creation timestamp.
   *
   * @return the created date
   */
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  /**
   * Sets the creation timestamp.
   *
   * @param createdDate the created date
   */
  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Returns the username of the user who created this element-trait association.
   *
   * @return the creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets the username of the user who created this element-trait association.
   *
   * @param createdBy the creator username
   */
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Returns the last modification timestamp.
   *
   * @return the modified date
   */
  public LocalDateTime getModifiedDate() {
    return modifiedDate;
  }

  /**
   * Sets the last modification timestamp.
   *
   * @param modifiedDate the modified date
   */
  public void setModifiedDate(LocalDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  /**
   * Returns the username of the user who last modified this element-trait association.
   *
   * @return the modifier username
   */
  public String getModifiedBy() {
    return modifiedBy;
  }

  /**
   * Sets the username of the user who last modified this element-trait association.
   *
   * @param modifiedBy the modifier username
   */
  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ElementTrait that = (ElementTrait) o;
    return Objects.equals(element, that.element) && Objects.equals(trait, that.trait);
  }

  @Override
  public int hashCode() {
    return Objects.hash(element, trait);
  }

  @Override
  public String toString() {
    return "ElementTrait{"
        + "id="
        + id
        + ", element="
        + (element != null ? element.getElementId() : null)
        + ", trait="
        + (trait != null ? trait.getName() : null)
        + ", value='"
        + value
        + '\''
        + ", inherited="
        + inherited
        + '}';
  }
}
