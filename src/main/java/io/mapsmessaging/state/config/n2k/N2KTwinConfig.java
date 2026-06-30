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

package io.mapsmessaging.state.config.n2k;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "NMEA 2000 twin integration configuration.")
public class N2KTwinConfig {


  protected boolean enable = true;

  @Schema(
      description = "Enables publishing tracked MAVLink drones and other known entities as NMEA 2000 AIS events.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean publishMavlinkDrones = true;

  @Schema(
      description =
          "Messaging topic subscription used to receive decoded NMEA 2000 JSON events for twin monitoring.",
      example = "/canbus0/n2k/json/#",
      defaultValue = "/canbus0/n2k/json/#",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected String topic = "/canbus0/n2k/json/#";

  @Schema(
      description =
          "Logical name for this NMEA 2000 twin integration. Used for display, logging and source identification.",
      example = "canbus0-n2k-drone-feed",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String name;

  @Schema(
      description =
          "Vehicle class associated with this NMEA 2000 feed. Used to describe the monitored platform.",
      example = "USV",
      allowableValues = {"UAV", "UGV", "USV", "UUV", "VESSEL", "ROVER", "UNKNOWN"},
      defaultValue = "UNKNOWN",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String vehicleClass;

  @Schema(
      description =
          "AIS publishing configuration used when MAVLink drones or other tracked entities are projected "
              + "onto NMEA 2000 AIS PGNs.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = false
  )
  protected N2KAisConfigDTO ais = new N2KAisConfigDTO();
}
