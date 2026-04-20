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
 * Protocol-agnostic link quality/state.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Communication link quality and connectivity state.")
public class LinkState {

  @Schema(
      description = "High-level link state (e.g. CONNECTED, DEGRADED, LOST).",
      example = "CONNECTED",
      nullable = true
  )
  private String state;

  @Schema(
      description = "Indicates whether the link is currently connected.",
      example = "true",
      nullable = true
  )
  private Boolean connected;

  @Schema(
      description = "Received Signal Strength Indicator in dBm.",
      example = "-67",
      nullable = true
  )
  private Integer rssiDbm;

  @Schema(
      description = "Signal-to-noise ratio in decibels.",
      example = "25.4",
      nullable = true
  )
  private Double snrDb;

  @Schema(
      description = "Estimated round-trip latency in milliseconds.",
      example = "42.5",
      nullable = true
  )
  private Double latencyMs;

  @Schema(
      description = "Receive error rate as a ratio (0.0 to 1.0).",
      example = "0.01",
      minimum = "0.0",
      maximum = "1.0",
      nullable = true
  )
  private Double rxErrorRate;

  @Schema(
      description = "Transmit error rate as a ratio (0.0 to 1.0).",
      example = "0.005",
      minimum = "0.0",
      maximum = "1.0",
      nullable = true
  )
  private Double txErrorRate;
}