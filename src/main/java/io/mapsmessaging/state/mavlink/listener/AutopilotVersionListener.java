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

package io.mapsmessaging.state.mavlink.listener;

import io.mapsmessaging.state.mavlink.packet.AutopilotVersionPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.GenericAutopilotState;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.AUTOPILOT_VERSION;

/**
 * Listener for AUTOPILOT_VERSION.
 */
public class AutopilotVersionListener implements Listener {

  public static final int LISTENER_ID = AUTOPILOT_VERSION;

  private final TwinManager twinManager;

  public AutopilotVersionListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof AutopilotVersionPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin droneTwin = (DroneTwin) twin;

      AutopilotState autopilotState = droneTwin.getAutopilotState();
      if (autopilotState == null) {
        autopilotState = new GenericAutopilotState();
      }

      MavlinkAutopilotSupport.populateAutopilotVersionFields(
          autopilotState,
          packet.getUid(),
          packet.getFlightSoftwareVersion(),
          packet.getMiddlewareSoftwareVersion(),
          packet.getOsSoftwareVersion(),
          packet.getCapabilities()
      );

      droneTwin.setAutopilotState(autopilotState);

    }, context);
  }
}