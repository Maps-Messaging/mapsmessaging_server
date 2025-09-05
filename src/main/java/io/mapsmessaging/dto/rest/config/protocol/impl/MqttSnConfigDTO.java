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

import io.mapsmessaging.config.protocol.PredefinedTopics;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "MQTT-SN Protocol Configuration DTO")
public class MqttSnConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Gateway ID for MQTT-SN", example = "1")
  protected String gatewayId = "1";

  @Schema(description = "Receive maximum", example = "10")
  protected int receiveMaximum = 10;

  @Schema(description = "Idle session timeout in seconds", example = "600")
  protected long idleSessionTimeout = 600;

  @Schema(description = "Maximum session expiry time in seconds", example = "86400")
  protected int maximumSessionExpiry = 86400;

  @Schema(description = "Enable port changes", example = "true")
  protected boolean enablePortChanges = true;

  @Schema(description = "Enable address changes", example = "false")
  protected boolean enableAddressChanges = false;

  @Schema(description = "Advertise the gateway", example = "false")
  protected boolean advertiseGateway = false;

  @Schema(description = "Registered topics", example = "")
  protected String registeredTopics = "";

  @Schema(description = "Advertise interval in seconds", example = "30")
  protected int advertiseInterval = 30;

  @Schema(description = "Maximum registered size", example = "32767")
  protected int maxRegisteredSize = 32767;

  @Schema(description = "Maximum in-flight events", example = "1")
  protected int maxInFlightEvents = 1;

  @Schema(description = "Drop QoS 0 events", example = "false")
  protected boolean dropQoS0 = false;

  @Schema(description = "Event queue timeout in seconds", example = "0")
  protected int eventQueueTimeout = 0;

  @Schema(description = "List of predefined topics")
  protected List<PredefinedTopics> predefinedTopicsList;
}
