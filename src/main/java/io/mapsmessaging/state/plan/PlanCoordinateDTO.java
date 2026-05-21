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

package io.mapsmessaging.state.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "PlanCoordinate",
    description = "Geospatial coordinate used by a plan item."
)
public class PlanCoordinateDTO {

  @Schema(
      description = "Latitude in decimal degrees.",
      example = "-33.8688"
  )
  private double latitude;

  @Schema(
      description = "Longitude in decimal degrees.",
      example = "151.2093"
  )
  private double longitude;

  @Schema(
      description = "Altitude in metres.",
      example = "120.5"
  )
  private double altitude;

  @Schema(
      description = "Altitude reference frame, such as AMSL, relative, terrain, or unknown.",
      example = "RELATIVE"
  )
  private String altitudeReference;

  @Schema(
      description = "Coordinate system used by this coordinate.",
      example = "WGS84"
  )
  private String coordinateSystem;
}