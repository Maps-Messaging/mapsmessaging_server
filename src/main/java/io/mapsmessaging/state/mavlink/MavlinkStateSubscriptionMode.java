/*
 *
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

package io.mapsmessaging.state.mavlink;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines which MAVLink-derived state streams should be maintained for a subscription.
 *
 * <p>The mode controls whether incoming MAVLink state updates are applied to the live
 * entity twin, the mission/plan state, or both.</p>
 */
@Schema(
    description = "Defines which MAVLink-derived state streams are maintained for a subscription."
)
public enum MavlinkStateSubscriptionMode {

  /**
   * Maintain only the live entity twin state.
   */
  @Schema(
      description = "Maintain only the live entity twin state."
  )
  TWIN,

  /**
   * Maintain only the MAVLink mission or plan state.
   */
  @Schema(
      description = "Maintain only the MAVLink mission or plan state."
  )
  PLAN,

  /**
   * Maintain both the live entity twin state and the MAVLink mission or plan state.
   */
  @Schema(
      description = "Maintain both the live entity twin state and the MAVLink mission or plan state."
  )
  BOTH;

  /**
   * Indicates whether this mode includes live entity twin state handling.
   *
   * @return {@code true} if twin state should be maintained
   */
  public boolean includesTwinState() {
    return this == TWIN || this == BOTH;
  }

  /**
   * Indicates whether this mode includes MAVLink mission or plan state handling.
   *
   * @return {@code true} if plan state should be maintained
   */
  public boolean includesPlanState() {
    return this == PLAN || this == BOTH;
  }
}