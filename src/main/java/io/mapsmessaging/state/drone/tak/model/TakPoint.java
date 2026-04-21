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
@Schema(description = "TAK point element.")
public class TakPoint {

  @Schema(description = "Latitude in decimal degrees.", example = "47.3979981")
  private Double lat;

  @Schema(description = "Longitude in decimal degrees.", example = "8.5461638")
  private Double lon;

  @Schema(description = "Height above ellipsoid in meters.", example = "0.253")
  private Double hae;

  @Schema(description = "Circular error in meters.", example = "10.0")
  private Double ce;

  @Schema(description = "Linear error in meters.", example = "15.0")
  private Double le;
}