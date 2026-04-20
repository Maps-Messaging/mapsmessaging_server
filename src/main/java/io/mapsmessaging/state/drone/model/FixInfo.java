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
 * GNSS fix quality and accuracy information.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GNSS fix quality and positional accuracy information.")
public class FixInfo {

  @Schema(
      description = "Type of GNSS fix (e.g. NO_FIX, 2D, 3D, RTK).",
      example = "3D",
      nullable = true
  )
  private String fixType;

  @Schema(
      description = "Number of satellites used in the fix.",
      example = "12",
      nullable = true
  )
  private Integer satelliteCount;

  @Schema(
      description = "Horizontal dilution of precision.",
      example = "0.8",
      nullable = true
  )
  private Double hdop;

  @Schema(
      description = "Vertical dilution of precision.",
      example = "1.2",
      nullable = true
  )
  private Double vdop;

  @Schema(
      description = "Estimated horizontal position accuracy in meters.",
      example = "1.5",
      nullable = true
  )
  private Double horizontalAccuracyMeters;

  @Schema(
      description = "Estimated vertical position accuracy in meters.",
      example = "2.3",
      nullable = true
  )
  private Double verticalAccuracyMeters;
}