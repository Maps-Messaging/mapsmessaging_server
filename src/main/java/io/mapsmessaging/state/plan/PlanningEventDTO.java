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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "PlanningEvent",
    description = "Event describing a change to a plan, participant, or plan item."
)
public class PlanningEventDTO {

  @Schema(
      description = "Unique event identifier.",
      example = "event-20260514-001"
  )
  private String eventId;

  @Schema(
      description = "Identifier of the plan affected by this event.",
      example = "plan-20260514-001"
  )
  private String planId;

  @Schema(
      description = "Type of planning event."
  )
  private PlanningEventType eventType;

  @Schema(
      description = "Event timestamp as epoch milliseconds.",
      example = "1778726460000"
  )
  private long timestamp;

  @Schema(
      description = "Source that generated the event.",
      example = "MAPS"
  )
  private String source;

  @Schema(
      description = "Participants affected by this event."
  )
  private List<String> affectedParticipantIds = new ArrayList<>();

  @Schema(
      description = "Entity twins affected by this event."
  )
  private List<String> affectedTwinIds = new ArrayList<>();

  @Schema(
      description = "Plan items affected by this event."
  )
  private List<String> affectedItemIds = new ArrayList<>();

  @Schema(
      description = "Previous state before the event, when applicable.",
      example = "CREATED"
  )
  private String previousState;

  @Schema(
      description = "New state after the event, when applicable.",
      example = "ACTIVE"
  )
  private String newState;

  @Schema(
      description = "Additional event metadata, including source protocol details."
  )
  private Map<String, Object> metadata = new LinkedHashMap<>();
}