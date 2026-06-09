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
@Schema(description = "MQTT Protocol Configuration DTO")
public class MqttConfigDTO extends ProtocolConfigDTO {

  public MqttConfigDTO() {
    super("mqtt");
  }

  @Schema(
      description = "Minimum server keep-alive interval in seconds. A value of 0 disables the lower bound.",
      example = "0",
      defaultValue = "0",
      minimum = "0",
      maximum = "65535"
  )
  protected int minServerKeepAlive = 0;

  @Schema(
      description = "Maximum server keep-alive interval in seconds. A value of 0 allows the client supplied keep-alive to be used without a configured upper bound.",
      example = "60",
      defaultValue = "60",
      minimum = "0",
      maximum = "65535"
  )
  protected int maxServerKeepAlive = 60;

  @Schema(
      description = "Maximum MQTT session expiry interval in seconds. Used for MQTT 5 session expiry handling.",
      example = "86400",
      defaultValue = "86400",
      minimum = "0",
      maximum = "4294967295"
  )
  protected long maximumSessionExpiry = 86400;

  @Schema(
      description = "Maximum MQTT buffer size in bytes used for queued or in-flight MQTT data.",
      example = "10485760",
      defaultValue = "10485760",
      minimum = "1024"
  )
  protected long maximumBufferSize = 10485760;

  @Schema(
      description = "Maximum number of QoS 1 and QoS 2 publications the server allows to be in-flight from a client.",
      example = "10",
      defaultValue = "10",
      minimum = "1",
      maximum = "65535"
  )
  protected int serverReceiveMaximum = 10;

  @Schema(
      description = "Maximum number of QoS 1 and QoS 2 publications the client allows to be in-flight from the server.",
      example = "65535",
      defaultValue = "65535",
      minimum = "1",
      maximum = "65535"
  )
  protected int clientReceiveMaximum = 65535;

  @Schema(
      description = "Maximum topic alias value the client may use when publishing to the server. A value of 0 disables client topic aliases.",
      example = "32767",
      defaultValue = "32767",
      minimum = "0",
      maximum = "65535"
  )
  protected int clientMaximumTopicAlias = 32767;

  @Schema(
      description = "Maximum topic alias value the server may use when publishing to the client. A value of 0 disables server topic aliases.",
      example = "0",
      defaultValue = "0",
      minimum = "0",
      maximum = "65535"
  )
  protected int serverMaximumTopicAlias = 0;

  @Schema(
      description = "Indicates if strict client identifier enforcement is enabled. When enabled, invalid or missing client identifiers are rejected instead of being relaxed by server policy.",
      example = "false",
      defaultValue = "false"
  )
  protected boolean strictClientId = false;

  @Schema(
      description = "MQTT protocol version accepted by this listener. AUTO allows the server to detect the MQTT version from the CONNECT packet.",
      example = "AUTO",
      defaultValue = "AUTO",
      allowableValues = {"AUTO", "MQTT_3_1_1", "MQTT_5"}
  )
  protected MqttVersion version = MqttVersion.AUTO;
}