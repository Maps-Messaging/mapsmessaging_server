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

package io.mapsmessaging.state.drone.tak.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Maps-specific link state metadata.")
public class TakLinkState {

  @Schema(description = "Link state.", example = "CONNECTED")
  private String state;

  @Schema(description = "Whether the link is connected.", example = "true")
  private Boolean connected;

  @Schema(description = "RSSI in dBm.", example = "-61")
  private Integer  rssiDbm;

  @Schema(description = "Signal to noise ratio in dB.", example = "18.5")
  private Double snrDb;

  @Schema(description = "Latency in milliseconds.", example = "42")
  private Double latencyMs;

  @Schema(description = "Receive error rate.", example = "0.01")
  private Double rxErrorRate;

  @Schema(description = "Transmit error rate.", example = "0.00")
  private Double txErrorRate;
}