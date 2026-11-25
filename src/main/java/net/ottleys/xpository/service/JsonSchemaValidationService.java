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

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for validating JSON data against JSON Schema definitions.
 *
 * <p>Uses the networknt json-schema-validator library to perform JSON Schema validation according
 * to the JSON Schema Draft 2020-12 specification.
 */
@Service
public class JsonSchemaValidationService {

  private final ObjectMapper objectMapper;
  private final SchemaRegistry schemaRegistry;

  /**
   * Constructs a JsonSchemaValidationService.
   *
   * @param objectMapper Jackson ObjectMapper for parsing JSON
   */
  public JsonSchemaValidationService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;

    // In Draft 2020-12, format assertions (e.g., "format": "email") are disabled by default
    // unless explicitly enabled in SchemaRegistryConfig.
    SchemaRegistryConfig config =
        SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build();

    this.schemaRegistry =
        SchemaRegistry.withDefaultDialect(
            SpecificationVersion.DRAFT_2020_12, // JSON Schema Draft 2020-12
            builder -> builder.schemaRegistryConfig(config));
  }

  /**
   * Validates JSON data against a JSON Schema.
   *
   * @param jsonData the JSON data to validate (as a string)
   * @param schemaJson the JSON Schema to validate against (as a string)
   * @return list of validation error messages (empty if valid)
   * @throws IllegalArgumentException if the schema or data is malformed
   */
  public List<String> validate(String jsonData, String schemaJson) {
    if (schemaJson == null || schemaJson.isBlank()) {
      // No schema defined - no validation needed
      return List.of();
    }

    // Treat null/blank data as empty object for validation
    String dataToValidate = (jsonData == null || jsonData.isBlank()) ? "{}" : jsonData;

    try {
      // Parse schema
      JsonNode schemaNode = objectMapper.readTree(schemaJson);
      Schema schema = schemaRegistry.getSchema(schemaNode);

      // Parse data
      JsonNode dataNode = objectMapper.readTree(dataToValidate);

      // Validate
      List<Error> errors = schema.validate(dataNode);

      // Convert to list of error messages (format: "/propertyName: error message")
      return errors.stream().map(Error::getMessage).toList();

    } catch (JacksonException e) {
      throw new IllegalArgumentException("Failed to validate JSON: " + e.getMessage(), e);
    }
  }
}
