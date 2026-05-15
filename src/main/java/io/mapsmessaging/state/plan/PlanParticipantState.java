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

@Schema(
    description = "Execution state of a participant assigned to a plan."
)
public enum PlanParticipantState {

  @Schema(description = "Participant has been assigned to the plan.")
  ASSIGNED,

  @Schema(description = "Participant is actively executing the plan or assigned task.")
  ACTIVE,

  @Schema(description = "Participant execution is paused.")
  PAUSED,

  @Schema(description = "Participant has completed its assigned work.")
  COMPLETED,

  @Schema(description = "Participant failed to complete its assigned work.")
  FAILED,

  @Schema(description = "Participant has been removed from the plan.")
  REMOVED
}