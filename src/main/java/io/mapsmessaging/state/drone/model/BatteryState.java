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

package io.mapsmessaging.state.drone.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Protocol-agnostic battery state.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Battery and power state for the drone or vehicle.")
public class BatteryState {

  @Schema(
      description = "Remaining battery percentage.",
      example = "78.5",
      nullable = true
  )
  private Double percentage;

  @Schema(
      description = "Battery voltage in volts.",
      example = "22.4",
      nullable = true
  )
  private Double voltageVolts;

  @Schema(
      description = "Battery current draw in amps.",
      example = "12.7",
      nullable = true
  )
  private Double currentAmps;

  @Schema(
      description = "Estimated remaining battery capacity in milliamp-hours.",
      example = "4200",
      nullable = true
  )
  private Double remainingMilliampHours;

  @Schema(
      description = "Battery temperature in degrees Celsius.",
      example = "34.2",
      nullable = true
  )
  private Double temperatureCelsius;

  @Schema(
      description = "Indicates whether the battery is currently charging.",
      example = "false",
      nullable = true
  )
  private Boolean charging;
}