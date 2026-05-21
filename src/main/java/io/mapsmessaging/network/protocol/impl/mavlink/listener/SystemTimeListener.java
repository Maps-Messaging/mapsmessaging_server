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

package io.mapsmessaging.network.protocol.impl.mavlink.listener;

import io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.network.protocol.impl.mavlink.packet.SystemTimePacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.TimeState;

import java.time.Instant;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.SYSTEM_TIME;

public class SystemTimeListener implements Listener {

  public static final int LISTENER_ID = SYSTEM_TIME;

  private final TwinManager twinManager;

  public SystemTimeListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof SystemTimePacket packet)) {
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

      TimeState timeState = drone.getTimeState();
      if (timeState == null) {
        timeState = new TimeState();
      }

      if (packet.hasValidUnixTime()) {
        timeState.setGpsTimeEpochMillis(packet.getUnixTimeEpochMillis());
        timeState.setGpsTimeValid(true);
      }

      if (packet.hasValidBootTime()) {
        timeState.setSystemTimeEpochMillis(packet.getBootTimeMillis());
      }

      if (packet.hasValidUnixTime()) {
        timeState.setTimeOffsetMillis(
            (double) (now.toEpochMilli() - packet.getUnixTimeEpochMillis())
        );
      }

      drone.setTimeState(timeState);
      drone.setIdentityUpdatedAt(now);

    }, context);
  }
}