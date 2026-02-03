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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "MAVLink protocol configuration. Controls session handling, topic mapping, JSON conversion, and optional frame forwarding."
)
public class MavlinkConfigDTO extends ProtocolConfigDTO {

  public MavlinkConfigDTO() {
    super("mavlink");
  }

  @Schema(
      description =
          "Fully qualified path to the MAVLink dialect XML. "
              + "If not provided, the common dialect is used.",
      example = "C:/path/to/dialects/common.xml",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String fullyQualifiedPathToDialectXml = "";

  @Schema(
      description = "Idle session timeout in seconds. Session is closed if no MAVLink traffic is received within this period.",
      example = "600",
      minimum = "1",
      maximum = "86400",
      defaultValue = "600",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected long idleSessionTimeout = 600;

  @Schema(
      description = "Maximum allowed session lifetime in seconds, regardless of activity.",
      example = "86400",
      minimum = "60",
      maximum = "604800",
      defaultValue = "86400",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int maximumSessionExpiry = 86400;

  @Schema(
      description = "Interval in seconds at which MAVLink heartbeat or advertise messages are emitted.",
      example = "30",
      minimum = "1",
      maximum = "3600",
      defaultValue = "30",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int advertiseInterval = 30;

  @Schema(
      description = "Maximum number of in-flight MAVLink events per session. Limits back-pressure and memory usage.",
      example = "1",
      minimum = "1",
      maximum = "1024",
      defaultValue = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int maxInFlightEvents = 1;

  @Schema(
      description =
          "Topic name template used when publishing decoded MAVLink messages. "
              + "Supported placeholders: {remoteSocket}, {systemId}, {componentId}, {messageName}.",
      example = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      defaultValue = "/{remoteSocket}/{systemId}/{componentId}/{messageName}",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String topicNameTemplate = "/{remoteSocket}/{systemId}/{componentId}/{messageName}";

  @Schema(
      description =
          "Convert incoming MAVLink frames into JSON using the registered MAVLink message definitions. "
              + "If false, raw binary frames are published.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean parseToJson = true;

  @Schema(
      description =
          "Comma-separated list of MAVLink-compatible UDP endpoints to forward received frames to. "
              + "Each entry must be a valid udp://host:port/ URI. Blank disables forwarding.",
      example = "udp://192.168.1.50:14550/,udp://192.168.1.51:14550/",
      defaultValue = "",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected String forwardUrls = "";

  @Schema(
      description =
          "When forwarding is enabled, forward raw MAVLink frames instead of decoded messages.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean forwardRawFrames = true;

  @Schema(
      description =
          "Prevent forwarding a MAVLink packet back to its source address and port "
              + "if that address is present in forwardUrls.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean dropIfTargetEqualsSource = true;

  @Schema(
      description =
          "Duplicate suppression window in milliseconds. "
              + "Packets received with identical content within this window are dropped. "
              + "Set to 0 to disable duplicate detection.",
      example = "0",
      minimum = "0",
      maximum = "60000",
      defaultValue = "0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int dedupWindowMillis = 0;
}
