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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import net.ottleys.xpository.config.ElementTypeProperties;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.repository.ElementTypeRepository;

/**
 * Service responsible for loading and validating element type definitions at application startup.
 *
 * <p>Scans configured locations for JSON element type definitions, validates inheritance
 * relationships, enforces security constraints (file size limits), and creates ElementType entities
 * in dependency order. Supports both classpath and filesystem resources with configurable scanning
 * locations.
 */
@Service
public class ElementTypeLoaderService {

  private static final Logger logger = LoggerFactory.getLogger(ElementTypeLoaderService.class);

  private final ElementTypeRepository elementTypeRepository;
  private final ObjectMapper objectMapper;
  private final ElementTypeProperties properties;

  public ElementTypeLoaderService(
      ElementTypeRepository elementTypeRepository,
      ObjectMapper objectMapper,
      ElementTypeProperties properties) {
    this.elementTypeRepository = elementTypeRepository;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  /**
   * Loads all element type definitions from configured locations at application startup.
   *
   * <p>Scans each configured location for JSON files, validates their schemas, sorts them by
   * dependency order (parents before children), and creates ElementType entities in the database.
   */
  @PostConstruct
  @Transactional
  public void loadElementTypes() {
    logger.info("Loading element types from {} location(s)...", properties.getLocations().size());

    List<TypeDefinition> definitions = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    // Load from each configured location in order
    for (String location : properties.getLocations()) {
      try {
        Resource[] resources = resolver.getResources(location);

        if (resources.length == 0) {
          logger.debug("No element type definitions found at {}", location);
          continue;
        }

        logger.info("Found {} type definition(s) at {}", resources.length, location);

        // Load all type definitions from this location
        for (Resource resource : resources) {
          try {
            TypeDefinition def = loadTypeDefinition(resource);
            definitions.add(def);
            logger.debug("Loaded type definition: {}", def.namespace);
          } catch (IOException e) {
            logger.error(
                "Failed to load type definition from {}: {}",
                resource.getFilename(),
                e.getMessage());
          }
        }
      } catch (IOException e) {
        logger.error("Failed to scan location {}: {}", location, e.getMessage());
      }
    }

    // Sort by dependency order (parents before children)
    List<TypeDefinition> sorted = sortByDependency(definitions);

    // Create or update types
    int created = 0;
    int skipped = 0;
    for (TypeDefinition def : sorted) {
      try {
        if (elementTypeRepository.existsByName(def.namespace)) {
          logger.debug("Element type already exists, skipping: {}", def.namespace);
          skipped++;
        } else {
          createElementType(def);
          created++;
          logger.info("Created element type: {}", def.namespace);
        }
      } catch (Exception e) {
        logger.error("Failed to create element type {}: {}", def.namespace, e.getMessage());
      }
    }

    logger.info(
        "Element type loading complete. Created: {}, Skipped: {}, Total: {}",
        created,
        skipped,
        definitions.size());
  }

  /**
   * Loads a single type definition from a resource file.
   *
   * @param resource the Spring Resource containing the JSON type definition
   * @return parsed TypeDefinition with metadata and schema
   * @throws IOException if the file cannot be read
   * @throws SecurityException if the file exceeds the maximum allowed size
   */
  private TypeDefinition loadTypeDefinition(Resource resource) throws IOException {
    logger.debug("Loading type definition from resource: {}", resource.getFilename());
    // Security: Validate file size to prevent DoS
    long contentLength = resource.contentLength();
    if (contentLength > properties.getMaxFileSize()) {
      throw new SecurityException(
          "Element type file exceeds maximum size: "
              + resource.getFilename()
              + " ("
              + contentLength
              + " bytes, max: "
              + properties.getMaxFileSize()
              + ")");
    }

    try (InputStream is = resource.getInputStream()) {
      JsonNode root = objectMapper.readTree(is);
      JsonNode ext = root.path("x-xpository");

      if (ext.isMissingNode() || !ext.isObject()) {
        throw new IllegalArgumentException(
            "Missing or invalid 'x-xpository' extension in file: " + resource.getFilename());
      }

      String namespace = getStandardFieldValues(ext, "namespace");
      String version = getStandardFieldValues(ext, "version");
      String displayName = getStandardFieldValues(ext, "displayName");
      String description = getStandardFieldValues(root, "description");
      String parent =
          ext.has("parent") && !ext.get("parent").isNull() ? ext.get("parent").asText() : null;

      boolean isAbstract = ext.path("metadata").path("abstract").asBoolean(false);

      // Store entire JSON as schema
      String schemaJson = objectMapper.writeValueAsString(root);

      return new TypeDefinition(
          namespace, version, displayName, description, parent, isAbstract, schemaJson);
    }
  }

  /**
   * Creates an ElementType entity from a type definition.
   *
   * <p>Validates inheritance rules including abstract/concrete type constraints and ensures parent
   * types exist before creating child types.
   *
   * @param def the type definition to create
   * @throws IllegalStateException if parent type is missing or inheritance rules are violated
   */
  private void createElementType(TypeDefinition def) {
    // Validate inheritance rules
    if (def.parent != null) {
      Optional<ElementType> parentOpt = elementTypeRepository.findByName(def.parent);
      if (parentOpt.isEmpty()) {
        throw new IllegalStateException(
            "Parent type '" + def.parent + "' not found for type '" + def.namespace + "'");
      }

      // Validate: Abstract cannot inherit from Concrete
      ElementType parentType = parentOpt.get();
      boolean parentIsAbstract = isAbstract(parentType);

      if (def.isAbstract && !parentIsAbstract) {
        throw new IllegalStateException(
            "Abstract type '"
                + def.namespace
                + "' cannot inherit from concrete type '"
                + def.parent
                + "'");
      }
    }

    ElementType elementType = new ElementType();
    elementType.setName(def.namespace);
    elementType.setDescription(def.description);

    // For built-in types we store the raw schema JSON for validation.
    elementType.setSchemaJson(def.schemaJson);

    elementTypeRepository.save(elementType);
  }

  /**
   * Checks if an element type is marked as abstract in its schema.
   *
   * @param type the ElementType to check
   * @return true if the type has metadata.abstract = true, false otherwise
   */
  private boolean isAbstract(ElementType type) {
    try {
      JsonNode schema = objectMapper.readTree(type.getSchemaJson());
      if (schema.has("metadata")) {
        JsonNode metadata = schema.get("metadata");
        if (metadata.has("abstract")) {
          return metadata.get("abstract").asBoolean();
        }
      }
    } catch (JsonProcessingException e) {
      logger.warn("Failed to parse schema for type {}: {}", type.getName(), e.getMessage());
    }
    return false;
  }

  /**
   * Sorts type definitions in dependency order with parent types before children.
   *
   * @param definitions the list of type definitions to sort
   * @return sorted list with all parent types appearing before their children
   */
  private List<TypeDefinition> sortByDependency(List<TypeDefinition> definitions) {
    List<TypeDefinition> sorted = new ArrayList<>();
    Set<String> processed = new HashSet<>();
    Map<String, TypeDefinition> byNamespace = new HashMap<>();

    for (TypeDefinition def : definitions) {
      byNamespace.put(def.namespace, def);
    }

    // Process types with no parent first, then their children
    for (TypeDefinition def : definitions) {
      addWithDependencies(def, byNamespace, processed, sorted);
    }

    return sorted;
  }

  /**
   * Recursively adds a type definition and its dependencies to the sorted list.
   *
   * <p>Ensures parent types are processed before their children using depth-first traversal.
   *
   * @param def the type definition to add
   * @param byNamespace map of namespace to type definition for parent lookup
   * @param processed set of already processed namespaces to prevent duplicates
   * @param sorted the result list to append to
   */
  private void addWithDependencies(
      TypeDefinition def,
      Map<String, TypeDefinition> byNamespace,
      Set<String> processed,
      List<TypeDefinition> sorted) {
    if (processed.contains(def.namespace)) {
      return;
    }

    // Process parent first
    if (def.parent != null && byNamespace.containsKey(def.parent)) {
      addWithDependencies(byNamespace.get(def.parent), byNamespace, processed, sorted);
    }

    sorted.add(def);
    processed.add(def.namespace);
  }

  /**
   * Retrieves the value of a standard field from a JSON node.
   *
   * @param node the JSON node to extract the field from
   * @param fieldName the name of the field to retrieve
   * @return the field value as a string, or null if missing or null
   */
  private String getStandardFieldValues(JsonNode node, String fieldName) throws IOException {
    if (node.has(fieldName) && !node.get(fieldName).isNull()) {
      return node.get(fieldName).asText();
    } else {
      if (logger.isWarnEnabled()) {
        logger.warn("Expected field \"{}\" is missing or null", fieldName);
      }
      throw new IOException("Expected field \"" + fieldName + "\" is missing or null");
    }
  }

  private record TypeDefinition(
      String namespace,
      String version,
      String displayName,
      String description,
      String parent,
      boolean isAbstract,
      String schemaJson) {}
}
