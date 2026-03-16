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
import io.mapsmessaging.dto.rest.config.transformer.TransformationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Transformation DTO that extracts a specific value from a JSON payload.")
public class JsonToValueTransformationDTO extends TransformationConfigDTO {

  public JsonToValueTransformationDTO() {
    super(TransformationType.JSON_TO_VALUE);
  }

  @Schema(
      description = "Json path key used by JsonParserExtension to locate a value. " +
          "If null, the transformer becomes a no-op and returns the original payload.",
      example = "data.temperature",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      maxLength = 2048
  )
  protected String key;
}
