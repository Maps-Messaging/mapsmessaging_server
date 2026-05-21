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
    name = "PlanCommand",
    description = "Command associated with a plan item."
)
public class PlanCommandDTO {

  @Schema(
      description = "Protocol-neutral command identifier. For MAVLink-derived plans this may contain the MAVLink command id.",
      example = "16"
  )
  private int commandId;

  @Schema(
      description = "Human-readable command name.",
      example = "NAV_WAYPOINT"
  )
  private String commandName;

  @Schema(
      description = "Command category, such as navigation, condition, action, or custom.",
      example = "NAVIGATION"
  )
  private String commandCategory;

  @Schema(
      description = "Command parameters keyed by name. Raw protocol values may be preserved here when no semantic name is available."
  )
  private Map<String, Object> parameters = new LinkedHashMap<>();

  @Schema(
      description = "Additional command metadata, including source protocol details."
  )
  private Map<String, Object> metadata = new LinkedHashMap<>();
}