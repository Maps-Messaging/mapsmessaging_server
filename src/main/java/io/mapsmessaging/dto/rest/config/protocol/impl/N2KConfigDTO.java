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

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description = "N2K protocol configuration. Controls session handling, topic mapping, JSON conversion, and optional frame forwarding."
)
public class N2KConfigDTO extends ProtocolConfigDTO {

  public N2KConfigDTO() {
    super("n2k");
  }

  @Schema(
      description = "Optional path to an external NMEA 2000 database file. If omitted, the built-in database bundled in the server JAR is used.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      example = "/etc/maps/n2k/n2k-database.xml"
  )
  protected String databasePath;

  
  @Schema(
      description = "Optional XML definition to use encoded as base64",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1
  )
  protected String base64EncodedDatabase;

  @Schema(
      description = "CAN bus source address used by the N2K node when transmitting frames and responding to requests.",
      example = "123",
      defaultValue = "123",
      minimum = "0",
      maximum = "255",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected int canBusAddress = 0x7B;

  @Schema(
      description =
          "Topic name template used when publishing decoded NMEA 2000 (N2K) messages. "
              + "Supported placeholders: {candevice}, {pgn}, {messageName}.",
      example = "/{candevice}/{pgn}/{messageName}",
      defaultValue = "/{candevice}/{pgn}/{messageName}",
      requiredMode = Schema.RequiredMode.REQUIRED
  )
  protected String topicNameTemplate = "/{candevice}/{pgn}/{messageName}";

  @Schema(
      description =
          "Topic to which raw CAN/NMEA 2000 frames are published when the PGN or message type is unknown. ",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      defaultValue = "/{candevice}/",
      example = "/{candevice}/unknown"
  )
  protected String unknownPacketTopic = "/{candevice}/unknown";

  @Schema(
      description =
          "Topic to which raw CAN/NMEA 2000 frames are published when the PGN or message type is unknown. ",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "/can1/#"
  )
  protected String inboundTopicName ;

  @Schema(
      description =
          "Convert incoming CANBUS frames into JSON using the registered N2K message definitions. "
              + "If false, raw binary frames are published.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean parseToJson = true;


  @Schema(
      description = "Monitors and publishes the mavlink drone position and details as AIS N2K events",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected boolean publishMavlinkDrones = true;

}
