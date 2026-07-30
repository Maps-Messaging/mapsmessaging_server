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

package io.mapsmessaging.state.mavlink.messages;

import io.mapsmessaging.state.drone.model.GeoPosition;
import java.time.Duration;

public final class MavlinkCommandIntFactory {
  public static final int MAV_CMD_DO_ORBIT = 34;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_LOITER_TIME = 19;
  public static final int MAV_CMD_DO_REPOSITION = 192;

  public static final int MAV_FRAME_GLOBAL = 0;
  public static final int MAV_FRAME_GLOBAL_INT = 5;
  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;
  public static final int MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11;

  public static final float ORBIT_YAW_BEHAVIOUR_HOLD_FRONT_TO_CIRCLE_CENTER = 5.0f;

  private MavlinkCommandIntFactory() {
  }

  public static MavlinkCommandInt reposition(int targetSystem, int targetComponent, GeoPosition position, int sequence) {
    MavlinkCommandInt commandInt = repositionCommand(targetSystem, targetComponent, resolvePositionFrame(position), sequence);
    setPosition(commandInt, position);
    return commandInt;
  }

  public static MavlinkCommandInt repositionRelativeAltitude(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double relativeAltitudeMeters,
      int sequence) {
    MavlinkCommandInt commandInt = repositionCommand(targetSystem, targetComponent, MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, sequence);
    setPosition(commandInt, position, relativeAltitudeMeters);
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
    MavlinkCommandInt commandInt = loiterUnlimitedCommand(targetSystem, targetComponent, resolvePositionFrame(position), radiusMeters, yawDegrees, sequence);
    setPosition(commandInt, position);
    return commandInt;
  }

  public static MavlinkCommandInt loiterUnlimitedRelativeAltitude(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double relativeAltitudeMeters,
      double radiusMeters,
      float yawDegrees,
      int sequence) {
    MavlinkCommandInt commandInt = loiterUnlimitedCommand(targetSystem, targetComponent, MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, radiusMeters, yawDegrees, sequence);
    setPosition(commandInt, position, relativeAltitudeMeters);
    return commandInt;
  }

  public static MavlinkCommandInt stop(int targetSystem, int targetComponent, int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, MAV_CMD_NAV_LOITER_UNLIM, sequence);
    commandInt.setParam1(0.0f);
    commandInt.setParam2(0.0f);
    commandInt.setParam3(0.0f);
    commandInt.setParam4(Float.NaN);
    commandInt.setLatitude(0);
    commandInt.setLongitude(0);
    commandInt.setAltitude(0.0f);
    return commandInt;
  }

  public static MavlinkCommandInt loiterTime(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double radiusMeters,
      Duration duration,
      int sequence) {
    MavlinkCommandInt commandInt = loiterTimeCommand(targetSystem, targetComponent, resolvePositionFrame(position), radiusMeters, duration, sequence);
    setPosition(commandInt, position);
    return commandInt;
  }

  public static MavlinkCommandInt loiterTimeRelativeAltitude(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double relativeAltitudeMeters,
      double radiusMeters,
      Duration duration,
      int sequence) {
    MavlinkCommandInt commandInt = loiterTimeCommand(targetSystem, targetComponent, MAV_FRAME_GLOBAL_RELATIVE_ALT_INT, radiusMeters, duration, sequence);
    setPosition(commandInt, position, relativeAltitudeMeters);
    return commandInt;
  }

  private static MavlinkCommandInt repositionCommand(int targetSystem, int targetComponent, int frame, int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, frame, MAV_CMD_DO_REPOSITION, sequence);
    commandInt.setParam1(-1.0f);
    commandInt.setParam2(1.0f);
    commandInt.setParam3(Float.NaN);
    commandInt.setParam4(Float.NaN);
    return commandInt;
  }

  private static MavlinkCommandInt loiterUnlimitedCommand(
      int targetSystem,
      int targetComponent,
      int frame,
      double radiusMeters,
      float yawDegrees,
      int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, frame, MAV_CMD_NAV_LOITER_UNLIM, sequence);
    commandInt.setParam1(0.0f);
    commandInt.setParam2(0.0f);
    commandInt.setParam3((float) radiusMeters);
    commandInt.setParam4(normaliseYawDegrees(yawDegrees));
    return commandInt;
  }

  private static MavlinkCommandInt loiterTimeCommand(
      int targetSystem,
      int targetComponent,
      int frame,
      double radiusMeters,
      Duration duration,
      int sequence) {
    MavlinkCommandInt commandInt = baseCommandInt(targetSystem, targetComponent, frame, MAV_CMD_NAV_LOITER_TIME, sequence);
    commandInt.setParam1(MavlinkDuration.toSeconds(duration, "duration"));
    commandInt.setParam2(0.0f);
    commandInt.setParam3((float) radiusMeters);
    commandInt.setParam4(Float.NaN);
    return commandInt;
  }

  private static MavlinkCommandInt baseCommandInt(int targetSystem, int targetComponent, int frame, int command, int sequence) {
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

  private static int resolvePositionFrame(GeoPosition position) {
    validatePosition(position);
    if (position.getAltitudeMslMeters() != null) {
      return MAV_FRAME_GLOBAL_INT;
    }
    if (position.getAltitudeAglMeters() != null) {
      return MAV_FRAME_GLOBAL_TERRAIN_ALT_INT;
    }
    return MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
  }

  private static void setPosition(MavlinkCommandInt commandInt, GeoPosition position) {
    validatePosition(position);
    commandInt.setLatitude(toScaledCoordinate(position.getLatitude()));
    commandInt.setLongitude(toScaledCoordinate(position.getLongitude()));
    commandInt.setAltitude(resolveAltitude(position));
  }

  private static void setPosition(MavlinkCommandInt commandInt, GeoPosition position, double relativeAltitudeMeters) {
    validatePosition(position);
    requireFinite(relativeAltitudeMeters, "relativeAltitudeMeters");
    commandInt.setLatitude(toScaledCoordinate(position.getLatitude()));
    commandInt.setLongitude(toScaledCoordinate(position.getLongitude()));
    commandInt.setAltitude((float) relativeAltitudeMeters);
  }

  private static float resolveAltitude(GeoPosition position) {
    Double altitudeMeters = position.getAltitudeMslMeters();
    if (altitudeMeters == null) {
      altitudeMeters = position.getAltitudeAglMeters();
    }
    if (altitudeMeters == null) {
      return 0.0f;
    }
    requireFinite(altitudeMeters, "altitudeMeters");
    return altitudeMeters.floatValue();
  }

  private static void validatePosition(GeoPosition position) {
    if (position == null) {
      throw new IllegalArgumentException("Position must not be null");
    }
    validateCoordinate(position.getLatitude(), -90.0d, 90.0d, "Latitude");
    validateCoordinate(position.getLongitude(), -180.0d, 180.0d, "Longitude");
    validateAltitude(position.getAltitudeMslMeters(), "MSL altitude");
    validateAltitude(position.getAltitudeAglMeters(), "AGL altitude");
  }

  private static void validateCoordinate(Double value, double minimum, double maximum, String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " must not be null");
    }
    if (!Double.isFinite(value) || value < minimum || value > maximum) {
      throw new IllegalArgumentException(name + " must be finite and between " + minimum + " and " + maximum + " degrees");
    }
  }

  private static void validateAltitude(Double value, String name) {
    if (value != null) {
      requireFinite(value, name);
    }
  }

  private static void requireFinite(double value, String name) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  private static int toScaledCoordinate(Double value) {
    return (int) Math.round(value * 10_000_000.0d);
  }

  private static float normaliseYawDegrees(float yawDegrees) {
    if (Float.isNaN(yawDegrees)) {
      return Float.NaN;
    }
    if (!Float.isFinite(yawDegrees)) {
      throw new IllegalArgumentException("yawDegrees must be finite or NaN");
    }
    float normalisedYawDegrees = yawDegrees % 360.0f;
    if (normalisedYawDegrees < 0.0f) {
      normalisedYawDegrees += 360.0f;
    }
    return normalisedYawDegrees;
  }
}
