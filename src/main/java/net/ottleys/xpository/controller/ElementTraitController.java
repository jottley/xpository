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
import net.ottleys.xpository.domain.ElementTrait;
import net.ottleys.xpository.dto.AssignTraitRequestDto;
import net.ottleys.xpository.dto.ElementTraitResponseDto;
import net.ottleys.xpository.dto.UpdateTraitValueRequestDto;
import net.ottleys.xpository.mapper.ElementTraitMapper;
import net.ottleys.xpository.service.ElementTraitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for element-trait operations.
 *
 * <p>Provides endpoints for assigning traits to elements, updating trait values, and removing
 * traits from elements.
 */
@RestController
@RequestMapping("/api/v1/elements/{elementId}/traits")
@Tag(name = "Element Traits", description = "Element-trait association management API")
public class ElementTraitController {

  private final ElementTraitService elementTraitService;
  private final ElementTraitMapper elementTraitMapper;

  /**
   * Constructs an ElementTraitController with required dependencies.
   *
   * @param elementTraitService service for element-trait business logic
   * @param elementTraitMapper mapper for entity-DTO conversion
   */
  public ElementTraitController(
      ElementTraitService elementTraitService, ElementTraitMapper elementTraitMapper) {
    this.elementTraitService = elementTraitService;
    this.elementTraitMapper = elementTraitMapper;
  }

  /**
   * Lists all traits for an element.
   *
   * @param elementId the element UUID
   * @return list of element-trait associations
   */
  @GetMapping
  @Operation(
      summary = "List element traits",
      description = "Retrieves all traits assigned to an element")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved traits"),
  })
  public ResponseEntity<List<ElementTraitResponseDto>> getElementTraits(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId) {
    List<ElementTrait> elementTraits = elementTraitService.findByElementId(elementId);
    List<ElementTraitResponseDto> dtos =
        elementTraits.stream().map(elementTraitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Retrieves a specific trait for an element.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @return element-trait association
   */
  @GetMapping("/{traitId}")
  @Operation(
      summary = "Get element trait",
      description = "Retrieves a specific trait assigned to an element")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Trait found"),
    @ApiResponse(responseCode = "404", description = "Trait not assigned to this element")
  })
  public ResponseEntity<ElementTraitResponseDto> getElementTrait(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId,
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId) {
    return elementTraitService
        .findByElementIdAndTraitId(elementId, traitId)
        .map(elementTraitMapper::toResponseDto)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Lists inherited traits for an element.
   *
   * @param elementId the element UUID
   * @return list of inherited traits
   */
  @GetMapping("/inherited")
  @Operation(
      summary = "List inherited traits",
      description = "Retrieves all traits inherited by an element")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved inherited traits"),
  })
  public ResponseEntity<List<ElementTraitResponseDto>> getInheritedTraits(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId) {
    List<ElementTrait> elementTraits = elementTraitService.findInheritedByElementId(elementId);
    List<ElementTraitResponseDto> dtos =
        elementTraits.stream().map(elementTraitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Lists directly assigned traits for an element.
   *
   * @param elementId the element UUID
   * @return list of directly assigned traits
   */
  @GetMapping("/direct")
  @Operation(
      summary = "List direct traits",
      description = "Retrieves all traits directly assigned to an element (non-inherited)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Successfully retrieved direct traits"),
  })
  public ResponseEntity<List<ElementTraitResponseDto>> getDirectTraits(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId) {
    List<ElementTrait> elementTraits = elementTraitService.findDirectByElementId(elementId);
    List<ElementTraitResponseDto> dtos =
        elementTraits.stream().map(elementTraitMapper::toResponseDto).toList();
    return ResponseEntity.ok(dtos);
  }

  /**
   * Assigns a trait to an element.
   *
   * @param elementId the element UUID
   * @param dto the assign request
   * @return the created element-trait association
   */
  @PostMapping
  @Operation(summary = "Assign trait", description = "Assigns a trait to an element with a value")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Trait assigned successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request data or trait already assigned"),
    @ApiResponse(responseCode = "404", description = "Element or trait not found")
  })
  public ResponseEntity<ElementTraitResponseDto> assignTrait(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId,
      @Valid @RequestBody AssignTraitRequestDto dto) {
    ElementTrait elementTrait =
        elementTraitService.assignTrait(elementId, dto.getTraitId(), dto.getValue());
    ElementTraitResponseDto responseDto = elementTraitMapper.toResponseDto(elementTrait);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  /**
   * Updates a trait value for an element.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @param dto the update request
   * @return the updated element-trait association
   */
  @PutMapping("/{traitId}")
  @Operation(
      summary = "Update trait value",
      description = "Updates the value of a trait assigned to an element")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Trait value updated successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid request data or trait is inherited"),
    @ApiResponse(responseCode = "404", description = "Trait not assigned to this element")
  })
  public ResponseEntity<ElementTraitResponseDto> updateTraitValue(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId,
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId,
      @Valid @RequestBody UpdateTraitValueRequestDto dto) {
    ElementTrait elementTrait =
        elementTraitService.updateTraitValue(elementId, traitId, dto.getValue());
    ElementTraitResponseDto responseDto = elementTraitMapper.toResponseDto(elementTrait);
    return ResponseEntity.ok(responseDto);
  }

  /**
   * Removes a trait from an element.
   *
   * @param elementId the element UUID
   * @param traitId the trait UUID
   * @return no content if successful
   */
  @DeleteMapping("/{traitId}")
  @Operation(summary = "Remove trait", description = "Removes a trait from an element")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Trait removed successfully"),
    @ApiResponse(responseCode = "400", description = "Trait is inherited and cannot be removed"),
    @ApiResponse(responseCode = "404", description = "Trait not assigned to this element")
  })
  public ResponseEntity<Void> removeTrait(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId,
      @Parameter(description = "Trait UUID") @PathVariable UUID traitId) {
    boolean removed = elementTraitService.removeTrait(elementId, traitId);
    return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  /**
   * Removes all traits from an element.
   *
   * @param elementId the element UUID
   * @return no content
   */
  @DeleteMapping
  @Operation(summary = "Remove all traits", description = "Removes all traits from an element")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "All traits removed successfully"),
  })
  public ResponseEntity<Void> removeAllTraits(
      @Parameter(description = "Element UUID") @PathVariable UUID elementId) {
    elementTraitService.removeAllTraits(elementId);
    return ResponseEntity.noContent().build();
  }
}
