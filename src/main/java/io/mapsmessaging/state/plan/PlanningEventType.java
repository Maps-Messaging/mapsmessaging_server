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
    description = "Type of planning lifecycle or execution event."
)
public enum PlanningEventType {

  @Schema(description = "A new plan was created.")
  PLAN_CREATED,

  @Schema(description = "An existing plan was updated.")
  PLAN_UPDATED,

  @Schema(description = "A plan was deleted.")
  PLAN_DELETED,

  @Schema(description = "A plan was received from an external source.")
  PLAN_RECEIVED,

  @Schema(description = "A plan passed validation.")
  PLAN_VALIDATED,

  @Schema(description = "A plan was rejected or failed validation.")
  PLAN_REJECTED,

  @Schema(description = "A participant was added to a plan.")
  PARTICIPANT_ADDED,

  @Schema(description = "A participant was removed from a plan.")
  PARTICIPANT_REMOVED,

  @Schema(description = "A participant assignment or state was updated.")
  PARTICIPANT_UPDATED,

  @Schema(description = "A plan item was added.")
  ITEM_ADDED,

  @Schema(description = "A plan item was updated.")
  ITEM_UPDATED,

  @Schema(description = "A plan item was removed.")
  ITEM_REMOVED,

  @Schema(description = "Plan items were reordered.")
  ITEM_REORDERED,

  @Schema(description = "A plan was activated.")
  PLAN_ACTIVATED,

  @Schema(description = "Plan execution started.")
  PLAN_STARTED,

  @Schema(description = "Plan execution paused.")
  PLAN_PAUSED,

  @Schema(description = "Plan execution resumed.")
  PLAN_RESUMED,

  @Schema(description = "Plan execution completed.")
  PLAN_COMPLETED,

  @Schema(description = "Plan execution aborted.")
  PLAN_ABORTED,

  @Schema(description = "A plan item started execution.")
  ITEM_STARTED,

  @Schema(description = "A plan item was reached.")
  ITEM_REACHED,

  @Schema(description = "A plan item was skipped.")
  ITEM_SKIPPED,

  @Schema(description = "A plan item failed.")
  ITEM_FAILED,

  @Schema(description = "Mission upload started.")
  MISSION_UPLOAD_STARTED,

  @Schema(description = "Mission upload completed.")
  MISSION_UPLOAD_COMPLETED,

  @Schema(description = "Mission upload failed.")
  MISSION_UPLOAD_FAILED,

  @Schema(description = "Mission download started.")
  MISSION_DOWNLOAD_STARTED,

  @Schema(description = "Mission download completed.")
  MISSION_DOWNLOAD_COMPLETED,

  @Schema(description = "Mission download failed.")
  MISSION_DOWNLOAD_FAILED
}