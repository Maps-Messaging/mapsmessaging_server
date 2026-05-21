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

package io.mapsmessaging.state.drone.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@Setter
@Schema(description = "Time synchronization and reference state for the drone or vehicle.")
public class TimeState {

  @Schema(
      description = "GPS time expressed as epoch milliseconds.",
      example = "1713590400000",
      nullable = true
  )
  private Long gpsTimeEpochMillis;

  @Schema(
      description = "System time expressed as epoch milliseconds.",
      example = "1713590400100",
      nullable = true
  )
  private Long systemTimeEpochMillis;

  @Schema(
      description = "Indicates whether the GPS time is valid.",
      example = "true",
      nullable = true
  )
  private Boolean gpsTimeValid;

  @Schema(
      description = "Difference between system time and GPS time in milliseconds.",
      example = "-100.0",
      nullable = true
  )
  private Double timeOffsetMillis;
}