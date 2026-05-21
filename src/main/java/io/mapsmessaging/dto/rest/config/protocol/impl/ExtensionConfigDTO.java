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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(
    title = "Extension Protocol Configuration DTO",
    description =
        "Generic protocol configuration for third-party integrations (e.g., IBM MQ, Pulsar, ROS). "
            + "The 'config' object is intentionally untyped and may contain any JSON object structure.",
    additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
    additionalPropertiesSchema = Object.class
)

public class ExtensionConfigDTO extends ProtocolConfigDTO {

  public ExtensionConfigDTO() {
    super("extension");
  }

  @Schema(
      description = "Name of the extension protocol implementation (e.g., ibmmq, pulsar, ros).",
      example = "pulsar",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected String protocol;

  @Schema(
      description =
          "Protocol-specific configuration object. This is intentionally untyped and may contain any JSON object structure.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
      additionalPropertiesSchema = Object.class,
      example = "{\"url\":\"pulsar://localhost:6650\",\"tenant\":\"public\",\"namespace\":\"default\"}"
  )
  protected Map<String, Object> config;
}
