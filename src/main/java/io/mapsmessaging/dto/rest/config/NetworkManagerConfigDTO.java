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

package io.mapsmessaging.dto.rest.config;


import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Network Manager Configuration DTO")
public class NetworkManagerConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Prefer IPv6 addresses when both IPv4 and IPv6 are available",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean preferIpV6Addresses = true;

  @Schema(
      description = "Scan for network changes (interface up/down, address changes, etc.)",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected boolean scanNetworkChanges = true;

  @Schema(
      description = "Interval in milliseconds to scan for new/changed network interfaces",
      example = "60000",
      defaultValue = "60000",
      minimum = "10000",
      maximum = "600000",
      requiredMode = Schema.RequiredMode.REQUIRED,
      nullable = false
  )
  protected int scanInterval = 60000;

  @Schema(
      description = "List of endpoint server configurations",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true,
      example = "[{\"type\":\"mqtt\",\"enabled\":true,\"hostnames\":\"0.0.0.0\",\"port\":1883}]"
  )
  protected List<EndPointServerConfigDTO> endPointServerConfigList;
}
