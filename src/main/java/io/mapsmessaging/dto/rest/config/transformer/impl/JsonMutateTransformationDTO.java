/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.dto.rest.config.transformer.impl;

import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutateOpDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

import static io.mapsmessaging.dto.rest.config.transformer.TransformationType.JSON_MUTATE;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    title = "JSON Mutate Transformation DTO",
    description = "Applies a small set of JSON mutations: set/remove/rename using dot paths."
)
public class JsonMutateTransformationDTO extends TransformationConfigDTO {

  public JsonMutateTransformationDTO() {
    super(JSON_MUTATE);
  }

  @ArraySchema(
      minItems = 1,
      arraySchema = @Schema(
          description = "Ordered list of mutation operations",
          requiredMode = Schema.RequiredMode.REQUIRED,
          nullable = false
      ),
      schema = @Schema(implementation = JsonMutateOpDTO.class)
  )
  protected List<JsonMutateOpDTO> operations;
}
