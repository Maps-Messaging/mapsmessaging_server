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

package io.mapsmessaging.state.drone.model.autopilot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Generic autopilot state associated with a vehicle twin.")
public abstract class AutopilotState {

  @Schema(
      description = "Autopilot implementation type.",
      example = "PX4",
      nullable = true
  )
  private String autopilotType;

  @Schema(
      description = "MAVLink base mode bitmask reported by the autopilot heartbeat.",
      example = "145",
      nullable = true
  )
  private Integer baseMode;

  @Schema(
      description = "Autopilot-specific custom mode value reported by the heartbeat.",
      example = "16973824",
      nullable = true
  )
  private Long customMode;

  @Schema(
      description = "MAVLink system status code reported by the autopilot.",
      example = "4",
      nullable = true
  )
  private Integer systemStatus;

  @Schema(
      description = "MAVLink protocol version reported by the autopilot heartbeat.",
      example = "3",
      nullable = true
  )
  private Integer mavlinkVersion;

  @Schema(
      description = "Autopilot unique identifier reported by AUTOPILOT_VERSION.",
      example = "5283920058631409232",
      nullable = true
  )
  private Long uid;

  @Schema(
      description = "Raw packed flight software version reported by AUTOPILOT_VERSION.",
      example = "17891392",
      nullable = true
  )
  private Long flightSoftwareVersionRaw;

  @Schema(
      description = "Decoded flight software version string.",
      example = "1.17.0",
      nullable = true
  )
  private String flightSoftwareVersion;

  @Schema(
      description = "Raw packed middleware software version reported by AUTOPILOT_VERSION.",
      example = "17891392",
      nullable = true
  )
  private Long middlewareSoftwareVersionRaw;

  @Schema(
      description = "Decoded middleware software version string.",
      example = "1.17.0",
      nullable = true
  )
  private String middlewareSoftwareVersion;

  @Schema(
      description = "Raw packed operating system software version reported by AUTOPILOT_VERSION.",
      example = "101777663",
      nullable = true
  )
  private Long osSoftwareVersionRaw;

  @Schema(
      description = "Decoded operating system software version string.",
      example = "6.17.0",
      nullable = true
  )
  private String osSoftwareVersion;

  @Schema(
      description = "Capability bitmask reported by AUTOPILOT_VERSION.",
      example = "59647",
      nullable = true
  )
  private Long capabilities;

  public String getFlightMode() {
    return null;
  }
}