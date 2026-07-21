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

package io.mapsmessaging.state.config;

import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class DroneInfoDTO {

  @Schema(
      description = "Unique drone id"
  )
  private String name;

  @Schema(
      description = "UUID of drone"
  )
  private UUID uuid;

  @Schema(
      description = "Configured UxV model name used to resolve the command model implementation."
  )
  private String modelName;

  @Schema(description = "Message encoding used when communicating with this drone.")
  private MessageEncodingEnum messageEncoding = MessageEncodingEnum.JSON;

  @Schema(
      description = "Total battery capacity in amp-hours."
  )
  private double batteryCapacityAh = 0.0;

  @Schema(
      description = "Total battery capacity in hours."
  )
  private double batteryCapacityHours = 0.0;

  @Schema(
      description = "Drone description"
  )
  private Map<String, Object> description;

  @Schema(
      description = "Task capabilities supported by this known MAVLink source."
  )
  private TaskCapabilities capabilities = new TaskCapabilities();

  @Schema(description = "Action performed when the current task is cancelled.")
  private StopActionEnum cancelAction = StopActionEnum.STOP;

  @Schema(description = "Action performed when a mission completes normally.")
  private StopActionEnum missionEndAction = StopActionEnum.STOP;

  @Schema(description = "Action performed when a mission exceeds its configured duration.")
  private StopActionEnum missionTimeoutAction = StopActionEnum.STOP;

  /**
   * Backwards-compatible alias for the former stopAction property.
   *
   * @return the configured cancellation action
   */
  @Deprecated
  public StopActionEnum getStopAction() {
    return cancelAction;
  }

  /**
   * Backwards-compatible alias for the former stopAction property.
   *
   * @param stopAction action to perform when the current task is cancelled
   */
  @Deprecated
  public void setStopAction(StopActionEnum stopAction) {
    cancelAction = stopAction;
  }
}