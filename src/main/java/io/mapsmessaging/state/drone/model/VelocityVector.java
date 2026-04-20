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
 * Protocol-agnostic velocity vector (meters per second).
 * NED convention where down is positive toward Earth center.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Velocity vector using North-East-Down (NED) coordinate system in meters per second.")
public class VelocityVector {

  @Schema(
      description = "Velocity component toward geographic north in meters per second.",
      example = "5.2",
      nullable = true
  )
  private Double northMetersPerSecond;

  @Schema(
      description = "Velocity component toward geographic east in meters per second.",
      example = "-1.3",
      nullable = true
  )
  private Double eastMetersPerSecond;

  @Schema(
      description = "Velocity component toward Earth center (down) in meters per second. Positive values indicate descent.",
      example = "0.8",
      nullable = true
  )
  private Double downMetersPerSecond;
}