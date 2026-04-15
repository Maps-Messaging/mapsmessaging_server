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

import io.mapsmessaging.network.protocol.impl.mavlink.packet.GpsRawIntPacket;
import io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.FixInfo;

import java.time.Instant;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.GPS_RAW_INT;

public class GpsRawIntListener implements Listener {

  public static final int LISTENER_ID = GPS_RAW_INT;

  private final TwinManager twinManager;

  public GpsRawIntListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof GpsRawIntPacket packet)) {
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

      FixInfo fixInfo = drone.getFixInfo();
      if (fixInfo == null) {
        fixInfo = new FixInfo();
      }

      fixInfo.setFixType(packet.getFixTypeName());

      if (packet.getSatellitesVisible() >= 0) {
        fixInfo.setSatelliteCount(packet.getSatellitesVisible());
      }

      if (!Double.isNaN(packet.getHdop())) {
        fixInfo.setHdop(packet.getHdop());
      }

      if (!Double.isNaN(packet.getVdop())) {
        fixInfo.setVdop(packet.getVdop());
      }

      drone.setFixInfo(fixInfo);
      drone.setGpsValid(packet.hasValidFix());
      drone.setNavigationUpdatedAt(now);

    }, context);
  }
}