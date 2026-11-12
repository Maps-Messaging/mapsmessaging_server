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

package io.mapsmessaging.state.config.capability;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCapability {

  @SerializedName("task_type")
  @Schema(name = "task_type", allowableValues = {
      "PlanTaskType_REPOSITION", "PlanTaskType_NAVIGATE", "PlanTaskType_ESCORT",
      "PlanTaskType_FOLLOW", "PlanTaskType_IDENTIFY", "PlanTaskType_TRACK",
      "PlanTaskType_SHADOW", "PlanTaskType_CLASSIFY", "PlanTaskType_DETECT",
      "PlanTaskType_SURVEY", "PlanTaskType_PATROL", "PlanTaskType_RECONNAISSANCE",
      "PlanTaskType_PICKET", "PlanTaskType_SCREEN", "PlanTaskType_STANDBY",
      "PlanTaskType_LOITER", "PlanTaskType_PREPARE", "PlanTaskType_SYNCHRONIZATION",
      "PlanTaskType_HANDOVER", "PlanTaskType_RESUPPLY", "PlanTaskType_RECOVER",
      "PlanTaskType_LAUNCH", "PlanTaskType_DETER", "PlanTaskType_WARN",
      "PlanTaskType_COVER", "PlanTaskType_MARK", "PlanTaskType_DESTROY",
      "PlanTaskType_NEUTRALIZE", "PlanTaskType_ENGAGE", "PlanTaskType_DEPLOY",
      "PlanTaskType_REMOVE", "PlanTaskType_JAM", "PlanTaskType_RELAY",
      "PlanTaskType_CLEAR", "PlanTaskType_AVOID", "PlanTaskType_BARRIER",
      "PlanTaskType_INSPECT"
  })
  private PlanTaskType taskType;

  @SerializedName("task_specialization")
  @Schema(name = "task_specialization")
  private TaskSpecialization specialization = TaskSpecialization.NONE;

  @SerializedName("authorities")
  @Schema(name = "authorities")
  private Authorities[] authorities;

}