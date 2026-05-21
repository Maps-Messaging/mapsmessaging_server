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
    name = "Plan",
    description = "Protocol-neutral operational plan that may involve one or more participants."
)
public class PlanDTO {

  @Schema(
      description = "Unique plan identifier.",
      example = "plan-20260514-001"
  )
  private String planId;

  @Schema(
      description = "Human-readable plan name.",
      example = "Harbour survey"
  )
  private String name;

  @Schema(
      description = "Human-readable plan description.",
      example = "Survey route covering the northern harbour boundary."
  )
  private String description;

  @Schema(
      description = "Source that created or supplied this plan.",
      example = "MISSION_PLANNER"
  )
  private String source;

  @Schema(
      description = "Plan creation timestamp as epoch milliseconds.",
      example = "1778726400000"
  )
  private long createdTimestamp;

  @Schema(
      description = "Last update timestamp as epoch milliseconds.",
      example = "1778726460000"
  )
  private long updatedTimestamp;

  @Schema(
      description = "Current lifecycle state of the plan."
  )
  private PlanState state;

  @Schema(
      description = "Participants assigned to this plan."
  )
  private List<PlanParticipantDTO> participants = new ArrayList<>();

  @Schema(
      description = "Ordered items that make up the plan."
  )
  private List<PlanItemDTO> items = new ArrayList<>();

  @Schema(
      description = "Additional plan metadata, including source protocol fields."
  )
  private Map<String, Object> metadata = new LinkedHashMap<>();
}