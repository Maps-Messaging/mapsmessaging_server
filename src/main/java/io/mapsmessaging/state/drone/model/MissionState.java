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
 * Mission execution state for the drone or vehicle.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mission execution state for the drone or vehicle.")
public class MissionState {

  @Schema(
      description = "Unique identifier of the mission.",
      example = "mission-001",
      nullable = true
  )
  private String missionId;

  @Schema(
      description = "Index of the current waypoint being executed.",
      example = "5",
      nullable = true
  )
  private Integer currentWaypointIndex;

  @Schema(
      description = "Total number of waypoints in the mission.",
      example = "20",
      nullable = true
  )
  private Integer totalWaypoints;

  @Schema(
      description = "Target position of the current or next waypoint.",
      nullable = true
  )
  private GeoPosition targetPosition;
}