package io.mapsmessaging.network.protocol.impl.mavlink.listener;

import io.mapsmessaging.network.protocol.impl.mavlink.packet.GlobalPositionPacket;
import io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.state.drone.model.VelocityVector;

import java.time.Instant;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.GLOBAL_POSITION_INT;

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
  public void handle(String twinId,
                     MavlinkPacket pkt,
                     TwinUpdateContext context) {

    // Defensive type check (dispatcher should guarantee this)
    if (!(pkt instanceof GlobalPositionPacket packet)) {
      return;
    }

    if (!packet.isValid()) {
      return;
    }

    Instant now = (context != null && context.getReceivedTime() != null)
        ? context.getReceivedTime()
        : Instant.now();

    final double vx = packet.getVx();
    final double vy = packet.getVy();
    final double vz = packet.getVz();
    twinManager.updateTwin(twinId, twin -> {

      DroneTwin drone = (DroneTwin) twin;

      // Position
      drone.setGeoPosition(new GeoPosition(
          packet.getLatitude(),
          packet.getLongitude(),
          packet.getAltitudeMeters(),
          null
      ));

      // Velocity (NED)
      drone.setVelocityVector(new VelocityVector(vx, vy, vz));

      // Derived navigation (guard NaN)
      drone.setHeadingDegrees(packet.getHeadingDegrees());

      if (!Double.isNaN(vx) && !Double.isNaN(vy)) {
        drone.setGroundSpeedMetersPerSecond(Math.sqrt(vx * vx + vy * vy));
      }

      if (!Double.isNaN(vz)) {
        drone.setVerticalSpeedMetersPerSecond(-vz); // NED → climb
      }

      // Freshness
      drone.setNavigationUpdatedAt(now);

    }, context);
  }
}