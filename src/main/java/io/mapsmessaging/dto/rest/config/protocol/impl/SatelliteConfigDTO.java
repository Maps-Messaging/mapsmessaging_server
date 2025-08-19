/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Base Satellite Configuration DTO")
public class SatelliteConfigDTO extends ProtocolConfigDTO {

  @Schema(description = "URL of the server")
  protected String baseUrl;

  @Schema(description = "Interval between polling for incoming messages")
  protected int pollInterval;

  @Schema(description = "HTTP Request time out in seconds")
  protected int httpRequestTimeout;

  @Schema(description = "Max number of events to be in flight per each modems")
  protected int maxInflightEventsPerDevice;

  @Schema(description = "Namespace path for outbound topic mapping for individual modems")
  protected String outboundNamespaceRoot;

  @Schema(description = "Namespace path for outbound topic broadcast for all devices")
  protected String outboundBroadcast;

  @Schema(description = "Mailbox ID")
  protected String mailboxId;

  @Schema(description = "Mailbox password")
  protected String mailboxPassword;

  @Schema(description = "Namespace root for the mailbox")
  protected String namespace;

  @Schema(description = "Device Info update time in minutes")
  protected int deviceInfoUpdateMinutes;

  @Schema(description = "maximum buffer size allowed by the satellite communications", example = "4000", defaultValue = "4000")
  protected int maxBufferSize;

  @Schema(description = "minimum sized buffer that will be compressed", example = "512", defaultValue = "256")
  protected int compressionCutoffSize;
}
