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
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.dto.rest.config.ml;


import io.mapsmessaging.dto.rest.config.BaseManagerConfigDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "Machine Learning Model Manager configuration")
public class MLModelManagerDTO extends BaseManagerConfigDTO {

  @Schema(
      description = "Enable in-memory caching of models",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected boolean enableCaching = false;

  @Schema(
      description = "Maximum number of models to cache",
      defaultValue = "10000",
      example = "10000",
      minimum = "1",
      maximum = "1000000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected int cacheSize = 10000;

  @Schema(
      description = "Model cache expiry time in minutes",
      defaultValue = "2",
      minimum = "1",
      maximum = "60",
      example = "2",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected int cacheExpiryMinutes = 2;

  @Schema(
      description = "Models to preload at startup",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false,
      defaultValue = "[]"
  )
  protected List<String> preloadModels = List.of();

  @Schema(
      description = "Auto-refresh configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected AutoRefreshConfigDTO autoRefresh;

  @Schema(
      description = "LLM access configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected LlmConfigDTO llmConfig;

  @Schema(
      description = "Model store configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected ModelStoreConfigDTO modelStore;

  @ArraySchema(
      schema = @Schema(implementation = MLEventStreamDTO.class),
      minItems = 0
  )
  @Schema(
      description = "List of configured model event streams",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false,
      example  = "[]"
  )
  protected List<MLEventStreamDTO> eventStreams = List.of();

  public MLModelManagerDTO(){
    super("MLModelManagerConfigDTO");
  }

  @Override
  public String getSimpleName() {
    return "ML Models";
  }
}

