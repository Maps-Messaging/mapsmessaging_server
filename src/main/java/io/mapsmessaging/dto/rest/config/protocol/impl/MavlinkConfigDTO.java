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

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Mavlink Protocol Configuration DTO")
public class MavlinkConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "Idle session timeout in seconds", example = "600")
  protected long idleSessionTimeout = 600;

  @Schema(description = "Maximum session expiry time in seconds", example = "86400")
  protected int maximumSessionExpiry = 86400;

  @Schema(description = "Advertise interval in seconds", example = "30")
  protected int advertiseInterval = 30;

  @Schema(description = "Maximum in-flight events", example = "1")
  protected int maxInFlightEvents = 1;

  @Schema(description = "Maximum in-flight events", example = "1")
  protected String topicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}";

  @Schema(description = "Flag to convert incomig mavlink to json", example = "false")
  protected boolean parseToJson = true;

  @Schema(
      description = "Comma-separated list of MAVLink-compatible UDP endpoints to forward received frames to. " +
          "Empty or blank disables forwarding.",
      example = "udp://192.168.1.50:14550/,udp://192.168.1.51:14550/"
  )
  protected String forwardUrls = "";

  @Schema(description = "Forward raw MAVLink frames to forwardUrls when forwarding is enabled", example = "true")
  protected boolean forwardRawFrames = true;

  @Schema(description = "Do not forward a packet back to its source address/port if that address appears in forwardUrls",
      example = "true")
  protected boolean dropIfTargetEqualsSource = true;

  @Schema(description = "Optional duplicate suppression window in milliseconds (0 disables). " +
      "Useful to reduce forwarding loops in multi-router networks.",
      example = "0")
  protected int dedupWindowMillis = 0;
}
