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

import io.mapsmessaging.network.protocol.impl.mavlink.packet.HomePositionPacket;
import io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;

import java.time.Instant;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.HOME_POSITION;

/**
 * Listener for HOME_POSITION.
 */
public class HomePositionListener implements Listener {

  public static final int LISTENER_ID = HOME_POSITION;

  private final TwinManager twinManager;

  public HomePositionListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    if (!(pkt instanceof HomePositionPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    final double latitude = packet.getLatitude();
    final double longitude = packet.getLongitude();
    final double altitudeMeters = packet.getAltitudeMeters();

    if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
      return;
    }

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin drone = (DroneTwin) twin;

      drone.setHomePosition(new GeoPosition(
          latitude,
          longitude,
          altitudeMeters,
          null
      ));

      drone.setNavigationUpdatedAt(now);

    }, context);
  }
}