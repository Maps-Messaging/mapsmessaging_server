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

  @Schema(description = "Mark outbound events and discard them if a TAK server echoes them back.", defaultValue = "true")
  protected boolean suppressEchoes = true;

  @Schema(description = "Stable origin written into outbound CoT echo markers. Supports {interfaceName}.", example = "maps-{interfaceName}")
  protected String echoOrigin = "maps-{interfaceName}";

  @Schema(description = "Maximum Maps bridge hops before an event is discarded.", defaultValue = "4", minimum = "1")
  protected int maximumHopCount = 4;

  @Schema(description = "Maximum semantic fingerprints retained for echo and duplicate detection.", defaultValue = "4096", minimum = "1")
  protected int fingerprintCacheSize = 4096;

  @Schema(description = "Seconds semantic fingerprints remain eligible for suppression.", defaultValue = "120", minimum = "1")
  protected int fingerprintCacheTtlSeconds = 120;

  @Schema(description = "Permitted clock difference when evaluating stale events.", defaultValue = "5", minimum = "0")
  protected int clockSkewSeconds = 5;

  @Schema(description = "Maximum UID timestamp entries retained for out-of-order protection.", defaultValue = "10000", minimum = "1")
  protected int maximumTrackedUids = 10_000;

  @Schema(description = "Maximum events waiting in the CoT inbound processing queue.", defaultValue = "1024", minimum = "1")
  protected int inboundQueueDepth = 1024;

  @Schema(description = "Maximum events waiting or writing in the CoT outbound queue.", defaultValue = "1024", minimum = "1")
  protected int outboundQueueDepth = 1024;

  @Schema(description = "Maximum permitted XML element nesting depth.", defaultValue = "64", minimum = "4")
  protected int maximumXmlDepth = 64;

  @Schema(description = "Seconds a CoT frame may remain incomplete in the socket writer before the connection is closed.", defaultValue = "30", minimum = "1")
  protected int writeTimeoutSeconds = 30;

  @Schema(description = "TAK client presence sent by outbound client connections.")
  protected CotPresenceConfigDTO presence = new CotPresenceConfigDTO();
}
