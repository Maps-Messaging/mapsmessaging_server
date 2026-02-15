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

package io.mapsmessaging.dto.rest.config.transformer.jsonmutate;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
    title = "JSON Mutate Operation DTO",
    description = "Single JSON mutation operation."
)
public class JsonMutateOpDTO {

  @Schema(
      description = "Operation type",
      allowableValues = {"set", "remove", "rename"},
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected JsonMutateOperation op;

  @Schema(
      description = "Target path for set/remove. Dot path with optional array indexes, e.g. payload.temp or payload.items[0].name",
      example = "payload.temperature",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String path;

  @Schema(
      description = "Source path for rename",
      example = "payload.tempC",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String from;

  @Schema(
      description = "Destination path for rename",
      example = "payload.temperatureC",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String to;

  @Schema(
      description = "Value for set. Stored as JSON element so it can be number/string/object/array.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected JsonElement value;
}
