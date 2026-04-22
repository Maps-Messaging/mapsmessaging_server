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
@Schema(description = "TAK protocol configuration")
public class TakProtocolDTO extends ProtocolConfigDTO {

  @Schema(
      description = "Hostname or IP address of the TAK server.",
      example = "opentak.syd.mapsmessaging.io",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1
  )
  private String hostname;

  @Schema(
      description = "Port of the TAK server (e.g. 8088 for TCP, 8089 for TLS).",
      example = "8088",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private int port = 8088;

  @Schema(
      description = "If true, all twins share a single TAK socket connection. If false, each twin uses its own socket.",
      example = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "false"
  )
  private boolean sharedConnection = false;

  @Schema(
      description = "Topic to publish TAK CoT XML messages to.",
      example = "tak/events",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String topic =null;


  public TakProtocolDTO() {
    super("tak");
  }
}