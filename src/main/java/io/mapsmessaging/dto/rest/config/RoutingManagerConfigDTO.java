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

package io.mapsmessaging.dto.rest.config;

import io.mapsmessaging.dto.rest.config.routing.PredefinedServerConfigDTO;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Routing Manager Configuration DTO")
public class RoutingManagerConfigDTO extends BaseManagerConfigDTO {

  @Schema(
      description = "Enables routing management",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected boolean enabled = false;

  @Schema(
      description = "Enables auto-discovery of servers",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected boolean autoDiscovery = true;

  @ArraySchema(
      schema = @Schema(implementation = PredefinedServerConfigDTO.class),
      minItems = 0
  )
  @Schema(
      description = "List of predefined server configurations",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false,
      example  = "[]"
  )
  protected List<PredefinedServerConfigDTO> predefinedServers = List.of();

  public RoutingManagerConfigDTO(){
    super("RoutingManagerConfigDTO");
  }

  @Override
  public String getSimpleName() {
    return "Routing";
  }
}
