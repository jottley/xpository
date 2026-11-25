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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import net.ottleys.xpository.domain.Trait;
import net.ottleys.xpository.dto.CreateTraitRequestDto;
import net.ottleys.xpository.dto.TraitResponseDto;
import net.ottleys.xpository.dto.UpdateTraitRequestDto;
import net.ottleys.xpository.mapper.TraitMapper;
import net.ottleys.xpository.service.TraitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for trait operations.
 *
 * <p>Provides CRUD endpoints for managing trait definitions. All endpoints require ADMIN role
 * except for read operations.
 */
@RestController
@RequestMapping("/api/v1/traits")
@Tag(name = "Traits", description = "Trait definition management API")
public class TraitController {

  private final TraitService traitService;
  private final TraitMapper traitMapper;

  /**
   * Constructs a TraitController with required dependencies.
   *
   * @param traitService service for trait business logic
   * @param traitMapper mapper for entity-DTO conversion
   */
  public TraitController(TraitService traitService, TraitMapper traitMapper) {
    this.traitService = traitService;
    this.traitMapper = traitMapper;
  }

  /**
   * Lists all traits with pagination.
   *
   * @param pageable pagination parameters
   * @return page of trait response DTOs
   */
  @GetMapping
  @Operation(
      summary = "List all traits",
      description = "Retrieves all trait definitions with pagination")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved traits"),
  })
  public ResponseEntity<Page<TraitResponseDto>> getAllTraits(
      @PageableDefault(size = 20) Pageable pageable) {
    Page<Trait> traits = traitService.findAll(pageable);
    Page<TraitResponseDto> dtos = traits.map(traitMapper::toResponseDto);
    return ResponseEntity.ok(dtos);
  }

  /**
   * Retrieves a trait by UUID.
   *
   * @param traitId the trait UUID
   * @return trait response DTO
   */
  @GetMapping("/{traitId}")
  @Operation(
      summary = "Get trait by ID",
      description = "Retrieves a specific trait definition by UUID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Trait found"),
    @ApiResponse(responseCode = "404", description = "Trait not found")
  })
  public ResponseEntity<TraitResponseDto> getTraitById(
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId) {
    return traitService
        .findByTraitId(traitId)
        .map(traitMapper::toResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Searches for traits by name.
   *
   * @param name the search term
   * @return list of matching traits
   */
  @GetMapping("/search")
  @Operation(
      summary = "Search traits by name",
      description = "Finds traits with names containing the search term (case-insensitive)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved matching traits"),
  })
  public ResponseEntity<List<TraitResponseDto>> searchTraits(
      @Parameter(description = "Search term") @RequestParam String name) {
    List<Trait> traits = traitService.searchByName(name);
    List<TraitResponseDto> dtos = traits.stream().map(traitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Lists traits by type.
   *
   * @param type the trait type
   * @return list of traits of the given type
   */
  @GetMapping("/type/{type}")
  @Operation(
      summary = "List traits by type",
      description = "Retrieves all traits of a specific type")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved traits"),
  })
  public ResponseEntity<List<TraitResponseDto>> getTraitsByType(
      @Parameter(description = "Trait type") @PathVariable Trait.TraitType type) {
    List<Trait> traits = traitService.findByType(type);
    List<TraitResponseDto> dtos = traits.stream().map(traitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Lists all required traits.
   *
   * @return list of required traits
   */
  @GetMapping("/required")
  @Operation(
      summary = "List required traits",
      description = "Retrieves all traits marked as required")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved required traits"),
  })
  public ResponseEntity<List<TraitResponseDto>> getRequiredTraits() {
    List<Trait> traits = traitService.findRequired();
    List<TraitResponseDto> dtos = traits.stream().map(traitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Lists all inheritable traits.
   *
   * @return list of inheritable traits
   */
  @GetMapping("/inheritable")
  @Operation(
      summary = "List inheritable traits",
      description = "Retrieves all traits marked as inheritable")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved inheritable traits"),
  })
  public ResponseEntity<List<TraitResponseDto>> getInheritableTraits() {
    List<Trait> traits = traitService.findInheritable();
    List<TraitResponseDto> dtos = traits.stream().map(traitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Creates a new trait.
   *
   * @param dto the create request
   * @return the created trait
   */
  @PostMapping
  @Operation(summary = "Create trait", description = "Creates a new trait definition")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Trait created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request data"),
    @ApiResponse(responseCode = "409", description = "Trait with this name already exists")
  })
  public ResponseEntity<TraitResponseDto> createTrait(
      @Valid @RequestBody CreateTraitRequestDto dto) {

    Trait trait = traitMapper.toEntity(dto);
    Trait savedTrait = traitService.create(trait);
    TraitResponseDto responseDto = traitMapper.toResponseDto(savedTrait);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  /**
   * Updates an existing trait.
   *
   * @param traitId the trait UUID
   * @param dto the update request
   * @return the updated trait
   */
  @PutMapping("/{traitId}")
  @Operation(summary = "Update trait", description = "Updates an existing trait definition")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Trait updated successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request data"),
    @ApiResponse(responseCode = "404", description = "Trait not found")
  })
  public ResponseEntity<TraitResponseDto> updateTrait(
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId,
      @Valid @RequestBody UpdateTraitRequestDto dto) {
    return traitService
        .findByTraitId(traitId)
        .map(trait -> {
          traitMapper.applyUpdates(dto, trait);
          Trait updatedTrait = traitService.update(trait);
          TraitResponseDto responseDto = traitMapper.toResponseDto(updatedTrait);
          return ResponseEntity.ok(responseDto);
        })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Deletes a trait by UUID.
   *
   * @param traitId the trait UUID
   * @return no content if successful
   */
  @DeleteMapping("/{traitId}")
  @Operation(
      summary = "Delete trait",
      description = "Deletes a trait definition (fails if trait is in use)")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Trait deleted successfully"),
    @ApiResponse(responseCode = "404", description = "Trait not found"),
    @ApiResponse(responseCode = "409", description = "Trait is in use and cannot be deleted")
  })
  public ResponseEntity<Void> deleteTrait(
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId) {
    boolean deleted = traitService.deleteByTraitId(traitId);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /**
   * Force deletes a trait and all its associations.
   *
   * @param traitId the trait UUID
   * @return no content if successful
   */
  @DeleteMapping("/{traitId}/force")
  @Operation(
      summary = "Force delete trait",
      description = "Deletes a trait and all element associations")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Trait force deleted successfully"),
    @ApiResponse(responseCode = "404", description = "Trait not found")
  })
  public ResponseEntity<Void> forceDeleteTrait(
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId) {
    boolean deleted = traitService.forceDeleteByTraitId(traitId);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
