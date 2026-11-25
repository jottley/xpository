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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.ottleys.xpository.domain.Collection;
import net.ottleys.xpository.domain.Element;
import net.ottleys.xpository.domain.ElementType;
import net.ottleys.xpository.dto.CollectionResponseDto;
import net.ottleys.xpository.dto.ElementResponseDto;
import net.ottleys.xpository.dto.ErrorResponse;
import net.ottleys.xpository.dto.UpdateElementRequestDto;
import net.ottleys.xpository.mapper.CollectionMapper;
import net.ottleys.xpository.mapper.ElementMapper;
import net.ottleys.xpository.repository.ElementCollectionMembershipRepository;
import net.ottleys.xpository.repository.ElementTypeRepository;
import net.ottleys.xpository.security.SecurityUtils;
import net.ottleys.xpository.service.ElementService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for Element resources.
 *
 * <p>Provides CRUD operations for elements following REST conventions. All endpoints are under
 * /api/v1/elements. This controller is thin - all business logic is delegated to the service layer.
 * Uses DTOs for request/response to separate API contract from domain model.
 *
 * <p>Authentication and authorization will be added in a future release. Currently uses "system" as
 * the default user for created/modified by fields.
 */
@RestController
@RequestMapping("/api/v1/elements")
@Tag(name = "Elements", description = "APIs for managing metadata elements")
public class ElementController {

  private final ElementService elementService;
  private final ElementMapper elementMapper;
  private final ElementTypeRepository elementTypeRepository;
  private final ElementCollectionMembershipRepository membershipRepository;
  private final CollectionMapper collectionMapper;
  private final net.ottleys.xpository.service.ElementVersionService versionService;
  private final net.ottleys.xpository.mapper.ElementVersionMapper versionMapper;

  /**
   * Constructs an ElementController with required dependencies.
   *
   * @param elementService service for element business logic
   * @param elementMapper mapper for entity/DTO conversion
   * @param elementTypeRepository repository for element type resolution
   * @param membershipRepository repository for element-collection membership queries
   * @param collectionMapper mapper for collection entity/DTO conversion
   * @param versionService service for element version management
   * @param versionMapper mapper for element version entity/DTO conversion
   */
  public ElementController(
      ElementService elementService,
      ElementMapper elementMapper,
      ElementTypeRepository elementTypeRepository,
      ElementCollectionMembershipRepository membershipRepository,
      CollectionMapper collectionMapper,
      net.ottleys.xpository.service.ElementVersionService versionService,
      net.ottleys.xpository.mapper.ElementVersionMapper versionMapper) {
    this.elementService = elementService;
    this.elementMapper = elementMapper;
    this.elementTypeRepository = elementTypeRepository;
    this.membershipRepository = membershipRepository;
    this.collectionMapper = collectionMapper;
    this.versionService = versionService;
    this.versionMapper = versionMapper;
  }

  /**
   * Lists all elements with optional filtering and pagination.
   *
   * <p>GET
   * /api/v1/elements?page=0&size=20&sort=createdDate,desc&elementTypeName=xpository:core:standard
   *
   * <p>Supports multiple query parameters for filtering: - elementTypeName: Filter by element type
   * name (namespace format) - createdBy: Filter by creator username - modifiedBy: Filter by
   * modifier username
   *
   * <p>All filters are optional and combine with AND logic.
   *
   * @param elementTypeName filter by element type name
   * @param createdBy filter by creator username
   * @param modifiedBy filter by modifier username
   * @param pageable pagination parameters (defaults: page=0, size=20)
   * @return page of element response DTOs matching the filters
   */
  @Operation(
      summary = "List and search elements",
      description =
          "Retrieves a paginated, filterable list of elements. Supports filtering by element type, creator, and modifier. All filters are optional and combine with AND logic.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved elements",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class)))
      })
  @GetMapping
  public ResponseEntity<Page<ElementResponseDto>> listElements(
      @Parameter(
              description = "Filter by element type name (namespace format)",
              example = "xpository:core:standard")
          @RequestParam(required = false)
          String elementTypeName,
      @Parameter(description = "Filter by creator username", example = "admin")
          @RequestParam(required = false)
          String createdBy,
      @Parameter(description = "Filter by modifier username", example = "admin")
          @RequestParam(required = false)
          String modifiedBy,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

    // Check if any filters are provided
    boolean hasFilters =
        (elementTypeName != null && !elementTypeName.isBlank())
            || (createdBy != null && !createdBy.isBlank())
            || (modifiedBy != null && !modifiedBy.isBlank());

    Page<Element> elements;
    if (hasFilters) {
      // Use search method with filters
      elements = elementService.searchElements(elementTypeName, createdBy, modifiedBy, pageable);
    } else {
      // No filters - use optimized findAll query
      elements = elementService.findAll(pageable);
    }

    Page<ElementResponseDto> responseDtos = elements.map(elementMapper::toResponseDto);
    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Retrieves a single element by UUID.
   *
   * <p>GET /api/v1/elements/{elementId}
   *
   * @param elementId the public UUID
   * @return 200 OK with element response DTO if found, 404 Not Found otherwise
   */
  @Operation(summary = "Get element by ID", description = "Retrieves a single element by its UUID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Element found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{elementId}")
  public ResponseEntity<ElementResponseDto> getElement(
      @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {
    return elementService
        .findByElementId(elementId)
        .map(elementMapper::toResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Partially updates an existing element (PATCH).
   *
   * <p>PATCH /api/v1/elements/{elementId}
   *
   * <p>Only fields provided in the request are updated. Null fields are ignored. This allows
   * clients to update specific fields without needing to send the entire resource. Version field is
   * required for optimistic locking.
   *
   * @param elementId the public UUID of element to update
   * @param requestDto update request DTO with optional fields
   * @return 200 OK with updated element, 404 Not Found if element doesn't exist
   */
  @Operation(
      summary = "Partially update an element",
      description =
          "Updates only the fields provided in the request. Null/missing fields are ignored and retain their current values. Version is required for optimistic locking.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Element updated successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ElementResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Element not found",
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
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - version mismatch",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @PatchMapping("/{elementId}")
  public ResponseEntity<ElementResponseDto> updateElement(
      @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Fields to update (only non-null fields are updated, version required)",
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = UpdateElementRequestDto.class),
                      examples = {
                        @ExampleObject(
                            name = "Update properties only",
                            value =
                                """
                                {
                                  "version": 0,
                                  "propertiesJson": "{\\"title\\": \\"Updated Document\\", \\"status\\": \\"published\\"}"
                                }
                                """),
                        @ExampleObject(
                            name = "Update element type and properties",
                            value =
                                """
                                {
                                  "version": 0,
                                  "elementTypeName": "xpository:document:generic",
                                  "propertiesJson": "{\\"title\\": \\"My Document\\", \\"author\\": \\"Jane Smith\\", \\"publishDate\\": \\"2026-06-05\\"}"
                                }
                                """),
                        @ExampleObject(
                            name = "Major version update (breaking change)",
                            value =
                                """
                                {
                                  "version": 0,
                                  "propertiesJson": "{\\"title\\": \\"New Major Revision\\"}",
                                  "majorVersion": true
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          UpdateElementRequestDto requestDto) {
    return elementService
        .findByElementId(elementId)
        .map(existing -> {
          // Update only non-null fields from request DTO
          elementMapper.updateEntity(existing, requestDto);

          // Resolve element type from name if provided
          if (requestDto.getElementTypeName() != null) {
            Optional<ElementType> elementTypeOpt =
                elementTypeRepository.findByName(requestDto.getElementTypeName());
            if (elementTypeOpt.isEmpty()) {
              throw new IllegalArgumentException(
                  "Element type with name " + requestDto.getElementTypeName() + " does not exist");
            }
            existing.setType(elementTypeOpt.get());
          }

          existing.setModifiedBy(SecurityUtils.getCurrentUsername());

          boolean isMajor = requestDto.isMajorVersion();
          Element savedElement = elementService.update(existing, isMajor);
          return ResponseEntity.ok(elementMapper.toResponseDto(savedElement));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Deletes an element by UUID.
   *
   * <p>DELETE /api/v1/elements/{elementId}
   *
   * <p>Performs a hard delete - permanently removes the element from the database. Unlike
   * collections, elements do not support soft delete.
   *
   * @param elementId the public UUID of element to delete
   * @return 204 No Content if deleted successfully, 404 Not Found if element doesn't exist
   */
  @Operation(
      summary = "Delete an element (soft delete)",
      description =
          "Soft deletes an element by marking it as deleted. The element remains in the database"
              + " for audit trails but is excluded from normal queries.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Element deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Element not found or already deleted",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @DeleteMapping("/{elementId}")
  public ResponseEntity<Void> deleteElement(
      @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId) {
    // Get current username from security context
    String username = SecurityUtils.getCurrentUsername();
    boolean deleted = elementService.deleteByElementId(elementId, username);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /**
   * Lists all collections containing an element.
   *
   * <p>GET /api/v1/elements/{elementId}/collections
   *
   * <p>Returns all collections that this element is a member of. This is useful for understanding
   * where an element is used and for navigation purposes.
   *
   * @param elementId the UUID of the element
   * @param pageable pagination parameters
   * @return page of collection response DTOs
   */
  @Operation(
      summary = "List collections containing an element",
      description =
          "Retrieves all collections that the specified element belongs to. Elements can belong to multiple collections simultaneously.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved collections",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Page.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{elementId}/collections")
  public ResponseEntity<Page<CollectionResponseDto>> listCollectionsForElement(
      @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

    // Verify element exists
    Optional<Element> elementOpt = elementService.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Find all collections containing this element
    List<Collection> collections = membershipRepository.findCollectionsByElement(elementOpt.get());

    // Convert to response DTOs
    List<CollectionResponseDto> responseDtos =
        collections.stream().map(collectionMapper::toResponseDto).toList();

    // Create a Page manually (TODO: implement proper pagination in repository)
    Page<CollectionResponseDto> page = new PageImpl<>(responseDtos, pageable, responseDtos.size());

    return ResponseEntity.ok(page);
  }

  /**
   * Gets version history for an element.
   *
   * <p>GET /api/v1/elements/{elementId}/versions
   *
   * <p>Returns all historical versions of the element, ordered by version number (newest first).
   * Each version snapshot captures the element's state at a point in time.
   *
   * @param elementId the element UUID
   * @return list of version snapshots
   */
  @Operation(
      summary = "Get element version history",
      description =
          "Retrieves all historical versions of an element, ordered by version number descending (newest first).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Version history retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation =
                                net.ottleys.xpository.dto.ElementVersionResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Element not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{elementId}/versions")
  public ResponseEntity<List<net.ottleys.xpository.dto.ElementVersionResponseDto>>
      getVersionHistory(
          @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
              @PathVariable
              UUID elementId) {

    // Verify element exists
    Optional<Element> elementOpt = elementService.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Get version history
    List<net.ottleys.xpository.domain.ElementVersion> versions =
        versionService.getVersionHistory(elementOpt.get());

    // Convert to response DTOs
    List<net.ottleys.xpository.dto.ElementVersionResponseDto> responseDtos =
        versions.stream().map(versionMapper::toResponseDto).toList();

    return ResponseEntity.ok(responseDtos);
  }

  /**
   * Gets a specific version of an element.
   *
   * <p>GET /api/v1/elements/{elementId}/versions/{majorVersion}.{minorVersion}
   *
   * <p>Returns a specific historical version snapshot by version number.
   *
   * @param elementId the element UUID
   * @param majorVersion the major version number
   * @param minorVersion the minor version number
   * @return the version snapshot
   */
  @Operation(
      summary = "Get specific element version",
      description = "Retrieves a specific historical version of an element by version number.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Version retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema =
                        @Schema(
                            implementation =
                                net.ottleys.xpository.dto.ElementVersionResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Element or version not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  @GetMapping("/{elementId}/versions/{majorVersion}.{minorVersion}")
  public ResponseEntity<net.ottleys.xpository.dto.ElementVersionResponseDto> getVersion(
      @Parameter(description = "Element UUID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID elementId,
      @Parameter(description = "Major version number", example = "1") @PathVariable
          Integer majorVersion,
      @Parameter(description = "Minor version number", example = "0") @PathVariable
          Integer minorVersion) {

    // Verify element exists
    Optional<Element> elementOpt = elementService.findByElementId(elementId);
    if (elementOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Get specific version
    Optional<net.ottleys.xpository.domain.ElementVersion> versionOpt =
        versionService.getVersion(elementOpt.get(), majorVersion, minorVersion);

    if (versionOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(versionMapper.toResponseDto(versionOpt.get()));
  }
}
