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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "PlanItem",
    description = "Single ordered item within a plan, such as a waypoint, command, or task."
)
public class PlanItemDTO {

  @Schema(
      description = "Unique identifier for this plan item.",
      example = "item-001"
  )
  private String itemId;

  @Schema(
      description = "Execution order within the plan.",
      example = "0"
  )
  private int sequence;

  @Schema(
      description = "Participant assigned to execute this item. May be null for shared or unassigned items.",
      example = "participant-uav-1"
  )
  private String participantId;

  @Schema(
      description = "Command represented by this plan item."
  )
  private PlanCommandDTO command;

  @Schema(
      description = "Coordinate associated with this item, if applicable."
  )
  private PlanCoordinateDTO coordinate;

  @Schema(
      description = "Indicates whether this item is the current active item.",
      example = "false"
  )
  private boolean current;

  @Schema(
      description = "Indicates whether execution should automatically continue to the next item.",
      example = "true"
  )
  private boolean autocontinue;

  @Schema(
      description = "Coordinate or execution frame for this item.",
      example = "GLOBAL_RELATIVE_ALT"
  )
  private String frame;

  @Schema(
      description = "Mission type or plan item category.",
      example = "MISSION"
  )
  private String missionType;

  @Schema(
      description = "Additional item metadata, including source protocol fields."
  )
  private Map<String, Object> metadata = new LinkedHashMap<>();
}