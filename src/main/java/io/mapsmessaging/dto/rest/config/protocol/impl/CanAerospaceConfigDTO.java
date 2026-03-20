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
 *
 */

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "CANAerospace protocol configuration")
public class CanAerospaceConfigDTO extends ProtocolConfigDTO {

  @Schema(
      description = "Optional path to an external CANAerospace YAML schema file. If omitted, the built-in schema bundled in the server is used.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      minLength = 1,
      example = "/etc/maps/canaerospace/canaerospace-schema.yaml"
  )
  protected String yamlPath;

  @Schema(
      description = "Topic name template used when publishing decoded CANAerospace messages. Supported placeholders: {candevice}, {messageName}.",
      example = "/{candevice}/{messageName}",
      defaultValue = "/{candevice}/{messageName}",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String topicNameTemplate = "/{candevice}/{messageName}";

  @Schema(
      description = "Topic used when publishing raw CANAerospace frames that could not be mapped to a decoded message name.",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      defaultValue = "/{candevice}/unknown",
      example = "/{candevice}/unknown"
  )
  protected String unknownPacketTopic = "/{candevice}/unknown";

  @Schema(
      description = "Optional inbound topic subscription used to receive outbound CANAerospace messages for transmission onto the CAN bus.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "/can1/#"
  )
  protected String inboundTopicName;

  @Schema(
      description = "Convert incoming CANAerospace frames into JSON using the configured schema. If false, raw binary CAN frames are published.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected boolean parseToJson = true;

  public CanAerospaceConfigDTO() {
    super("canaerospace");
  }
}