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
import lombok.Getter;

@Getter
public class DroneTwinReadinessEvaluation {

  private final Instant evaluatedAt;
  private final Set<DroneTwinMissingState> missingStates;
  private final Set<DroneTwinMissingState> degradedStates;
  private final Set<DroneTwinMissingState> blockingStates;

  public DroneTwinReadinessEvaluation(Instant evaluatedAt) {
    this.evaluatedAt = evaluatedAt;
    this.missingStates = EnumSet.noneOf(DroneTwinMissingState.class);
    this.degradedStates = EnumSet.noneOf(DroneTwinMissingState.class);
    this.blockingStates = EnumSet.noneOf(DroneTwinMissingState.class);
  }

  public void missing(DroneTwinMissingState missingState) {
    missingStates.add(missingState);
  }

  public void degraded(DroneTwinMissingState missingState) {
    missingStates.add(missingState);
    degradedStates.add(missingState);
  }

  public void blocking(DroneTwinMissingState missingState) {
    missingStates.add(missingState);
    blockingStates.add(missingState);
  }
}