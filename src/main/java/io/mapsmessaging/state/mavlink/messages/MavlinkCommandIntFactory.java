/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License.
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

package io.mapsmessaging.state.mavlink.messages;

import io.mapsmessaging.state.drone.model.GeoPosition;

import java.time.Duration;

public final class MavlinkCommandIntFactory {
  public static final int MAV_CMD_DO_ORBIT = 34;
  public static final int MAV_FRAME_GLOBAL = 0;

  public static final float ORBIT_YAW_BEHAVIOUR_HOLD_FRONT_TO_CIRCLE_CENTER = 5.0f;

  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_LOITER_TIME = 19;
  public static final int MAV_CMD_DO_REPOSITION = 192;

  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;

  private MavlinkCommandIntFactory() {
  }

  public static MavlinkCommandInt reposition(int targetSystem, int targetComponent, GeoPosition position, float yawDegrees, int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, MAV_CMD_DO_REPOSITION, sequence);
    commandInt.setParam1(-1.0f);
    commandInt.setParam2(1.0f);
    commandInt.setParam3(0.0f);
    commandInt.setParam4(toYawRadians(yawDegrees));
    setPosition(commandInt, position);
    return commandInt;
  }

  public static MavlinkCommandInt orbit(int targetSystem, int targetComponent, GeoPosition position, double radiusMeters, int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, MAV_FRAME_GLOBAL, MAV_CMD_DO_ORBIT, sequence);
    commandInt.setParam1((float) radiusMeters);
    commandInt.setParam2(Float.NaN);
    commandInt.setParam3(ORBIT_YAW_BEHAVIOUR_HOLD_FRONT_TO_CIRCLE_CENTER);
    commandInt.setParam4(Float.NaN);
    setPosition(commandInt, position);
    return commandInt;
  }

  public static MavlinkCommandInt loiterUnlimited(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double radiusMeters,
      float yawDegrees,
      int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(
        targetSystem,
        targetComponent,
        MAV_FRAME_GLOBAL_RELATIVE_ALT_INT,
        MAV_CMD_NAV_LOITER_UNLIM,
        sequence);

    commandInt.setParam1(0.0f);
    commandInt.setParam2(0.0f);
    commandInt.setParam3((float) radiusMeters);
    commandInt.setParam4(toYawRadians(yawDegrees));
    setPosition(commandInt, position);

    return commandInt;
  }



  public static MavlinkCommandInt loiterTime(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double radiusMeters,
      Duration duration,
      float yawDegrees,
      int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(
        targetSystem,
        targetComponent,
        MAV_FRAME_GLOBAL_RELATIVE_ALT_INT,
        MAV_CMD_NAV_LOITER_TIME,
        sequence);

    commandInt.setParam1(toSeconds(duration));
    commandInt.setParam2(0.0f);
    commandInt.setParam3((float) radiusMeters);
    commandInt.setParam4(toYawRadians(yawDegrees));
    setPosition(commandInt, position);

    return commandInt;
  }

  private static MavlinkCommandInt baseCommandInt(
      int targetSystem,
      int targetComponent,
      int frame,
      int command,
      int sequence) {
    MavlinkCommandInt commandInt = new MavlinkCommandInt();
    commandInt.setTargetSystem(targetSystem);
    commandInt.setTargetComponent(targetComponent);
    commandInt.setFrame(frame);
    commandInt.setCommand(command);
    commandInt.setSequence(sequence);
    commandInt.setCurrent(0);
    commandInt.setAutocontinue(0);
    return commandInt;
  }

  private static void setPosition(MavlinkCommandInt commandInt, GeoPosition position) {
    if (position == null) {
      throw new IllegalArgumentException("Position must not be null");
    }

    commandInt.setLatitude(toScaledCoordinate(position.getLatitude()));
    commandInt.setLongitude(toScaledCoordinate(position.getLongitude()));

    Double altitudeMeters = position.getPreferredAltitudeMeters();
    if (altitudeMeters != null) {
      commandInt.setAltitude(altitudeMeters.floatValue());
    }
  }

  private static int toScaledCoordinate(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Coordinate must not be null");
    }
    return (int) Math.round(value * 10_000_000.0d);
  }

  private static float toSeconds(Duration duration) {
    if (duration == null || duration.isZero() || duration.isNegative()) {
      return 0.0f;
    }
    return duration.toSeconds();
  }

  private static float toYawRadians(float yawDegrees) {
    if (Float.isNaN(yawDegrees)) {
      return Float.NaN;
    }

    if (!Float.isFinite(yawDegrees)) {
      return 0.0f;
    }

    float normalisedYawDegrees = yawDegrees % 360.0f;
    if (normalisedYawDegrees < 0.0f) {
      normalisedYawDegrees += 360.0f;
    }

    return (float) Math.toRadians(normalisedYawDegrees);
  }
}