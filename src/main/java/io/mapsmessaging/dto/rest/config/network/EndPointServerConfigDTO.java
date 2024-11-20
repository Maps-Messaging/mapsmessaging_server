/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.dto.rest.config.network;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(
    title = "EndPoint Server Configuration DTO",
    description = "Represents configuration settings for an endpoint server.")
public class EndPointServerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Name of the endpoint server", example = "MainServer")
  protected String name;

  @Schema(description = "URL for the endpoint server", example = "tcp://localhost:1883")
  protected String url;

  @Schema(
      description = "Endpoint-specific configuration",
      implementation = EndPointConfigDTO.class)
  protected EndPointConfigDTO endPointConfig;

  @Schema(description = "SASL configuration", implementation = SaslConfigDTO.class)
  protected SaslConfigDTO saslConfig;

  @Schema(description = "List of protocol configurations for the endpoint")
  protected List<ProtocolConfigDTO> protocolConfigs;

  @Schema(description = "Protocol types available at the endpoint", example = "mqtt, amqp, stomp")
  protected String protocols;

  @Schema(description = "Authentication realm", example = "defaultRealm")
  protected String authenticationRealm;

  @Schema(description = "Backlog for the endpoint server", example = "100")
  protected int backlog;

  @Schema(description = "Selector task wait time", example = "10")
  protected int selectorTaskWait;

  public ProtocolConfigDTO getProtocolConfig(String protocol) {
    ProtocolConfigDTO config = protocolConfigs.stream()
        .filter(protocolConfig -> protocolConfig.getType().equalsIgnoreCase(protocol))
        .findFirst()
        .orElse(null);
    if(config == null && protocol != null) {
      config = protocolConfigs.stream()
          .filter(protocolConfig -> protocolConfig.getType().contains(protocol))
          .findFirst()
          .orElse(null);
    }
    return config;
  }

}
