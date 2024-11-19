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

package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "MQTT V5 Protocol Configuration DTO")
public class MqttV5ConfigDTO extends MqttConfig {

  @Schema(description = "Type of the protocol configuration", example = "mqtt-v5")
  private final String type = "mqtt-v5";

  @Schema(description = "Minimum server keep-alive interval in seconds", example = "0")
  protected int minServerKeepAlive = 0;

  @Schema(description = "Maximum server keep-alive interval in seconds", example = "60")
  protected int maxServerKeepAlive = 60;
}
