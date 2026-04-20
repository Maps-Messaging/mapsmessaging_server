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
 * Protocol-agnostic body orientation (degrees).
 * Roll, pitch, yaw follow standard aerospace conventions.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Body orientation expressed as roll, pitch, and yaw in degrees.")
public class Orientation {

  @Schema(
      description = "Roll angle in degrees (rotation around the longitudinal axis).",
      example = "2.5",
      nullable = true
  )
  private Double rollDegrees;

  @Schema(
      description = "Pitch angle in degrees (rotation around the lateral axis).",
      example = "-1.2",
      nullable = true
  )
  private Double pitchDegrees;

  @Schema(
      description = "Yaw angle in degrees (rotation around the vertical axis).",
      example = "180.0",
      minimum = "0.0",
      maximum = "360.0",
      nullable = true
  )
  private Double yawDegrees;
}