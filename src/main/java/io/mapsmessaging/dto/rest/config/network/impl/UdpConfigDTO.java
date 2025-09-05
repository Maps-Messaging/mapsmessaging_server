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

package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@Schema(description = "UDP Configuration DTO")
public class UdpConfigDTO extends EndPointConfigDTO {

  @Schema(description = "Timeout for reusing packets, in milliseconds", example = "1000")
  private long packetReuseTimeout;

  @Schema(description = "Idle session timeout duration, in seconds", example = "600")
  private long idleSessionTimeout;

  @Schema(description = "Expiry time for HMAC host lookup cache, in seconds", example = "600")
  private long hmacHostLookupCacheExpiry;

  @Schema(description = "List of HMAC configurations for nodes")
  private List<HmacConfigDTO> hmacConfigList;
}
