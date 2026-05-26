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

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.CommandAckPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;

import java.time.Instant;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.COMMAND_ACK;

/**
 * Listener for COMMAND_ACK.
 */
public class CommandAckListener implements Listener {

  public static final int LISTENER_ID = COMMAND_ACK;

  private final TwinManager twinManager;

  public CommandAckListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof CommandAckPacket packet)) {
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

      droneTwin.setLastAcknowledgedCommand(packet.getCommand());
      droneTwin.setLastCommandAcknowledgement(packet.getResultName());
      droneTwin.setLastCommandAcknowledgementAt(now);
      droneTwin.setOperationalUpdatedAt(now);

    }, context);
  }
}