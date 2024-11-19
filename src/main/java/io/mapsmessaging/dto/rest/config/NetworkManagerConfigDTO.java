/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.dto.rest.config;


import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Network Manager Configuration DTO")
public class NetworkManagerConfigDTO extends BaseConfigDTO {

  @Schema(description = "Prefer IPv6 addresses", example = "true")
  protected boolean preferIpV6Addresses;

  @Schema(description = "Scan for network changes", example = "true")
  protected boolean scanNetworkChanges;

  @Schema(description = "Scan interval in milliseconds", example = "60000")
  protected int scanInterval;

  @Schema(description = "Global endpoint server configuration", implementation = EndPointServerConfigDTO.class)
  protected EndPointServerConfigDTO global;

  @Schema(description = "List of endpoint server configurations")
  protected List<EndPointServerConfigDTO> endPointServerConfigList;
}
