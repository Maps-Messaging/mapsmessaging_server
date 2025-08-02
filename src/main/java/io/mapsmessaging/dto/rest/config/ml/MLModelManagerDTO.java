/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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


import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MLModelManagerDTO extends BaseConfigDTO {

  @Schema(description = "Enable in-memory caching of models")
  protected boolean enableCaching;

  @Schema(description = "Maximum number of models to cache")
  protected int cacheSize;

  @Schema(description = "Model cache expiry time in minutes")
  protected int cacheExpiryMinutes;

  @Schema(description = "Models to preload at startup")
  protected List<String> preloadModels;

  @Schema(description = "Auto-refresh configuration")
  protected AutoRefreshConfig autoRefresh;

  @Schema(description = "Model store configuration")
  protected ModelStoreConfig modelStore;

  @Schema(description = "List of configured model event streams")
  protected List<MLEventStreamDTO> eventStreams;
}
