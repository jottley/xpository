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

import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.ElementRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;

/**
 * Integration tests for JSON Schema validation in ElementService.
 *
 * <p>Tests the integration of JsonSchemaValidationService with ElementService to validate element
 * properties against element type schemas (JSON Schema Draft 2020-12). Tests cover valid/invalid
 * properties, missing schemas, and malformed data.
 */
@SpringBootTest
class ElementSchemaValidationTest {

  @Autowired private ElementService elementService;
  @Autowired private ElementTypeRepository elementTypeRepository;
  @Autowired private ElementRepository elementRepository;

  private ElementType documentType;

  @BeforeEach
  void setUp() {
    // Clean up elements only (don't delete built-in element types)
    // Delete each element individually to avoid issues with null entity references
    elementRepository.findAll().stream()
        .filter(Objects::nonNull)
        .forEach(elementRepository::delete);

    // Check if our test element type already exists
    Optional<ElementType> existing =
        elementTypeRepository.findByName("xpository:document:validated");
    if (existing.isPresent()) {
      documentType = existing.get();
      return;
    }

    // Create an element type with a JSON Schema
    documentType = new ElementType();
    documentType.setName("xpository:document:validated");
    documentType.setDescription("A document type with JSON Schema validation");

    // JSON Schema: requires title (string), status (enum), publishDate (date)
    String schema =
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "title": {
              "type": "string",
              "minLength": 1
            },
            "status": {
              "type": "string",
              "enum": ["draft", "published", "archived"]
            },
            "publishDate": {
              "type": "string",
              "format": "date"
            }
          },
          "required": ["title", "status"]
        }
        """;
    documentType.setSchemaJson(schema);

    documentType = elementTypeRepository.save(documentType);
  }

  @AfterEach
  void tearDown() {
    // Only delete elements, leave element types for reuse
    // Delete each element individually to avoid issues with null entity references
    elementRepository.findAll().stream()
        .filter(Objects::nonNull)
        .forEach(elementRepository::delete);
  }

  @Test
  void shouldCreateElementWithValidProperties() {
    // Valid properties matching schema
    String validProperties =
        """
        {
          "title": "My Document",
          "status": "published",
          "publishDate": "2026-06-05"
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(validProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    Element saved = elementService.create(element);

    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getElementId()).isNotNull();
    assertThat(saved.getPropertiesJson()).isEqualTo(validProperties);
  }

  @Test
  void shouldRejectElementWithInvalidProperties() {
    // Invalid: missing required "status" field
    String invalidProperties =
        """
        {
          "title": "My Document"
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(invalidProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    assertThatThrownBy(() -> elementService.create(element))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Element properties do not match the schema")
        .hasMessageContaining("xpository:document:validated");
  }

  @Test
  void shouldRejectElementWithWrongPropertyType() {
    // Invalid: status is number instead of string
    String invalidProperties =
        """
        {
          "title": "My Document",
          "status": 123
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(invalidProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    assertThatThrownBy(() -> elementService.create(element))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Element properties do not match the schema");
  }

  @Test
  void shouldRejectElementWithInvalidEnumValue() {
    // Invalid: status value not in enum
    String invalidProperties =
        """
        {
          "title": "My Document",
          "status": "pending"
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(invalidProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    assertThatThrownBy(() -> elementService.create(element))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Element properties do not match the schema");
  }

  @Test
  void shouldAllowElementWithNoSchemaDefinedInType() {
    // Create or reuse an element type without a schema
    ElementType simpleType =
        elementTypeRepository
            .findByName("xpository:simple:noschema")
            .orElseGet(
                () -> {
                  ElementType type = new ElementType();
                  type.setName("xpository:simple:noschema");
                  type.setDescription("Type without schema validation");
                  type.setSchemaJson(null); // No schema
                  return elementTypeRepository.save(type);
                });

    // Any properties should be accepted
    String anyProperties =
        """
        {
          "foo": "bar",
          "baz": 123
        }
        """;

    Element element = new Element();
    element.setType(simpleType);
    element.setPropertiesJson(anyProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    Element saved = elementService.create(element);

    assertThat(saved).isNotNull();
    assertThat(saved.getPropertiesJson()).isEqualTo(anyProperties);
  }

  @Test
  void shouldValidateSchemaOnUpdate() {
    // First create with valid properties
    String validProperties =
        """
        {
          "title": "My Document",
          "status": "draft"
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(validProperties);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    Element saved = elementService.create(element);

    // Now try to update with invalid properties (missing required "status" field)
    String invalidProperties =
        """
        {
          "title": "Updated Document"
        }
        """;

    saved.setPropertiesJson(invalidProperties);
    saved.setModifiedBy("testuser");

    // Update should fail validation - this is the key test
    assertThatThrownBy(() -> elementService.update(saved))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Element properties do not match the schema");
  }

  @Test
  void shouldAllowAdditionalPropertiesByDefault() {
    // Properties with extra fields not defined in schema
    String propertiesWithExtra =
        """
        {
          "title": "My Document",
          "status": "published",
          "extraField": "this is not in the schema"
        }
        """;

    Element element = new Element();
    element.setType(documentType);
    element.setPropertiesJson(propertiesWithExtra);
    element.setCreatedBy("testuser");
    element.setModifiedBy("testuser");

    // By default, JSON Schema allows additional properties
    Element saved = elementService.create(element);

    assertThat(saved).isNotNull();
    assertThat(saved.getPropertiesJson()).isEqualTo(propertiesWithExtra);
  }
}
