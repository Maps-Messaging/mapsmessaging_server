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
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MissionCurrentPacket;

import java.time.Instant;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.MISSION_CURRENT;

public class MissionCurrentListener implements Listener {

  public static final int LISTENER_ID = MISSION_CURRENT;

  private final TwinManager twinManager;

  public MissionCurrentListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId, MavlinkPacket pkt, TwinUpdateContext context) {

    if (!(pkt instanceof MissionCurrentPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    twinManager.updateTwin(twinId, twin -> {

      DroneTwin drone = (DroneTwin) twin;

      if (packet.getSequence() >= 0) {
        drone.setCurrentMissionSequence(packet.getSequence());
        drone.setCurrentMissionUpdatedAt(now);
        drone.setMissionState(missionStateName(packet.getMissionState()));
      }
      if (packet.getTotal() >= 0) {
        drone.setCurrentMissionTotal(packet.getTotal());
      }
      if (packet.getMissionState() >= 0) {
        drone.setCurrentMissionStateCode(packet.getMissionState());
      }
      if (packet.getMissionId() >= 0L) {
        drone.setCurrentMissionId(packet.getMissionId());
      }

      drone.setOperationalUpdatedAt(now);

    }, context);
  }

  private static String missionStateName(int missionState) {
    return switch (missionState) {
      case 0 -> "MISSION_UNKNOWN";
      case 1 -> "MISSION_NO_MISSION";
      case 2 -> "MISSION_NOT_STARTED";
      case 3 -> "MISSION_ACTIVE";
      case 4 -> "MISSION_PAUSED";
      case 5 -> "MISSION_COMPLETE";
      case -1 -> "MISSION_ACTIVE";
      default -> "MISSION_STATE_" + missionState;
    };
  }
}