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

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.config.auth.AuthConfig;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Endpoint Connection Server Configuration DTO")
public class EndPointConnectionServerConfigDTO extends EndPointServerConfigDTO {

  @Schema(description = "Authentication configuration for endpoint connection")
  protected AuthConfig authConfig;

  @Schema(description = "Link transformation for the endpoint connection", example = "transformationType")
  protected String linkTransformation;

  @Schema(description = "List of link configurations")
  protected List<LinkConfigDTO> linkConfigs;

  @Schema(description = "Is this a 3rd party plugin connection")
  protected boolean pluginConnection;

  @Schema(description = "An arbitrary cost associated with using this connection", defaultValue = "10", example = "0")
  protected int cost;

  @Schema(description = "Optional name of the group that the connection belongs to", defaultValue = "", example="Main data uplink")
  protected String groupName;
}
