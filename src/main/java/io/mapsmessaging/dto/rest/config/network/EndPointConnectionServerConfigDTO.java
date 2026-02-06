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

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
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

  @Schema(
      description = "Authentication configuration for the endpoint connection",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected AuthConfigDTO authConfig;

  @Schema(
      description =
          "Link transformation identifier. Must match the name of a registered "
              + "ProtocolMessageTransformation discovered via ServiceLoader.",
      example = "Schema-To-Json",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected String linkTransformation;

  @Schema(
      description = "List of link configurations for this endpoint connection",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "[{\"direction\":\"pull\",\"remote_namespace\":\"/+/1/1/GPS_RAW_INT\",\"local_namespace\":\"/\"}]"
  )
  protected List<LinkConfigDTO> linkConfigs;

  @Schema(
      description = "True if this is a third-party plugin connection",
      example = "false",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean pluginConnection = false;

  @Schema(
      description = "An arbitrary cost associated with using this connection (lower is preferred)",
      example = "10",
      defaultValue = "10",
      minimum = "0",
      maximum = "1000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int cost = 10;

  @Schema(
      description = "Optional group name for this connection",
      example = "Main data uplink",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String groupName;
}

