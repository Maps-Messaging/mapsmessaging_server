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

import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "OrbComm ST and OGi Modem Protocol Configuration DTO")
public class StoGiConfigDTO extends BaseSatelliteConfigDTO {

  @Schema(description = "Serial port configuration")
  protected SerialConfigDTO serial;

  @Schema(description = "Time in milliseconds to wait for a modem response")
  protected long modemResponseTimeout;

  @Schema(description = "Initial modem setup string")
  protected String initialSetup;

  @Schema(description ="Time in seconds between polling modem location and statistics, 0 disables it", example = "60", defaultValue = "0")
  protected long locationPollInterval;

  @Schema(description = "If present, then the name of the topic to send modem statistics to")
  protected String modemStatsTopic;

}
