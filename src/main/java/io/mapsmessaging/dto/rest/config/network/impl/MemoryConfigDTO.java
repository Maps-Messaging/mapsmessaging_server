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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "memorySecurity")
@Schema(description = "Shared-memory/RDMA byte-stream endpoint configuration")
public abstract class MemoryConfigDTO extends EndPointConfigDTO {
  protected MemoryConfigDTO(String type) {
    super(type);
  }

  @Schema(description = "Unix control socket path or Windows local pipe name for native shared memory")
  protected String localAddress;
  @Schema(description = "Use native local admission; false explicitly selects legacy loopback file-backed shared memory", defaultValue = "true")
  protected boolean nativeSharedMemory = true;
  protected int slotSize = 65536;
  protected int slotCount = 256;
  protected int maxConnections = 64;
  protected int maxPendingConnections = 64;
  protected int maxNegotiations = 16;
  protected int handshakeTimeoutMillis = 5000;
  @Schema(description = "Maximum idle readiness polling interval, in microseconds", minimum = "1", defaultValue = "1000")
  protected long readinessPollMicros = 1000;
  protected MemorySecurityConfigDTO memorySecurity = new MemorySecurityConfigDTO();
}
