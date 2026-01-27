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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.auth.SaslConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(
    title = "EndPoint Server Configuration DTO",
    description = "Represents configuration settings for an endpoint server.")
public class EndPointServerConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Name of the endpoint server",
      example = "MainServer",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String name;

  @Schema(
      description = "URL for the endpoint server",
      example = "tcp://localhost:1883",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1,
      maxLength = 2048,
      pattern = "^(tcp|ssl|udp|dtls|ws|wss|serial)://[^\\s]+$"
  )
  protected String url;


  @Schema(
      description = "Endpoint-specific configuration",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      implementation = EndPointConfigDTO.class
  )
  protected EndPointConfigDTO endPointConfig;

  @Schema(
      description = "SASL configuration",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      implementation = SaslConfigDTO.class
  )
  protected SaslConfigDTO saslConfig;

  @Schema(
      description = "List of protocol configurations for the endpoint",
      requiredMode = Schema.RequiredMode.REQUIRED,
      minLength = 1,
      nullable = false
  )
  protected List<ProtocolConfigDTO> protocolConfigs;

  @Schema(
      description = "Authentication realm",
      example = "defaultRealm",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String authenticationRealm;

  @Schema(
      description = "Backlog for the endpoint server",
      example = "100",
      defaultValue = "100",
      minimum = "1",
      maximum = "10000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int backlog = 100;

  @Schema(
      description = "Selector task wait time",
      example = "10",
      minimum = "1",
      maximum = "1000",
      defaultValue = "10",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int selectorTaskWait = 10;

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

  @JsonIgnore
  public String getProtocols() {
    return protocolConfigs.stream()
        .map(ProtocolConfigDTO::getType)
        .collect(Collectors.joining(", "));
  }

}
