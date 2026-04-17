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

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(description = "MQTT Last Will and Testament (LWT) configuration")
public class MqttWillConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Topic to publish the Will message to",
      example = "system/bridge/client1/status",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String topic;

  @Schema(
      description = "Payload to publish when the Will is triggered",
      example = "{\"status\":\"offline\"}",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String payload="";

  @Schema(
      description = "Payload encoding type (string or base64)",
      example = "string",
      defaultValue = "string",
      allowableValues = {"string", "base64"},
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected String payloadEncoding = "string";

  @Schema(
      description = "Quality of Service level for the Will message (0, 1, or 2)",
      example = "1",
      defaultValue = "0",
      minimum = "0",
      maximum = "2",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected int qos = 0;

  @Schema(
      description = "Retain flag for the Will message",
      example = "true",
      defaultValue = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected boolean retain = false;

  @Schema(
      description = "MQTT v5 Will Delay Interval in seconds before publishing the Will",
      example = "15",
      defaultValue = "0",
      minimum = "0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected long delayInterval = 0;

  @Schema(
      description = "MQTT v5 Message Expiry Interval in seconds",
      example = "300",
      defaultValue = "0",
      minimum = "0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected long messageExpiryInterval = 0;

  @Schema(
      description = "MQTT v5 Content Type of the Will message",
      example = "application/json",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String contentType;

  @Schema(
      description = "MQTT v5 Payload Format Indicator (0 = unspecified, 1 = UTF-8)",
      example = "1",
      defaultValue = "0",
      minimum = "0",
      maximum = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected int payloadFormatIndicator = 0;
}