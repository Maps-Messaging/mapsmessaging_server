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

package io.mapsmessaging.state.mavlink.bootstrap.checks;

import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinMissingState;
import io.mapsmessaging.state.mavlink.bootstrap.DroneTwinReadinessEvaluation;

public class AutopilotReadinessCheck implements DroneTwinReadinessCheck {

  @Override
  public void evaluate(
      DroneTwin droneTwin,
      DroneTwinReadinessEvaluation evaluation
  ) {
    AutopilotState autopilotState = droneTwin.getAutopilotState();

    if (autopilotState == null || autopilotState.getAutopilotType() == null) {
      evaluation.degraded(DroneTwinMissingState.MISSING_AUTOPILOT_TYPE);
      return;
    }

    if (autopilotState.getUid() == null
        && autopilotState.getFlightSoftwareVersion() == null
        && autopilotState.getCapabilities() == null) {
      evaluation.degraded(DroneTwinMissingState.MISSING_AUTOPILOT_VERSION);
    }
  }
}