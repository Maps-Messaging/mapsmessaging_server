/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.Map;
import lombok.Getter;

@Getter
public class MountStatusPacket extends MavlinkPacket {

  private static final double CENTIDEGREES_PER_DEGREE = 100.0d;

  private final double pitchDegrees;
  private final double rollDegrees;
  private final double bearingDegrees;
  private final Integer mountMode;
  private final boolean valid;

  public MountStatusPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();
    pitchDegrees = getDouble(fields, "pointing_a") / CENTIDEGREES_PER_DEGREE;
    rollDegrees = getDouble(fields, "pointing_b") / CENTIDEGREES_PER_DEGREE;
    bearingDegrees = normaliseBearing(
        getDouble(fields, "pointing_c") / CENTIDEGREES_PER_DEGREE);
    int rawMountMode = getInt(fields, "mount_mode");
    mountMode = rawMountMode < 0 ? null : rawMountMode;
    valid = frame.isValid();
  }

  private static double normaliseBearing(double degrees) {
    if (!Double.isFinite(degrees)) {
      return Double.NaN;
    }
    double normalised = degrees % 360.0d;
    return normalised < 0.0d ? normalised + 360.0d : normalised;
  }
}
