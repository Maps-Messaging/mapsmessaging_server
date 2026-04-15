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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * GNSS fix quality and accuracy information.
 */
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixInfo {

  /**
   * Example values:
   * NO_FIX, 2D, 3D, RTK_FLOAT, RTK_FIXED
   */
  private String fixType;

  private Integer satelliteCount;

  private Double hdop;
  private Double vdop;

  private Double horizontalAccuracyMeters;
  private Double verticalAccuracyMeters;
}