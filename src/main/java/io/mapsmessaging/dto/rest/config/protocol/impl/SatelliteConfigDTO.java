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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Base Satellite Configuration DTO")
public class SatelliteConfigDTO extends BaseSatelliteConfigDTO {

  @Schema(description = "URL of the server")
  protected String baseUrl;

  @Schema(description = "HTTP Request time out in seconds")
  protected int httpRequestTimeout;

  @Schema(description = "Max number of events to be in flight per each modems")
  protected int maxInflightEventsPerDevice;


  @Schema(
      description = "Topic template for publishing decoded common (SIN < 127) inbound messages (after parsing SIN/MIN).",
      example = "/{deviceId}/common/in/{sin}/{min}",
      defaultValue = "/{deviceId}/common/in/{sin}/{min}"
  )
  protected String commonInboundPublishRoot;

  @Schema(
      description = "Topic root for accepting outbound common (SIN < 127) messages to be encoded and sent to the modem. Wildcards are allowed.",
      example = "/{deviceId}/common/out/#",
      defaultValue = "/{deviceId}/common/out/#"
  )
  protected String commonOutboundPublishRoot;

  @Schema(
      description = "Topic template for publishing decoded MAPS (SIN 147) inbound messages into a namespace tree (after parsing).",
      example = "/{deviceId}/maps/in/{namespace}/#",
      defaultValue = "/{deviceId}/maps/in/{namespace}/#"
  )
  protected String mapsInboundPublishRoot;

  @Schema(
      description = "Topic template for accepting outbound MAPS (SIN 147) messages from a namespace tree to be encoded and sent to the modem.",
      example = "/{deviceId}/maps/out/{namespace}/#",
      defaultValue = "/{deviceId}/maps/out/{namespace}/#"
  )
  protected String mapsOutboundPublishRoot;

  @Schema(
      description = "Topic used to broadcast a message to all modems/clients (encoded and sent to each).",
      example = "/inmarsat/broadcast",
      defaultValue = "/inmarsat/broadcast"
  )
  protected String outboundBroadcast;




  @Schema(description = "Mailbox ID")
  protected String mailboxId;

  @Schema(description = "Mailbox password")
  protected String mailboxPassword;



  @Schema(description = "Device Info update time in minutes")
  protected int deviceInfoUpdateMinutes;

}
