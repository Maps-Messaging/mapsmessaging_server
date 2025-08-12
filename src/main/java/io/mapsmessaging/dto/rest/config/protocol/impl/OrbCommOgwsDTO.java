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
@Schema(description = "OrbComm OGWS Protocol Configuration DTO")
public class OrbCommOgwsDTO  extends ProtocolConfigDTO {

  @Schema (description = "Orbcomm OSGI gateway URL to use as the base")
  protected String baseUrl;

  @Schema(description = "Interval between polling the OGWS for incoming messages")
  protected int pollInterval;

  @Schema(description = "HTTP Request time out in seconds")
  protected int httpRequestTimeout;

  @Schema(description = "Max number of events to be in flight per each modems")
  protected int maxInflightEventsPerModem;

  @Schema(description = "Namespace path for outbound topic mapping for individual modems")
  protected String outboundNamespaceRoot;


}
