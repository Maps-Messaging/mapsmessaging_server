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
 * Protocol-agnostic geodetic position (degrees, meters).
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Geodetic position of the entity expressed in latitude, longitude and altitude.")
public class GeoPosition {

  @Schema(
      description = "Latitude in decimal degrees.",
      example = "-33.8688",
      minimum = "-90",
      maximum = "90",
      nullable = true
  )
  private Double latitude;

  @Schema(
      description = "Longitude in decimal degrees.",
      example = "151.2093",
      minimum = "-180",
      maximum = "180",
      nullable = true
  )
  private Double longitude;

  @Schema(
      description = "Altitude above mean sea level in meters.",
      example = "120.5",
      nullable = true
  )
  private Double altitudeMslMeters;

  @Schema(
      description = "Altitude above ground level in meters.",
      example = "35.2",
      nullable = true
  )
  private Double altitudeAglMeters;
}