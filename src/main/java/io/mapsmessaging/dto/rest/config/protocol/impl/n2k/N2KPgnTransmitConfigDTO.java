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

package io.mapsmessaging.dto.rest.config.protocol.impl.n2k;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
    name = "N2KPgnTransmitConfig",
    description = "Configuration for periodically transmitting a specific NMEA 2000 PGN."
)
public class N2KPgnTransmitConfigDTO {

  public static final boolean DEFAULT_ENABLED = true;
  public static final long DEFAULT_INTERVAL_MILLISECONDS = 1000L;

  public N2KPgnTransmitConfigDTO(
      boolean enabled,
      long intervalMilliseconds
  ) {
    this.enabled = enabled;
    this.intervalMilliseconds = intervalMilliseconds;
  }

  @Schema(
      description = "Enables transmission of this PGN.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean enabled = DEFAULT_ENABLED;

  @Schema(
      description = "Minimum interval in milliseconds between transmitted instances of this PGN. A value of 0 disables periodic transmission for this PGN.",
      example = "1000",
      defaultValue = "1000",
      minimum = "1",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected long intervalMilliseconds = DEFAULT_INTERVAL_MILLISECONDS;
}