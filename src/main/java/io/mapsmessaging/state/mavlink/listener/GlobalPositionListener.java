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

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.GLOBAL_POSITION_INT;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.VelocityVector;
import io.mapsmessaging.state.mavlink.packet.GlobalPositionPacket;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import java.time.Instant;

/**
 * Listener for GLOBAL_POSITION_INT.
 */
public class GlobalPositionListener implements Listener {

  public static final int LISTENER_ID = GLOBAL_POSITION_INT;

  private final TwinManager twinManager;

  public GlobalPositionListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId, MavlinkPacket pkt, TwinUpdateContext context) {
    if (!(pkt instanceof GlobalPositionPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    Double latitude = finiteOrNull(packet.getLatitude());
    Double longitude = finiteOrNull(packet.getLongitude());
    Double altitude = finiteOrNull(packet.getAltitudeMeters());
    Double north = finiteOrNull(packet.getVx());
    Double east = finiteOrNull(packet.getVy());
    Double down = finiteOrNull(packet.getVz());
    Double heading = finiteOrNull(packet.getHeadingDegrees());

    twinManager.updateTwin(twinId, twin -> {
      DroneTwin drone = (DroneTwin) twin;

      drone.setGeoPosition(new GeoPosition(latitude, longitude, altitude, null));
      drone.setVelocityVector(new VelocityVector(north, east, down));
      drone.setHeadingDegrees(heading);
      drone.setGroundSpeedMetersPerSecond(
          north != null && east != null ? Math.sqrt(north * north + east * east) : null
      );
      drone.setVerticalSpeedMetersPerSecond(down != null ? -down : null);
      drone.setNavigationUpdatedAt(now);
    }, context);
  }

  private static Double finiteOrNull(double value) {
    return Double.isFinite(value) ? value : null;
  }
}
