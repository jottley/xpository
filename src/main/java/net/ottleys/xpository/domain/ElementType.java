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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

/**
 * Represents a type definition for elements with custom properties defined via JSON schema.
 *
 * <p>ElementTypes define the structure and validation rules for elements through a flexible JSON
 * schema that supports inheritance, custom properties, and validation constraints. Types can be
 * marked as abstract and form inheritance hierarchies.
 */
@Entity
@Table(name = "element_types")
public class ElementType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Element type name is required")
  @Size(min = 1, max = 255, message = "Element type name must be between 1 and 255 characters")
  @FullTextField
  @Column(nullable = false, unique = true)
  private String name;

  @Size(max = 65535, message = "Description cannot exceed 65535 characters")
  @Column(columnDefinition = "TEXT")
  private String description;

  // Schema definition for custom properties (JSON)
  @Size(max = 65535, message = "Schema JSON cannot exceed 65535 characters")
  @Column(columnDefinition = "TEXT")
  private String schemaJson;

  // Constructors
  public ElementType() {}

  public ElementType(String name) {
    this.name = name;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSchemaJson() {
    return schemaJson;
  }

  public void setSchemaJson(String schemaJson) {
    this.schemaJson = schemaJson;
  }

  @PreRemove
  private void preventSystemElementTypeDeletion() {
    if (this.name != null && this.name.startsWith("xpository:")) { // or this.systemType
      throw new IllegalStateException("Cannot delete built-in element type: " + this.name);
    }
  }
}
