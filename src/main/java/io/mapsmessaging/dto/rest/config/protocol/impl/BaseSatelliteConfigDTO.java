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
@Schema(description = "Common Satellite Configuration DTO")
public class BaseSatelliteConfigDTO extends ProtocolConfigDTO {

  public BaseSatelliteConfigDTO(String type){
    super(type);
  }

  @Schema(description = "Time in seconds to poll the modem for incoming messages", example = "15", defaultValue = "10")
  protected int incomingMessagePollInterval = 10;

  @Schema(description = "Time in seconds to poll for outgoing messages", example = "60", defaultValue = "60")
  protected int outgoingMessagePollInterval = 60;

  @Schema(description = "maximum buffer size allowed by the satellite communications", example = "4000", defaultValue = "4000")
  protected int maxBufferSize = 4000;

  @Schema(description = "minimum sized buffer that will be compressed", example = "512", defaultValue = "128")
  protected int compressionCutoffSize = 128;

  @Schema(description = "life time of message in minutes", example = "5", defaultValue = "10")
  protected int messageLifeTimeInMinutes = 10;

  @Schema(description = "Shared secret for encryption", example="this is a shared secret", defaultValue = "")
  protected String sharedSecret;

  @Schema(description = "If set, then high priority messages will NOT be queued, will incur additional charges", defaultValue = "false", example = "false")
  protected boolean sendHighPriorityMessages = false;

  @Schema(description = "The SIN number that maps should use, must be greater then 128", defaultValue = "147", example = "147")
  protected int sinNumber = 147;

}
