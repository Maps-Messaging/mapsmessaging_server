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

package io.mapsmessaging.state.mavlink.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record UxvNavigationPlan(
    List<UxvModelCommandSet> missionPhase,
    List<UxvModelCommandSet> postMissionUploadPhase,
    Duration duration,
    UxvModelCommandSet terminalAction) {

  public UxvNavigationPlan {
    missionPhase = copyRequiredPhase(missionPhase, "missionPhase");
    postMissionUploadPhase = copyRequiredPhase(postMissionUploadPhase, "postMissionUploadPhase");
    duration = duration == null ? Duration.ZERO : duration;
    terminalAction = Objects.requireNonNull(terminalAction, "terminalAction must not be null");

    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration must not be negative");
    }
  }

  public boolean hasTimeout() {
    return !duration.isZero();
  }

  private static List<UxvModelCommandSet> copyRequiredPhase(List<UxvModelCommandSet> phase, String name) {
    Objects.requireNonNull(phase, name + " must not be null");

    if (phase.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be empty");
    }

    return List.copyOf(phase);
  }
}