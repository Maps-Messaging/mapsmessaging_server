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

import io.mapsmessaging.dto.rest.config.protocol.impl.VehicleClass;
import io.mapsmessaging.state.mavlink.packet.HeartbeatPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.LinkState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;

import java.time.Instant;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.HEARTBEAT;

/**
 * Listener for HEARTBEAT.
 */
public class HeartbeatListener implements Listener {

  public static final int LISTENER_ID = HEARTBEAT;

  private final TwinManager twinManager;

  public HeartbeatListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof HeartbeatPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin droneTwin = (DroneTwin) twin;

      droneTwin.setVehicleClass(VehicleClass.fromMavType(packet.getVehicleClass()));
      droneTwin.setArmed(packet.isArmed());
      droneTwin.setFlightMode(packet.getFlightMode());

      AutopilotState autopilotState = MavlinkAutopilotSupport.resolveAutopilotState(
          droneTwin.getAutopilotState(),
          packet.getAutopilot()
      );

      MavlinkAutopilotSupport.populateHeartbeatFields(
          autopilotState,
          packet.getAutopilot(),
          packet.getBaseMode(),
          packet.getCustomMode(),
          packet.getSystemStatus(),
          packet.getMavlinkVersion()
      );

      droneTwin.setAutopilotState(autopilotState);
      droneTwin.setFlightMode(autopilotState.getFlightMode());

      LinkState linkState = droneTwin.getLinkState();
      if (linkState == null) {
        linkState = new LinkState();
      }

      linkState.setConnected(true);
      linkState.setState("CONNECTED");

      droneTwin.setLinkState(linkState);
      droneTwin.setConnectivityUpdatedAt(now);
      droneTwin.setOperationalUpdatedAt(now);

    }, context);
  }
}