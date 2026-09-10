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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "TAK protocol configuration")
public class TakProtocolDTO {

  @Schema(
      description = "Deprecated. Configure the TAK server on a TCP or TLS endpoint using the CoT protocol.",
      example = "opentak.syd.mapsmessaging.io",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false,
      minLength = 1
  )
  private String hostname;

  @Schema(
      description = "Deprecated. Configure the port in the CoT endpoint URL.",
      example = "8088",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  private int port = 8088;

  @Schema(
      description = "Deprecated. CoT endpoint connection sharing is managed by NetworkManager.",
      example = "false",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      defaultValue = "false"
  )
  private boolean sharedConnection = false;

  @Schema(
      description = "Topic used to hand outbound CoT XML to a CoT endpoint protocol.",
      example = "/tak/cot",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  private String topic = null;

}
