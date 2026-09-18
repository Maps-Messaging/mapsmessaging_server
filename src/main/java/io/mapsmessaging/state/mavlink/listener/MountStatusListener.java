/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.state.mavlink.listener;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.MOUNT_STATUS;

import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinUpdateContext;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.mavlink.packet.MavlinkPacket;
import io.mapsmessaging.state.mavlink.packet.MountStatusPacket;
import java.time.Instant;

public class MountStatusListener implements Listener {

  public static final int LISTENER_ID = MOUNT_STATUS;

  private final TwinManager twinManager;

  public MountStatusListener(TwinManager twinManager) {
    this.twinManager = twinManager;
  }

  @Override
  public void handle(String twinId, MavlinkPacket pkt, TwinUpdateContext context) {
    if (!(pkt instanceof MountStatusPacket packet) || !packet.isValid()) {
      return;
    }

    Instant now =
        context != null && context.getReceivedTime() != null
            ? context.getReceivedTime()
            : Instant.now();

    twinManager.updateTwin(
        twinId,
        twin -> {
          DroneTwin drone = (DroneTwin) twin;
          drone.setCameraPitchDegrees(finiteOrNull(packet.getPitchDegrees()));
          drone.setCameraRollDegrees(finiteOrNull(packet.getRollDegrees()));
          drone.setCameraBearingDegrees(finiteOrNull(packet.getBearingDegrees()));
          drone.setCameraMountMode(packet.getMountMode());
          drone.setCameraOrientationUpdatedAt(now);
        },
        context);
  }

  private static Double finiteOrNull(double value) {
    return Double.isFinite(value) ? value : null;
  }
}
