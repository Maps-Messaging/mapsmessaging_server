/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.dto.rest.config.protocol.impl;

import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Cursor on Target XML stream protocol configuration.")
public class CotConfigDTO extends ProtocolConfigDTO {

  public CotConfigDTO() {
    super("cot");
  }

  @Schema(description = "Topic receiving complete CoT XML events read from the endpoint.", example = "/tak/cot/inbound")
  protected String inboundTopicName = "/tak/cot/inbound";

  @Schema(description = "Topic supplying CoT XML events to write to the endpoint.", example = "/tak/cot")
  protected String outboundTopicName = "/tak/cot";

  @Schema(description = "Maximum size of one CoT XML event in bytes.", defaultValue = "1048576", minimum = "256")
  protected int maximumEventSize = 1_048_576;

  @Schema(description = "Maximum internal protocol session lifetime in seconds; zero means no expiry.", defaultValue = "0")
  protected int maximumSessionExpiry;

  @Schema(description = "Quality of service used for inbound and outbound topic traffic.", defaultValue = "0", minimum = "0", maximum = "2")
  protected int qualityOfService;

  @Schema(description = "Store inbound events while topic consumers are offline.", defaultValue = "false")
  protected boolean storeOffline;

  @Schema(description = "Append a newline after outbound CoT events when one is not already present.", defaultValue = "true")
  protected boolean appendNewLine = true;

  @Schema(description = "TAK client presence sent by outbound client connections.")
  protected CotPresenceConfigDTO presence = new CotPresenceConfigDTO();
}
