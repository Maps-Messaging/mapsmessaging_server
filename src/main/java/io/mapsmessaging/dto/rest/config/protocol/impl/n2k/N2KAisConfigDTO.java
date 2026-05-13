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

@Data
@Schema(
    name = "N2KAisConfig",
    description = "Configuration for publishing AIS-related NMEA 2000 PGNs from tracked entity state."
)
public class N2KAisConfigDTO {

  public static final boolean DEFAULT_ENABLED = true;

  public N2KAisConfigDTO() {
  }

  @Schema(
      description = "Enables AIS PGN publishing from tracked entity state.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean enabled = DEFAULT_ENABLED;

  @Schema(
      description = "AIS Class B Position Report, PGN 129039, transmit configuration.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected N2KPgnTransmitConfigDTO pgn129039 = new N2KPgnTransmitConfigDTO(
      true,
      1000L
  );

  @Schema(
      description = "AIS Class B Extended Position Report, PGN 129040, transmit configuration.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected N2KPgnTransmitConfigDTO pgn129040 = new N2KPgnTransmitConfigDTO(
      true,
      2000L
  );

  @Schema(
      description = "AIS Class B static data part A, PGN 129809, transmit configuration.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected N2KPgnTransmitConfigDTO pgn129809 = new N2KPgnTransmitConfigDTO(
      true,
      60_000L
  );

  @Schema(
      description = "AIS Class B static data part B, PGN 129810, transmit configuration.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected N2KPgnTransmitConfigDTO pgn129810 = new N2KPgnTransmitConfigDTO(
      true,
      60_000L
  );
}