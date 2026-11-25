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
package net.ottleys.xpository.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for assigning a trait to an element.
 *
 * <p>Validates that trait ID and value are provided.
 */
@Schema(description = "Request to assign a trait to an element")
public class AssignTraitRequestDto {

  @NotNull(message = "Trait ID is required")
  @Schema(
      description = "UUID of the trait to assign",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private UUID traitId;

  @NotBlank(message = "Trait value is required")
  @Schema(description = "Value for the trait", example = "Confidential")
  private String value;

  /** Default constructor. */
  public AssignTraitRequestDto() {}

  /**
   * Returns the trait ID.
   *
   * @return the trait ID
   */
  public UUID getTraitId() {
    return traitId;
  }

  /**
   * Sets the trait ID.
   *
   * @param traitId the trait ID
   */
  public void setTraitId(UUID traitId) {
    this.traitId = traitId;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }
}
