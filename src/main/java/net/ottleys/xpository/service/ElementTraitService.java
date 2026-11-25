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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.exception.BusinessValidationException;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTraitRepository;
import net.ottleys.xpository.repository.TraitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for ElementTrait business logic.
 *
 * <p>Handles assigning traits to elements, updating trait values, removing traits, and validating
 * values against trait constraints. All public methods are transactional to ensure data
 * consistency. Read-only operations use readOnly=true for performance optimization.
 */
@Service
@Transactional
public class ElementTraitService {

  private final ElementTraitRepository elementTraitRepository;
  private final ElementRepository elementRepository;
  private final TraitRepository traitRepository;
  private final ObjectMapper objectMapper;

  /**
   * Constructs an ElementTraitService with required dependencies.
   *
   * @param elementTraitRepository repository for element-trait data access
   * @param elementRepository repository for element lookups
   * @param traitRepository repository for trait lookups
   * @param objectMapper JSON object mapper for validation
   */
  public ElementTraitService(
      ElementTraitRepository elementTraitRepository,
      ElementRepository elementRepository,
      TraitRepository traitRepository,
      ObjectMapper objectMapper) {
    this.elementTraitRepository = elementTraitRepository;
    this.elementRepository = elementRepository;
    this.traitRepository = traitRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Retrieves all traits for an element by UUID.
   *
   * @param elementId the element UUID
   * @return list of element-trait associations
   */
  @Transactional(readOnly = true)
  public List<ElementTrait> findByElementId(UUID elementId) {
    return elementTraitRepository.findByElementId(elementId);
  }

  /**
   * Retrieves all elements with a specific trait by trait UUID.
   *
   * @param traitId the trait UUID
   * @return list of element-trait associations
   */
  @Transactional(readOnly = true)
  public List<ElementTrait> findByTraitId(UUID traitId) {
    return elementTraitRepository.findByTraitId(traitId);
  }

  /**
   * Retrieves a specific element-trait association.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @return Optional containing the association if found
   */
  @Transactional(readOnly = true)
  public Optional<ElementTrait> findByElementIdAndTraitId(UUID elementId, UUID traitId) {
    return elementTraitRepository.findByElementIdAndTraitId(elementId, traitId);
  }

  /**
   * Retrieves all inherited trait values for an element.
   *
   * @param elementId the element UUID
   * @return list of inherited traits
   */
  @Transactional(readOnly = true)
  public List<ElementTrait> findInheritedByElementId(UUID elementId) {
    Optional<Element> elementOpt = elementRepository.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return List.of();
    }
    return elementTraitRepository.findInheritedByElement(elementOpt.get());
  }

  /**
   * Retrieves all directly assigned (non-inherited) trait values for an element.
   *
   * @param elementId the element UUID
   * @return list of directly assigned traits
   */
  @Transactional(readOnly = true)
  public List<ElementTrait> findDirectByElementId(UUID elementId) {
    Optional<Element> elementOpt = elementRepository.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return List.of();
    }
    return elementTraitRepository.findDirectByElement(elementOpt.get());
  }

  /**
   * Assigns a trait to an element with a value.
   *
   * <p>Creates a new element-trait association with the specified value. Validates that both
   * element and trait exist, that the trait is not already assigned, and that the value conforms to
   * the trait's type and allowed values constraints.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @param value the trait value
   * @return the saved element-trait association
   * @throws IllegalArgumentException if element or trait does not exist
   * @throws BusinessValidationException if trait is already assigned or value is invalid
   */
  public ElementTrait assignTrait(UUID elementId, UUID traitId, String value) {
    // Validate element exists
    Element element =
        elementRepository
            .findByElementId(elementId)
            .orElseThrow(() -> new IllegalArgumentException("Element not found: " + elementId));

    // Validate trait exists
    Trait trait =
        traitRepository
            .findByTraitId(traitId)
            .orElseThrow(() -> new IllegalArgumentException("Trait not found: " + traitId));

    // Check if already assigned
    if (elementTraitRepository.existsByElementAndTrait(element, trait)) {
      throw new BusinessValidationException(
          "Trait '" + trait.getName() + "' is already assigned to this element");
    }

    // Validate value against trait constraints
    validateValue(trait, value);

    // Create association
    ElementTrait elementTrait = new ElementTrait(element, trait, value);
    elementTrait.setInherited(false);
    return elementTraitRepository.save(elementTrait);
  }

  /**
   * Updates the value of an assigned trait.
   *
   * <p>Modifies the value of an existing element-trait association. Validates that the new value
   * conforms to the trait's type and allowed values constraints. Cannot modify inherited traits.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @param newValue the new trait value
   * @return the updated element-trait association
   * @throws IllegalArgumentException if association does not exist
   * @throws BusinessValidationException if trait is inherited or value is invalid
   */
  public ElementTrait updateTraitValue(UUID elementId, UUID traitId, String newValue) {
    ElementTrait elementTrait =
        elementTraitRepository
            .findByElementIdAndTraitId(elementId, traitId)
            .orElseThrow(() ->
                new IllegalArgumentException("Trait is not assigned to this element: " + traitId));

    // Cannot update inherited traits
    if (elementTrait.isInherited()) {
      throw new BusinessValidationException(
          "Cannot update inherited trait '"
              + elementTrait.getTrait().getName()
              + "'. Override it by assigning a new value.");
    }

    // Validate new value
    validateValue(elementTrait.getTrait(), newValue);

    // Update value
    elementTrait.setValue(newValue);
    return elementTraitRepository.save(elementTrait);
  }

  /**
   * Removes a trait from an element.
   *
   * <p>Deletes the element-trait association. Cannot remove inherited traits; they must be removed
   * from the source element.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @return true if trait was removed, false if not found
   * @throws BusinessValidationException if trait is inherited
   */
  public boolean removeTrait(UUID elementId, UUID traitId) {
    Optional<ElementTrait> elementTraitOpt =
        elementTraitRepository.findByElementIdAndTraitId(elementId, traitId);

    if (elementTraitOpt.isEmpty()) {
      return false;
    }

    ElementTrait elementTrait = elementTraitOpt.get();

    // Cannot remove inherited traits
    if (elementTrait.isInherited()) {
      throw new BusinessValidationException(
          "Cannot remove inherited trait '"
              + elementTrait.getTrait().getName()
              + "'. Remove it from the source element.");
    }

    elementTraitRepository.delete(elementTrait);
    return true;
  }

  /**
   * Removes all traits from an element.
   *
   * <p>Deletes all element-trait associations for an element. Used during element deletion.
   *
   * @param elementId the element UUID
   */
  public void removeAllTraits(UUID elementId) {
    Optional<Element> elementOpt = elementRepository.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return;
    }
    elementTraitRepository.deleteByElement(elementOpt.get());
  }

  /**
   * Validates a trait value against trait constraints.
   *
   * <p>Checks that the value:
   *
   * <ul>
   *   <li>Conforms to the trait's type (STRING, NUMBER, BOOLEAN, DATE)
   *   <li>Is one of the allowed values if allowedValuesJson is specified
   * </ul>
   *
   * @param trait the trait definition
   * @param value the value to validate
   * @throws BusinessValidationException if value is invalid
   */
  private void validateValue(Trait trait, String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessValidationException("Trait value cannot be empty");
    }

    // Validate type
    switch (trait.getTraitType()) {
      case NUMBER:
        validateNumber(value, trait.getName());
        break;
      case BOOLEAN:
        validateBoolean(value, trait.getName());
        break;
      case DATE:
        validateDate(value, trait.getName());
        break;
      case STRING:
        // No type validation needed for STRING
        break;
      default:
        throw new BusinessValidationException("Unknown trait type: " + trait.getTraitType());
    }

    // Validate against allowed values if specified
    validateAllowedValues(trait, value);
  }

  /**
   * Validates that a value is a valid number.
   *
   * @param value the value to validate
   * @param traitName the trait name for error messages
   * @throws BusinessValidationException if value is not a valid number
   */
  private void validateNumber(String value, String traitName) {
    try {
      Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new BusinessValidationException(
          "Invalid value for trait '" + traitName + "': expected a number, got '" + value + "'");
    }
  }

  /**
   * Validates that a value is a valid boolean.
   *
   * @param value the value to validate
   * @param traitName the trait name for error messages
   * @throws BusinessValidationException if value is not a valid boolean
   */
  private void validateBoolean(String value, String traitName) {
    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
      throw new BusinessValidationException(
          "Invalid value for trait '"
              + traitName
              + "': expected 'true' or 'false', got '"
              + value
              + "'");
    }
  }

  /**
   * Validates that a value is a valid ISO-8601 date.
   *
   * @param value the value to validate
   * @param traitName the trait name for error messages
   * @throws BusinessValidationException if value is not a valid date
   */
  private void validateDate(String value, String traitName) {
    try {
      LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new BusinessValidationException(
          "Invalid value for trait '"
              + traitName
              + "': expected ISO-8601 date (YYYY-MM-DD), got '"
              + value
              + "'");
    }
  }

  /**
   * Validates that a value is in the allowed values list if specified.
   *
   * @param trait the trait definition
   * @param value the value to validate
   * @throws BusinessValidationException if value is not in allowed values
   */
  private void validateAllowedValues(Trait trait, String value) {
    String allowedValuesJson = trait.getAllowedValuesJson();

    if (allowedValuesJson == null || allowedValuesJson.isBlank()) {
      return; // No restrictions
    }

    try {
      JsonNode arrayNode = objectMapper.readTree(allowedValuesJson);
      if (!arrayNode.isArray()) {
        return; // Invalid format, skip validation
      }

      // Check if value matches any allowed value
      boolean isAllowed = false;
      Iterator<JsonNode> elements = arrayNode.elements();
      while (elements.hasNext()) {
        JsonNode element = elements.next();
        if (element.isTextual() && element.asText().equals(value)) {
          isAllowed = true;
          break;
        } else if (element.isNumber() && element.asText().equals(value)) {
          isAllowed = true;
          break;
        } else if (element.isBoolean() && element.asText().equals(value)) {
          isAllowed = true;
          break;
        }
      }

      if (!isAllowed) {
        throw new BusinessValidationException(
            "Invalid value for trait '"
                + trait.getName()
                + "': '"
                + value
                + "' is not in the allowed values list");
      }
    } catch (JsonProcessingException e) {
      // Invalid JSON format, skip validation
    }
  }

  /**
   * Inherits all inheritable traits from a parent element to a child element.
   *
   * <p>This method copies all traits marked as inheritable from the parent element to the child
   * element. Inherited traits are marked with inherited=true and reference the parent element ID.
   * If the child already has a direct assignment for a trait, it is not overridden by inheritance.
   *
   * @param childElementId the child element UUID
   * @param parentElementId the parent element UUID
   * @return number of traits inherited
   */
  public int inheritTraitsFromParent(UUID childElementId, UUID parentElementId) {
    Element childElement =
        elementRepository
            .findByElementId(childElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Child element not found: " + childElementId));

    Element parentElement =
        elementRepository
            .findByElementId(parentElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Parent element not found: " + parentElementId));

    // Get all traits from parent
    List<ElementTrait> parentTraits = elementTraitRepository.findByElementId(parentElementId);

    // Get child's directly assigned traits to avoid overriding
    List<ElementTrait> childDirectTraits =
        elementTraitRepository.findDirectByElementId(childElementId);
    List<UUID> childTraitIds =
        childDirectTraits.stream().map(et -> et.getTrait().getTraitId()).toList();

    int inheritedCount = 0;

    for (ElementTrait parentTrait : parentTraits) {
      Trait trait = parentTrait.getTrait();

      // Only inherit if:
      // 1. Trait is marked as inheritable
      // 2. Child doesn't already have a direct assignment for this trait
      if (trait.isInheritable() && !childTraitIds.contains(trait.getTraitId())) {
        // Check if child already has an inherited version of this trait
        Optional<ElementTrait> existingInherited =
            elementTraitRepository.findByElementIdAndTraitId(childElementId, trait.getTraitId());

        if (existingInherited.isEmpty()) {
          // Create inherited trait
          ElementTrait inherited = new ElementTrait();
          inherited.setElement(childElement);
          inherited.setTrait(trait);
          inherited.setValue(parentTrait.getValue());
          inherited.setInherited(true);
          inherited.setInheritedFromElementId(parentElement.getId());

          elementTraitRepository.save(inherited);
          inheritedCount++;
        }
      }
    }

    return inheritedCount;
  }

  /**
   * Removes all inherited traits from an element that were inherited from a specific parent.
   *
   * <p>This method is typically called when a parent-child relationship is broken in a collection.
   *
   * @param childElementId the child element UUID
   * @param parentElementInternalId the parent element's internal database ID
   * @return number of traits removed
   */
  public int removeInheritedTraitsFromParent(UUID childElementId, Long parentElementInternalId) {
    Element childElement =
        elementRepository
            .findByElementId(childElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Child element not found: " + childElementId));

    List<ElementTrait> inheritedTraits =
        elementTraitRepository.findInheritedByElementId(childElementId);

    int removedCount = 0;

    for (ElementTrait trait : inheritedTraits) {
      if (trait.getInheritedFromElementId() != null
          && trait.getInheritedFromElementId().equals(parentElementInternalId)) {
        elementTraitRepository.delete(trait);
        removedCount++;
      }
    }

    return removedCount;
  }

  /**
   * Refreshes inherited traits for a child element from its parent.
   *
   * <p>This method removes all inherited traits from the parent and re-applies them, ensuring the
   * child has the latest trait values from the parent. Useful when parent traits are updated.
   *
   * @param childElementId the child element UUID
   * @param parentElementId the parent element UUID
   * @return number of traits refreshed
   */
  public int refreshInheritedTraits(UUID childElementId, UUID parentElementId) {
    Element parentElement =
        elementRepository
            .findByElementId(parentElementId)
            .orElseThrow(() ->
                new BusinessValidationException("Parent element not found: " + parentElementId));

    // Remove old inherited traits from this parent
    removeInheritedTraitsFromParent(childElementId, parentElement.getId());

    // Re-apply inherited traits
    return inheritTraitsFromParent(childElementId, parentElementId);
  }
}
