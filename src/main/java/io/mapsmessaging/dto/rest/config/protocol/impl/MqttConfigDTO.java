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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "MQTT Protocol Configuration DTO")
public class MqttConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Maximum session expiry for MQTT", example = "86400")
  protected long maximumSessionExpiry = 86400;

  @Schema(description = "Maximum buffer size for MQTT", example = "10485760") // Example for 10M
  protected long maximumBufferSize = 10485760;

  @Schema(description = "Server receive maximum", example = "10")
  protected int serverReceiveMaximum = 10;

  @Schema(description = "Client receive maximum", example = "65535")
  protected int clientReceiveMaximum = 65535;

  @Schema(description = "Client maximum topic alias", example = "32767")
  protected int clientMaximumTopicAlias = 32767;

  @Schema(description = "Server maximum topic alias", example = "0")
  protected int serverMaximumTopicAlias = 0;

  @Schema(description = "Indicates if strict client ID enforcement is enabled", example = "false")
  protected boolean strictClientId = false;
}
