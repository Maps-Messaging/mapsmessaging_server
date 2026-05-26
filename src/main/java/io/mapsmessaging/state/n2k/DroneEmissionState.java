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

package io.mapsmessaging.state.n2k;

import io.mapsmessaging.state.n2k.handler.PgnEmissionState;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
public class DroneEmissionState {


  private final Map<Integer, PgnEmissionState> pgnStates = new ConcurrentHashMap<>();

  public PgnEmissionState getOrCreateState(int pgn) {
    return pgnStates.computeIfAbsent(pgn, ignored -> new PgnEmissionState());
  }

  private long lastPositionEmitAt;
  private long lastExtendedEmitAt;
  private long lastStaticEmitAt;

  private boolean positionReportSent;
  private boolean extendedReportSent;
  private boolean staticReportSent;

  private Double lastLatitude;
  private Double lastLongitude;
  private Double lastHeadingDegrees;
  private Double lastCourseOverGroundDegrees;
  private Double lastGroundSpeedMetersPerSecond;

  private String lastStaticSignature;
}