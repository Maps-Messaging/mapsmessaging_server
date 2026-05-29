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
import java.util.EnumMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MavlinkBootstrapState {

  private String twinId;
  private DroneTwinReadinessState readinessState;
  private boolean completed;
  private boolean failed;
  private Instant createdAt;
  private Instant updatedAt;
  private Map<DroneTwinMissingState, MavlinkBootstrapRequestTracker> requestTrackers;

  public MavlinkBootstrapState(String twinId) {
    this.twinId = twinId;
    this.readinessState = DroneTwinReadinessState.UNKNOWN;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
    this.requestTrackers = new EnumMap<>(DroneTwinMissingState.class);
  }

  public MavlinkBootstrapRequestTracker getOrCreateTracker(DroneTwinMissingState missingState) {
    return requestTrackers.computeIfAbsent(
        missingState,
        MavlinkBootstrapRequestTracker::new
    );
  }
}