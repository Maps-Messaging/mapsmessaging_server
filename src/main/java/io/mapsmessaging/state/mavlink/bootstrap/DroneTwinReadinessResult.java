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

package io.mapsmessaging.state.mavlink.bootstrap;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DroneTwinReadinessResult {

  private String twinId;
  private DroneTwinReadinessState readinessState;
  private boolean registrationReady;
  private boolean commandReady;
  private Set<DroneTwinMissingState> missingStates;
  private Set<DroneTwinMissingState> degradedStates;
  private Set<DroneTwinMissingState> blockingStates;
  private Instant evaluatedAt;

  public DroneTwinReadinessResult(String twinId) {
    this.twinId = twinId;
    this.readinessState = DroneTwinReadinessState.UNKNOWN;
    this.missingStates = EnumSet.noneOf(DroneTwinMissingState.class);
    this.degradedStates = EnumSet.noneOf(DroneTwinMissingState.class);
    this.blockingStates = EnumSet.noneOf(DroneTwinMissingState.class);
    this.evaluatedAt = Instant.now();
  }

  public boolean hasMissingState(DroneTwinMissingState missingState) {
    return missingStates != null && missingStates.contains(missingState);
  }
}