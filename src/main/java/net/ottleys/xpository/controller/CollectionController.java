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
package net.ottleys.xpository.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.CollectionType;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.CreateCollectionRequestDto;
import net.ottleys.xpository.dto.CreateElementRequestDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.UpdateCollectionRequestDto;
import net.ottleys.xpository.exception.ResourceNotFoundException;
import net.ottleys.xpository.mapper.CollectionMapper;
import net.ottleys.xpository.mapper.ElementMapper;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import net.ottleys.xpository.security.SecurityUtils;
import net.ottleys.xpository.service.CollectionService;
import net.ottleys.xpository.service.ElementCollectionMembershipService;
import net.ottleys.xpository.service.ElementService;

/**
 * REST API controller for Collection resources.
 *
 * <p>Provides CRUD operations for collections following REST conventions. All endpoints are under
 * /api/v1/collections. This controller is thin - all business logic is delegated to the service
 * layer. Uses DTOs for request/response to separate API contract from domain model.
 *
 * <p>Authentication and authorization will be added in a future release. Currently uses "system" as
 * the default user for created/modified by fields.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

  private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

  private final CollectionService collectionService;
  private final CollectionMapper collectionMapper;
  private final ElementService elementService;
  private final ElementMapper elementMapper;
  private final ElementTypeRepository elementTypeRepository;
  private final ElementCollectionMembershipService membershipService;
  private final ElementCollectionMembershipRepository membershipRepository;
  private final net.ottleys.xpository.service.ElementRelationshipService relationshipService;

  /**
   * Constructs a CollectionController with required dependencies.
   *
   * @param collectionService service for collection business logic
   * @param collectionMapper mapper for entity/DTO conversion
   * @param elementService service for element business logic
   * @param elementMapper mapper for element entity/DTO conversion
   * @param elementTypeRepository repository for element type resolution
   * @param membershipService service for element-collection membership management
   * @param membershipRepository repository for membership queries
   * @param relationshipService service for element relationship management
   */
  public CollectionController(
      CollectionService collectionService,
      CollectionMapper collectionMapper,
      ElementService elementService,
      ElementMapper elementMapper,
      ElementTypeRepository elementTypeRepository,
      ElementCollectionMembershipService membershipService,
      ElementCollectionMembershipRepository membershipRepository,
      net.ottleys.xpository.service.ElementRelationshipService relationshipService) {
    this.collectionService = collectionService;
    this.collectionMapper = collectionMapper;
    this.elementService = elementService;
    this.elementMapper = elementMapper;
    this.elementTypeRepository = elementTypeRepository;
    this.membershipService = membershipService;
    this.membershipRepository = membershipRepository;
    this.relationshipService = relationshipService;
  }

  /**
   * Lists all collections with optional filtering and pagination.
   *
   * <p>GET /api/v1/collections?page=0&size=20&sort=name,asc&type=TREE&name=project
   *
   * <p>Supports multiple query parameters for filtering: - parentId: Filter by parent collection
   * UUID (use "null" string for top-level collections) - type: Filter by collection type (LIST,
   * TREE, GRAPH, STACK) - systemCollection: Filter by system collection flag (true/false) - name:
   * Search by name (case-insensitive partial match)
   *
   * <p>All filters are optional and combine with AND logic.
   *
   * @param parentId filter by parent collection UUID (use "null" for top-level)
   * @param type filter by collection type
   * @param systemCollection filter by system collection flag
   * @param name search by name (partial match)
   * @param pageable pagination parameters (defaults: page=0, size=20)
   * @return page of collection response DTOs matching the filters
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "List and search collections",
      description =
          "Retrieves a paginated, filterable list of non-deleted collections. Supports filtering by parent, type, system flag, and name search. All filters are optional and combine with AND logic.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved collections",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class)))
      })
  @GetMapping
  public ResponseEntity<Page<CollectionResponseDto>> listCollections(
      @Parameter(
              description =
                  "Filter by parent collection UUID (use 'null' for top-level collections, omit to ignore parent)",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @RequestParam(required = false)
          String parentId,
      @Parameter(
              description = "Filter by collection type",
              example = "TREE",
              schema = @Schema(allowableValues = {"LIST", "TREE", "GRAPH", "STACK"}))
          @RequestParam(required = false)
          CollectionType type,
      @Parameter(description = "Filter by system collection flag", example = "false")
          @RequestParam(required = false)
          Boolean systemCollection,
      @Parameter(
              description = "Search by name (case-insensitive partial match)",
              example = "project")
          @RequestParam(required = false)
          String name,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

    // Check if any filters are provided
    boolean hasFilters =
        parentId != null
            || type != null
            || systemCollection != null
            || (name != null && !name.isBlank());

    Page<Collection> collections;
    if (hasFilters) {
      // Parse parentId - handle "null" string for top-level collections
      UUID parentUuid = null;
      boolean includeParentFilter = false;
      if (parentId != null) {
        includeParentFilter = true;
        if (!"null".equalsIgnoreCase(parentId)) {
          try {
            parentUuid = UUID.fromString(parentId);
          } catch (IllegalArgumentException e) {
            // Invalid UUID format - let validation handle it
            // For now, treat as null (top-level)
          }
        }
      }

      logger.info(
          "Search Filters - Parent UUID: {}, Include Parent Filter: {}, Type: {}, System Collection: {}, Name: {}",
          parentUuid,
          includeParentFilter,
          type,
          systemCollection,
          name);
      // Use search method with filters
      collections =
          collectionService.searchCollections(
              parentUuid, includeParentFilter, type, systemCollection, name, pageable);
    } else {
      logger.warn("No filters applied, name: {}, using findAll query.", name);
      // No filters - use optimized findAll query
      collections = collectionService.findAll(pageable);
    }

    Page<CollectionResponseDto> responseDtos = collections.map(collectionMapper::toResponseDto);
    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Retrieves a single collection by UUID.
   *
   * <p>GET /api/v1/collections/{collectionId}
   *
   * @param collectionId the public UUID
   * @return 200 OK with collection response DTO if found, 404 Not Found otherwise
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "Get collection by ID",
      description = "Retrieves a single non-deleted collection by its UUID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collection found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CollectionResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}")
  public ResponseEntity<CollectionResponseDto> getCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId) {
    return collectionService
        .findByCollectionId(collectionId)
        .map(collectionMapper::toResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Creates a new collection.
   *
   * <p>POST /api/v1/collections
   *
   * <p>Request body is validated using Bean Validation annotations. The createdBy and modifiedBy
   * fields are set from the authenticated user context.
   *
   * @param requestDto validated create request DTO
   * @return 201 Created with location header and collection response DTO
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "Create a new collection",
      description =
          "Creates a new collection with the provided details. Name and type are required. Returns the created collection with generated UUID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Collection created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CollectionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or validation error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping
  public ResponseEntity<CollectionResponseDto> createCollection(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Collection creation request",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = CreateCollectionRequestDto.class),
                      examples = {
                        @ExampleObject(
                            name = "Simple LIST collection",
                            value =
                                """
                                {
                                  "name": "Documents",
                                  "description": "Document management collection",
                                  "type": "LIST",
                                  "allowedElementTypes": ["xpository:core:document", "xpository:core:folder"],
                                  "inheritPermissions": true
                                }
                                """),
                        @ExampleObject(
                            name = "TREE collection with parent",
                            value =
                                """
                                {
                                  "name": "Projects",
                                  "description": "Project hierarchy",
                                  "type": "TREE",
                                  "configurationJson": "{\\"maxDepth\\": 5}",
                                  "parentCollectionId": "550e8400-e29b-41d4-a716-446655440000",
                                  "inheritPermissions": true
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          CreateCollectionRequestDto requestDto) {
    Collection collection = collectionMapper.toEntity(requestDto);

    // Set audit fields from authenticated user context
    String currentUser = SecurityUtils.getCurrentUsername();
    collection.setCreatedBy(currentUser);
    collection.setModifiedBy(currentUser);


    // Validate allowed element types if provided
    if (requestDto.getAllowedElementTypes() != null && !requestDto.getAllowedElementTypes().isEmpty()) {
        // Validate allowed element types exist
        for (String elementType : requestDto.getAllowedElementTypes()) {
            if (!elementService.isValidElementType(elementType)) {
            throw new IllegalArgumentException("Invalid element type: " + elementType);
            }
        }
    }

    if (requestDto.getParentCollectionId() != null) {
      // Resolve parent collection from parentCollectionId if provided
      Collection parentCollection = resolveParentCollection(requestDto.getParentCollectionId());
      collection.setParentCollection(parentCollection);

      collection.getAllowedElementTypes()
          .forEach(elementType -> {
            if (!parentCollection.getAllowedElementTypes().contains(elementType)) {
              throw new IllegalArgumentException(
                  "Parent collection does not allow element type: " + elementType);
            }
          });
    }

    if (requestDto.getInheritPermissions() != null) {
        collection.setInheritPermissions(requestDto.getInheritPermissions());
    }

    Collection savedCollection = collectionService.create(collection);
    CollectionResponseDto responseDto = collectionMapper.toResponseDto(savedCollection);

    // Return 201 Created with Location header pointing to the new resource
    String locationUri = "/api/v1/collections/" + savedCollection.getCollectionId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION, locationUri)
        .body(responseDto);
  }

  private Collection resolveParentCollection(UUID parentCollectionId) {
    if (parentCollectionId == null) {
      return null;
    }
    Collection parentCollection = collectionService.findById(parentCollectionId);
    if (parentCollection == null) {
      throw new ResourceNotFoundException("Parent collection not found");
    }
    return parentCollection;
  }

  /**
   * Fully replaces an existing collection (PUT).
   *
   * <p>PUT /api/v1/collections/{collectionId}
   *
   * <p>All fields in the request replace the existing collection. Fields not provided will be set
   * to null/default values. This is different from PATCH which only updates provided fields.
   *
   * @param collectionId the public UUID of collection to replace
   * @param requestDto validated create request DTO with all fields
   * @return 200 OK with updated collection, 404 Not Found if collection doesn't exist
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "Replace a collection (full update)",
      description =
          "Completely replaces an existing collection. All fields must be provided. Unspecified fields are set to null/default.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collection replaced successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CollectionResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PutMapping("/{collectionId}")
  public ResponseEntity<CollectionResponseDto> replaceCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Complete collection data (all fields required)",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = CreateCollectionRequestDto.class),
                      examples = {
                        @ExampleObject(
                            name = "Replace with TREE collection",
                            value =
                                """
                                {
                                  "name": "Updated Collection",
                                  "description": "Completely replaced collection",
                                  "type": "TREE",
                                  "configurationJson": "{\\"maxDepth\\": 5}",
                                  "allowedElementTypes": ["xpository:core:document", "xpository:core:folder"],
                                  "metadataJson": "{\\"department\\": \\"Engineering\\"}",
                                  "validationRulesJson": null,
                                  "parentCollectionId": null,
                                  "inheritPermissions": true
                                }
                                """),
                        @ExampleObject(
                            name = "Replace minimal LIST collection",
                            value =
                                """
                                {
                                  "name": "Simple List",
                                  "description": null,
                                  "type": "LIST",
                                  "configurationJson": null,
                                  "allowedElementTypes": null,
                                  "metadataJson": null,
                                  "validationRulesJson": null,
                                  "parentCollectionId": null,
                                  "inheritPermissions": true
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          CreateCollectionRequestDto requestDto) {
    return collectionService
        .findByCollectionId(collectionId)
        .map(existing -> {
          // Replace all fields from request DTO
          Collection updated = collectionMapper.toEntity(requestDto);
          updated.setId(existing.getId()); // Keep internal ID
          updated.setCollectionId(existing.getCollectionId()); // Keep UUID
          updated.setCreatedBy(existing.getCreatedBy()); // Keep creation audit
          updated.setCreatedDate(existing.getCreatedDate());
          updated.setModifiedBy(SecurityUtils.getCurrentUsername()); // Update modification audit

          // TODO: Resolve parent collection from parentCollectionId if provided
          // TODO: Validate allowed element types exist

          Collection savedCollection = collectionService.update(updated);
          return ResponseEntity.ok(collectionMapper.toResponseDto(savedCollection));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Partially updates an existing collection (PATCH).
   *
   * <p>PATCH /api/v1/collections/{collectionId}
   *
   * <p>Only fields provided in the request are updated. Null fields are ignored. This allows
   * clients to update specific fields without needing to send the entire resource.
   *
   * @param collectionId the public UUID of collection to update
   * @param requestDto update request DTO with optional fields
   * @return 200 OK with updated collection, 404 Not Found if collection doesn't exist
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "Partially update a collection",
      description =
          "Updates only the fields provided in the request. Null/missing fields are ignored and retain their current values.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collection updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CollectionResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PatchMapping("/{collectionId}")
  public ResponseEntity<CollectionResponseDto> updateCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Fields to update (only non-null fields are updated)",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = UpdateCollectionRequestDto.class),
                      examples = {
                        @ExampleObject(
                            name = "Update name only",
                            value =
                                """
                                {
                                  "name": "Updated Collection Name"
                                }
                                """),
                        @ExampleObject(
                            name = "Update description and metadata",
                            value =
                                """
                                {
                                  "description": "Updated description",
                                  "metadataJson": "{\\"department\\": \\"Engineering\\", \\"priority\\": \\"high\\"}"
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          UpdateCollectionRequestDto requestDto) {
    return collectionService
        .findByCollectionId(collectionId)
        .map(existing -> {
          // Update only non-null fields from request DTO
          collectionMapper.updateEntity(existing, requestDto);
          existing.setModifiedBy(SecurityUtils.getCurrentUsername());

          // TODO: Resolve parent collection from parentCollectionId if provided
          // TODO: Validate allowed element types exist

          Collection savedCollection = collectionService.update(existing);
          return ResponseEntity.ok(collectionMapper.toResponseDto(savedCollection));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Soft deletes a collection by UUID.
   *
   * <p>DELETE /api/v1/collections/{collectionId}
   *
   * <p>Performs a soft delete - marks the collection as deleted without removing it from the
   * database. The collection will no longer appear in list queries but can be restored or
   * hard-deleted by administrators.
   *
   * @param collectionId the public UUID of collection to delete
   * @return 204 No Content if deleted successfully, 404 Not Found if collection doesn't exist
   */
  @Tag(
      name = "Collections",
      description = "APIs for managing collections - containers for organizing elements")
  @Operation(
      summary = "Delete a collection (soft delete)",
      description =
          "Soft deletes a collection by marking it as deleted. The collection is hidden from queries but remains in the database for potential recovery.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Collection deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{collectionId}")
  public ResponseEntity<Void> deleteCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId) {
    boolean deleted =
        collectionService.deleteByCollectionId(collectionId, SecurityUtils.getCurrentUsername());
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // ==================== Nested Element Operations ====================

  /**
   * Creates a new element within a collection.
   *
   * <p>POST /api/v1/collections/{collectionId}/elements
   *
   * <p>Elements must always belong to at least one collection and cannot exist independently. This
   * endpoint creates an element and automatically adds it to the specified collection.
   *
   * @param collectionId the UUID of the collection to add the element to
   * @param requestDto validated create request DTO
   * @return 201 Created with location header and element response DTO
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Create a new element in a collection",
      description =
          "Creates a new element within the specified collection. Element type is required and must exist. Elements cannot exist without belonging to a collection. Returns the created element with generated UUID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Element created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or validation error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Element type does not exist",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/elements")
  public ResponseEntity<ElementResponseDto> createElementInCollection(
      @Parameter(
              description = "Collection UUID to add element to",
              example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Element creation request",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = CreateElementRequestDto.class),
                      examples = {
                        @ExampleObject(
                            name = "Standard element",
                            value =
                                """
                                {
                                  "elementTypeName": "xpository:core:standard",
                                  "propertiesJson": "{\\"title\\": \\"My Document\\", \\"description\\": \\"Sample document\\", \\"status\\": \\"draft\\"}"
                                }
                                """),
                        @ExampleObject(
                            name = "Document element",
                            value =
                                """
                                {
                                  "elementTypeName": "xpository:document:generic",
                                  "propertiesJson": "{\\"title\\": \\"Project Report\\", \\"author\\": \\"John Doe\\", \\"publishDate\\": \\"2026-06-05\\", \\"language\\": \\"en\\", \\"confidentiality\\": \\"internal\\"}"
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          CreateElementRequestDto requestDto) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    Element element = elementMapper.toEntity(requestDto);

    // Resolve element type from name
    Optional<ElementType> elementTypeOpt =
        elementTypeRepository.findByName(requestDto.getElementTypeName());
    if (elementTypeOpt.isEmpty()) {
      throw new IllegalArgumentException(
          "Element type with name " + requestDto.getElementTypeName() + " does not exist");
    }
    element.setType(elementTypeOpt.get());

    // Validate element type is allowed in collection
    membershipService.validateElementTypeAllowed(element, collection);

    // Set audit fields from authenticated user context
    element.setCreatedBy(SecurityUtils.getCurrentUsername());
    element.setModifiedBy(SecurityUtils.getCurrentUsername());

    // Create element
    Element savedElement = elementService.create(element);

    // Resolve parent element if provided
    Element parentElement = null;
    if (requestDto.getParentElementId() != null) {
      parentElement =
          elementService
              .findByElementId(requestDto.getParentElementId())
              .orElseThrow(() ->
                  new IllegalArgumentException(
                      "Parent element with UUID "
                          + requestDto.getParentElementId()
                          + " does not exist"));
    }

    // Link element to collection with optional parent
    membershipService.addElementToCollection(
        savedElement, collection, SecurityUtils.getCurrentUsername(), parentElement);

    ElementResponseDto responseDto = elementMapper.toResponseDto(savedElement);

    // Return 201 Created with Location header pointing to the new resource
    String locationUri = "/api/v1/elements/" + savedElement.getElementId();
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION, locationUri)
        .body(responseDto);
  }

  /**
   * Lists all elements in a collection.
   *
   * <p>GET /api/v1/collections/{collectionId}/elements
   *
   * @param collectionId the UUID of the collection
   * @param pageable pagination parameters
   * @return page of element response DTOs
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "List elements in a collection",
      description =
          "Retrieves all elements that belong to the specified collection with pagination support.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved elements",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements")
  public ResponseEntity<Page<ElementResponseDto>> listElementsInCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

    // Verify collection exists
    Optional<Collection> collectionOpt = collectionService.findByCollectionId(collectionId);
    if (collectionOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Collection collection = collectionOpt.get();

    // For now, this returns all elements (not paginated)
    Page<Element> elementsPage =
        membershipRepository.findElementsByCollection(collection, pageable);

    // Convert to response DTOs
    Page<ElementResponseDto> page = elementsPage.map(elementMapper::toResponseDto);

    // Return the page of element response DTOs
    return ResponseEntity.ok(page);
  }

  /**
   * Adds an existing element to a collection.
   *
   * <p>POST /api/v1/collections/{collectionId}/elements/{elementId}
   *
   * <p>This endpoint allows adding an element that already exists to another collection. Elements
   * can belong to multiple collections simultaneously.
   *
   * @param collectionId the UUID of the collection
   * @param elementId the UUID of the element to add
   * @return 204 No Content if successful
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Add existing element to collection",
      description =
          "Adds an existing element to the specified collection. Elements can belong to multiple collections. This operation is idempotent - no error if element is already in the collection.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Element added successfully (or already present)"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/elements/{elementId}")
  public ResponseEntity<Void> addElementToCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(
              description = "Element UUID to add",
              example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Optional<Collection> collectionOpt = collectionService.findByCollectionId(collectionId);
    if (collectionOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Verify element exists
    Optional<Element> elementOpt = elementService.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Validate element type is allowed in collection
    membershipService.validateElementTypeAllowed(elementOpt.get(), collectionOpt.get());

    // Add element to collection (idempotent)
    membershipService.addElementToCollection(
        elementOpt.get(), collectionOpt.get(), SecurityUtils.getCurrentUsername());

    return ResponseEntity.noContent().build();
  }

  /**
   * Removes an element from a collection.
   *
   * <p>DELETE /api/v1/collections/{collectionId}/elements/{elementId}
   *
   * <p>Removes the element from the specified collection. The element itself is not deleted and
   * remains in other collections it belongs to.
   *
   * @param collectionId the UUID of the collection
   * @param elementId the UUID of the element to remove
   * @return 204 No Content if successful, 404 if element not in collection
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Remove element from collection",
      description =
          "Removes an element from the specified collection. The element is not deleted and remains in other collections. Returns 404 if the element is not in this collection.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Element removed successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found, element not found, or element not in collection",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{collectionId}/elements/{elementId}")
  public ResponseEntity<Void> removeElementFromCollection(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(
              description = "Element UUID to remove",
              example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Optional<Collection> collectionOpt = collectionService.findByCollectionId(collectionId);
    if (collectionOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Verify element exists
    Optional<Element> elementOpt = elementService.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Remove element from collection
    boolean removed =
        membershipService.removeElementFromCollection(elementOpt.get(), collectionOpt.get());

    return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /**
   * Moves an element up in the order (LIST collections only).
   *
   * <p>POST /api/v1/collections/{collectionId}/elements/{elementId}/move-up
   *
   * <p>Decreases the orderPosition by 1 by swapping with the element above it. Only works for LIST
   * collections.
   *
   * @param collectionId the UUID of the LIST collection
   * @param elementId the UUID of the element to move up
   * @return 204 No Content if moved, 200 OK if already at top, 404 if not found
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Move element up in LIST collection",
      description =
          "Moves an element up in the order by swapping positions with the element above it. Only works for LIST collections. Returns 200 if already at the top.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Element moved up successfully"),
        @ApiResponse(responseCode = "200", description = "Element already at top position"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a LIST type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/elements/{elementId}/move-up")
  public ResponseEntity<Void> moveElementUp(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Move element up
    boolean moved = membershipService.moveElementUp(element, collection);

    return moved ? ResponseEntity.noContent().build() : ResponseEntity.ok().build();
  }

  /**
   * Moves an element down in the order (LIST collections only).
   *
   * <p>POST /api/v1/collections/{collectionId}/elements/{elementId}/move-down
   *
   * <p>Increases the orderPosition by 1 by swapping with the element below it. Only works for LIST
   * collections.
   *
   * @param collectionId the UUID of the LIST collection
   * @param elementId the UUID of the element to move down
   * @return 204 No Content if moved, 200 OK if already at bottom, 404 if not found
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Move element down in LIST collection",
      description =
          "Moves an element down in the order by swapping positions with the element below it. Only works for LIST collections. Returns 200 if already at the bottom.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Element moved down successfully"),
        @ApiResponse(responseCode = "200", description = "Element already at bottom position"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a LIST type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/elements/{elementId}/move-down")
  public ResponseEntity<Void> moveElementDown(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Move element down
    boolean moved = membershipService.moveElementDown(element, collection);

    return moved ? ResponseEntity.noContent().build() : ResponseEntity.ok().build();
  }

  /**
   * Reorders an element to a specific position (LIST collections only).
   *
   * <p>POST /api/v1/collections/{collectionId}/elements/{elementId}/reorder?position={newPosition}
   *
   * <p>Moves an element to the specified position, shifting other elements accordingly. Only works
   * for LIST collections.
   *
   * @param collectionId the UUID of the LIST collection
   * @param elementId the UUID of the element to reorder
   * @param newPosition the target position (0-based)
   * @return 204 No Content if reordered successfully
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Reorder element to specific position in LIST collection",
      description =
          "Moves an element to a specific position in the order. All elements between the old and new positions are shifted accordingly. Only works for LIST collections.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Element reordered successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a LIST type or position is invalid",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/elements/{elementId}/reorder")
  public ResponseEntity<Void> reorderElement(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId,
      @Parameter(description = "Target position (0-based)", example = "3", required = true)
          @RequestParam
          int position) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Reorder element
    membershipService.reorderElement(element, collection, position);

    return ResponseEntity.noContent().build();
  }

  // ==================== TREE Collection Operations ====================

  /**
   * Gets all descendant elements in a subtree (TREE collections only).
   *
   * <p>GET /api/v1/collections/{collectionId}/elements/{elementId}/subtree
   *
   * <p>Returns all descendants (children, grandchildren, etc.) of the specified element. Uses
   * materialized path for efficient querying.
   *
   * @param collectionId the UUID of the TREE collection
   * @param elementId the UUID of the root element of the subtree
   * @return list of all descendant elements
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Get subtree of element in TREE collection",
      description =
          "Retrieves all descendant elements (children, grandchildren, etc.) of the specified element. Only works for TREE collections.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subtree retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a TREE type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements/{elementId}/subtree")
  public ResponseEntity<List<ElementResponseDto>> getSubtree(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Get subtree
    List<Element> subtreeElements = membershipService.getSubtree(element, collection);
    List<ElementResponseDto> responseDtos =
        subtreeElements.stream().map(elementMapper::toResponseDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Gets all ancestor elements (TREE collections only).
   *
   * <p>GET /api/v1/collections/{collectionId}/elements/{elementId}/ancestors
   *
   * <p>Returns all ancestors (parent, grandparent, etc.) in order from immediate parent to root.
   *
   * @param collectionId the UUID of the TREE collection
   * @param elementId the UUID of the element
   * @return list of ancestor elements
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Get ancestors of element in TREE collection",
      description =
          "Retrieves all ancestor elements (parent, grandparent, etc.) in order from immediate parent to root. Only works for TREE collections.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ancestors retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a TREE type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements/{elementId}/ancestors")
  public ResponseEntity<List<ElementResponseDto>> getAncestors(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Get ancestors
    List<Element> ancestorElements = membershipService.getAncestors(element, collection);
    List<ElementResponseDto> responseDtos =
        ancestorElements.stream().map(elementMapper::toResponseDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Gets immediate children of an element (TREE collections only).
   *
   * <p>GET /api/v1/collections/{collectionId}/elements/{elementId}/children
   *
   * <p>Returns only direct children (depth = parent.depth + 1).
   *
   * @param collectionId the UUID of the TREE collection
   * @param elementId the UUID of the parent element
   * @return list of child elements
   */
  @Tag(name = "Elements", description = "APIs for managing metadata elements")
  @Operation(
      summary = "Get children of element in TREE collection",
      description =
          "Retrieves immediate children (direct descendants) of the specified element. Only works for TREE collections.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Children retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a TREE type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements/{elementId}/children")
  public ResponseEntity<List<ElementResponseDto>> getChildren(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(description = "Element UUID", example = "660e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Verify element exists
    Element element =
        elementService
            .findByElementId(elementId)
            .orElseThrow(() ->
                new IllegalArgumentException("Element with UUID " + elementId + " does not exist"));

    // Get children
    List<Element> childElements = membershipService.getChildren(element, collection);
    List<ElementResponseDto> responseDtos =
        childElements.stream().map(elementMapper::toResponseDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Gets root elements in a TREE collection.
   *
   * <p>GET /api/v1/collections/{collectionId}/roots
   *
   * <p>Returns all elements at the top level of the tree hierarchy (no parent).
   *
   * @param collectionId the UUID of the TREE collection
   * @return list of root elements
   */
  @Tag(name = "Collections", description = "APIs for managing collections")
  @Operation(
      summary = "Get root elements in TREE collection",
      description =
          "Retrieves all root elements (top-level elements with no parent) in a TREE collection.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Root elements retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a TREE type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/roots")
  public ResponseEntity<List<ElementResponseDto>> getRootElements(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Get root elements
    List<Element> rootElements = membershipService.getRootElements(collection);
    List<ElementResponseDto> responseDtos =
        rootElements.stream().map(elementMapper::toResponseDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Creates a relationship between two elements in a GRAPH collection.
   *
   * <p>POST /api/v1/collections/{collectionId}/relationships
   *
   * <p>Relationships are directed (source → target) with a type (e.g., "depends-on"). Multiple
   * relationship types can exist between the same two elements.
   *
   * @param collectionId the UUID of the GRAPH collection
   * @param requestDto validated create relationship request DTO
   * @return 201 Created with relationship response DTO
   */
  @Tag(
      name = "Relationships",
      description = "APIs for managing element relationships in GRAPH collections")
  @Operation(
      summary = "Create a relationship in GRAPH collection",
      description =
          "Creates a directed relationship between two elements in a GRAPH collection. Relationships have types (e.g., 'depends-on', 'references') and optional JSON metadata.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Relationship created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation =
                                net.ottleys.xpository.dto.RelationshipResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or validation error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a GRAPH type or relationship already exists",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/relationships")
  public ResponseEntity<net.ottleys.xpository.dto.RelationshipResponseDto> createRelationship(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Valid @RequestBody net.ottleys.xpository.dto.CreateRelationshipRequestDto requestDto) {

    net.ottleys.xpository.domain.ElementRelationship relationship =
        relationshipService.createRelationship(
            collectionId,
            requestDto.getSourceElementId(),
            requestDto.getTargetElementId(),
            requestDto.getRelationshipType(),
            requestDto.getRelationshipMetadata(),
            SecurityUtils.getCurrentUsername());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(net.ottleys.xpository.mapper.RelationshipMapper.toDto(relationship));
  }

  /**
   * Gets all outgoing relationships from an element in a GRAPH collection.
   *
   * <p>GET /api/v1/collections/{collectionId}/elements/{elementId}/relationships/outgoing
   *
   * @param collectionId the UUID of the GRAPH collection
   * @param elementId the UUID of the source element
   * @return list of outgoing relationships
   */
  @Tag(
      name = "Relationships",
      description = "APIs for managing element relationships in GRAPH collections")
  @Operation(
      summary = "Get outgoing relationships",
      description = "Retrieves all relationships where the specified element is the source.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Outgoing relationships retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation =
                                net.ottleys.xpository.dto.RelationshipResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements/{elementId}/relationships/outgoing")
  public ResponseEntity<List<net.ottleys.xpository.dto.RelationshipResponseDto>>
      getOutgoingRelationships(
          @Parameter(
                  description = "Collection UUID",
                  example = "550e8400-e29b-41d4-a716-446655440000")
              @PathVariable
              UUID collectionId,
          @Parameter(
                  description = "Source element UUID",
                  example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
              @PathVariable
              UUID elementId) {

    List<net.ottleys.xpository.domain.ElementRelationship> relationships =
        relationshipService.getOutgoingRelationships(collectionId, elementId);

    List<net.ottleys.xpository.dto.RelationshipResponseDto> responseDtos =
        relationships.stream().map(net.ottleys.xpository.mapper.RelationshipMapper::toDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Gets all incoming relationships to an element in a GRAPH collection.
   *
   * <p>GET /api/v1/collections/{collectionId}/elements/{elementId}/relationships/incoming
   *
   * @param collectionId the UUID of the GRAPH collection
   * @param elementId the UUID of the target element
   * @return list of incoming relationships
   */
  @Tag(
      name = "Relationships",
      description = "APIs for managing element relationships in GRAPH collections")
  @Operation(
      summary = "Get incoming relationships",
      description = "Retrieves all relationships where the specified element is the target.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Incoming relationships retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation =
                                net.ottleys.xpository.dto.RelationshipResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection or element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/elements/{elementId}/relationships/incoming")
  public ResponseEntity<List<net.ottleys.xpository.dto.RelationshipResponseDto>>
      getIncomingRelationships(
          @Parameter(
                  description = "Collection UUID",
                  example = "550e8400-e29b-41d4-a716-446655440000")
              @PathVariable
              UUID collectionId,
          @Parameter(
                  description = "Target element UUID",
                  example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
              @PathVariable
              UUID elementId) {

    List<net.ottleys.xpository.domain.ElementRelationship> relationships =
        relationshipService.getIncomingRelationships(collectionId, elementId);

    List<net.ottleys.xpository.dto.RelationshipResponseDto> responseDtos =
        relationships.stream().map(net.ottleys.xpository.mapper.RelationshipMapper::toDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Deletes a specific relationship between two elements.
   *
   * <p>DELETE
   * /api/v1/collections/{collectionId}/relationships?source={sourceId}&target={targetId}&type={type}
   *
   * @param collectionId the UUID of the GRAPH collection
   * @param sourceElementId the UUID of the source element
   * @param targetElementId the UUID of the target element
   * @param relationshipType the relationship type
   * @return 204 No Content
   */
  @Tag(
      name = "Relationships",
      description = "APIs for managing element relationships in GRAPH collections")
  @Operation(
      summary = "Delete a relationship",
      description = "Deletes a specific relationship between two elements in a GRAPH collection.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Relationship deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Collection, element, or relationship not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a GRAPH type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{collectionId}/relationships")
  public ResponseEntity<Void> deleteRelationship(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Parameter(
              description = "Source element UUID",
              example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
          @RequestParam(name = "source")
          UUID sourceElementId,
      @Parameter(
              description = "Target element UUID",
              example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
          @RequestParam(name = "target")
          UUID targetElementId,
      @Parameter(description = "Relationship type", example = "depends-on")
          @RequestParam(name = "type")
          String relationshipType) {

    relationshipService.deleteRelationship(
        collectionId, sourceElementId, targetElementId, relationshipType);

    return ResponseEntity.noContent().build();
  }

  /**
   * Pushes an element onto a STACK collection.
   *
   * <p>POST /api/v1/collections/{collectionId}/stack/push
   *
   * <p>LIFO (Last-In-First-Out) operation. Adds the element to the top of the stack.
   *
   * @param collectionId the UUID of the STACK collection
   * @param requestDto validated create element request DTO
   * @return 201 Created with element response DTO
   */
  @Tag(name = "Collections", description = "APIs for managing collections")
  @Operation(
      summary = "Push element onto STACK collection",
      description =
          "Creates a new element and pushes it onto the top of a STACK collection. LIFO operation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Element pushed successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or validation error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a STACK type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/stack/push")
  public ResponseEntity<ElementResponseDto> pushElement(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId,
      @Valid @RequestBody CreateElementRequestDto requestDto) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    Element element = elementMapper.toEntity(requestDto);

    // Resolve element type from name
    Optional<ElementType> elementTypeOpt =
        elementTypeRepository.findByName(requestDto.getElementTypeName());
    if (elementTypeOpt.isEmpty()) {
      throw new IllegalArgumentException(
          "Element type '" + requestDto.getElementTypeName() + "' does not exist");
    }
    element.setType(elementTypeOpt.get());
    element.setCreatedBy(SecurityUtils.getCurrentUsername());
    element.setModifiedBy(SecurityUtils.getCurrentUsername());

    // Create element
    Element savedElement = elementService.create(element);

    // Push onto stack
    membershipService.push(savedElement, collection, SecurityUtils.getCurrentUsername());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(elementMapper.toResponseDto(savedElement));
  }

  /**
   * Pops the top element from a STACK collection.
   *
   * <p>POST /api/v1/collections/{collectionId}/stack/pop
   *
   * <p>LIFO (Last-In-First-Out) operation. Removes and returns the top element.
   *
   * @param collectionId the UUID of the STACK collection
   * @return 200 OK with popped element, or 404 if stack is empty
   */
  @Tag(name = "Collections", description = "APIs for managing collections")
  @Operation(
      summary = "Pop element from STACK collection",
      description =
          "Removes and returns the top element from a STACK collection. LIFO operation. Returns 404 if stack is empty.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Element popped successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found or stack is empty",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a STACK type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PostMapping("/{collectionId}/stack/pop")
  public ResponseEntity<ElementResponseDto> popElement(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Pop from stack
    Element poppedElement = membershipService.pop(collection);
    if (poppedElement == null) {
      throw new IllegalArgumentException("Stack is empty");
    }

    return ResponseEntity.ok(elementMapper.toResponseDto(poppedElement));
  }

  /**
   * Peeks at the top element of a STACK collection without removing it.
   *
   * <p>GET /api/v1/collections/{collectionId}/stack/peek
   *
   * <p>Returns the top element without modifying the stack.
   *
   * @param collectionId the UUID of the STACK collection
   * @return 200 OK with top element, or 404 if stack is empty
   */
  @Tag(name = "Collections", description = "APIs for managing collections")
  @Operation(
      summary = "Peek at top element of STACK collection",
      description =
          "Returns the top element of a STACK collection without removing it. Returns 404 if stack is empty.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Top element retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Collection not found or stack is empty",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "422",
            description = "Collection is not a STACK type",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{collectionId}/stack/peek")
  public ResponseEntity<ElementResponseDto> peekElement(
      @Parameter(description = "Collection UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID collectionId) {

    // Verify collection exists
    Collection collection =
        collectionService
            .findByCollectionId(collectionId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Collection with UUID " + collectionId + " does not exist"));

    // Peek at stack
    Element topElement = membershipService.peek(collection);
    if (topElement == null) {
      throw new IllegalArgumentException("Stack is empty");
    }

    return ResponseEntity.ok(elementMapper.toResponseDto(topElement));
  }
}
