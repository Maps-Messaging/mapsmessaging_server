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
    description = "Lifecycle state of a plan."
)
public enum PlanState {

  @Schema(description = "Plan has been created but is not yet active.")
  CREATED,

  @Schema(description = "Plan has been received from an external source.")
  RECEIVED,

  @Schema(description = "Plan has passed validation.")
  VALIDATED,

  @Schema(description = "Plan failed validation or was rejected.")
  REJECTED,

  @Schema(description = "Plan is active and may be executed.")
  ACTIVE,

  @Schema(description = "Plan execution has been paused.")
  PAUSED,

  @Schema(description = "Plan execution has completed.")
  COMPLETED,

  @Schema(description = "Plan execution was aborted.")
  ABORTED,

  @Schema(description = "Plan has been deleted.")
  DELETED
}