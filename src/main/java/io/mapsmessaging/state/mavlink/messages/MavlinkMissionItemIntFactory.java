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

public final class MavlinkMissionItemIntFactory {

  public static final int MAV_FRAME_MISSION = 2;
  public static final int MAV_FRAME_GLOBAL_INT = 5;
  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;
  public static final int MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11;

  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_LOITER_TIME = 19;
  public static final int MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;
  public static final int MAV_CMD_DO_JUMP = 177;

  public static final int MAV_MISSION_TYPE_MISSION = 0;

  private static final float DEFAULT_WAYPOINT_ACCEPTANCE_RADIUS_METERS = 2.0f;

  private MavlinkMissionItemIntFactory() {
  }

  public static MavlinkMissionItemInt waypoint(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      GeoPosition position) {
    return waypoint(
        targetSystem,
        targetComponent,
        missionSequence,
        position,
        0.0f,
        DEFAULT_WAYPOINT_ACCEPTANCE_RADIUS_METERS,
        0.0f,
        Float.NaN);
  }

  public static MavlinkMissionItemInt waypoint(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      GeoPosition position,
      float holdTimeSeconds,
      float acceptanceRadiusMeters,
      float passRadiusMeters,
      float yawDegrees) {
    MavlinkMissionItemInt missionItem =
        positionMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            MAV_CMD_NAV_WAYPOINT,
            position);

    missionItem.setParam1(holdTimeSeconds);
    missionItem.setParam2(acceptanceRadiusMeters);
    missionItem.setParam3(passRadiusMeters);
    missionItem.setParam4(yawDegrees);

    return missionItem;
  }

  public static MavlinkMissionItemInt loiterUnlimited(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      GeoPosition position,
      double radiusMeters) {
    MavlinkMissionItemInt missionItem =
        positionMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            MAV_CMD_NAV_LOITER_UNLIM,
            position);

    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3((float) radiusMeters);
    missionItem.setParam4(Float.NaN);

    return missionItem;
  }

  public static MavlinkMissionItemInt loiterTime(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      GeoPosition position,
      Duration duration,
      double radiusMeters) {
    MavlinkMissionItemInt missionItem =
        positionMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            MAV_CMD_NAV_LOITER_TIME,
            position);

    missionItem.setParam1(MavlinkDuration.toSeconds(duration, "duration"));
    missionItem.setParam2(0.0f);
    missionItem.setParam3((float) radiusMeters);
    missionItem.setParam4(Float.NaN);

    return missionItem;
  }


  public static MavlinkMissionItemInt jump(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      int targetMissionSequence,
      int repeatCount) {
    if (targetMissionSequence < 0) {
      throw new IllegalArgumentException("targetMissionSequence must not be negative");
    }
    if (repeatCount < 0) {
      throw new IllegalArgumentException("repeatCount must not be negative");
    }

    MavlinkMissionItemInt missionItem =
        commandMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            MAV_CMD_DO_JUMP);

    missionItem.setParam1(targetMissionSequence);
    missionItem.setParam2(repeatCount);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(0.0f);

    return missionItem;
  }

  public static MavlinkMissionItemInt returnToLaunch(
      int targetSystem,
      int targetComponent,
      int missionSequence) {
    MavlinkMissionItemInt missionItem =
        commandMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            MAV_CMD_NAV_RETURN_TO_LAUNCH);

    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(0.0f);

    return missionItem;
  }

  private static MavlinkMissionItemInt positionMissionItem(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      int command,
      GeoPosition position) {
    validatePosition(position);

    MavlinkMissionItemInt missionItem =
        baseMissionItem(
            targetSystem,
            targetComponent,
            missionSequence,
            resolvePositionFrame(position),
            command);

    missionItem.setLatitude(toScaledCoordinate(position.getLatitude()));
    missionItem.setLongitude(toScaledCoordinate(position.getLongitude()));
    missionItem.setAltitude(resolveAltitude(position));

    return missionItem;
  }

  private static MavlinkMissionItemInt commandMissionItem(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      int command) {
    return baseMissionItem(
        targetSystem,
        targetComponent,
        missionSequence,
        MAV_FRAME_MISSION,
        command);
  }

  private static MavlinkMissionItemInt baseMissionItem(
      int targetSystem,
      int targetComponent,
      int missionSequence,
      int frame,
      int command) {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setTargetSystem(targetSystem);
    missionItem.setTargetComponent(targetComponent);
    missionItem.setMissionSequence(missionSequence);
    missionItem.setFrame(frame);
    missionItem.setCommand(command);
    missionItem.setCurrent(0);
    missionItem.setAutocontinue(1);
    missionItem.setMissionType(MAV_MISSION_TYPE_MISSION);
    return missionItem;
  }

  private static int resolvePositionFrame(GeoPosition position) {
    if (position.getAltitudeMslMeters() != null) {
      return MAV_FRAME_GLOBAL_INT;
    }
    if (position.getAltitudeAglMeters() != null) {
      return MAV_FRAME_GLOBAL_TERRAIN_ALT_INT;
    }
    return MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
  }

  private static float resolveAltitude(GeoPosition position) {
    Double altitudeMeters = position.getPreferredAltitudeMeters();
    return altitudeMeters == null ? 0.0f : altitudeMeters.floatValue();
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

  private static void validateAltitude(Double altitudeMeters, String name) {
    if (altitudeMeters != null && !Double.isFinite(altitudeMeters)) {
      throw new IllegalArgumentException(name + " must be finite");
    }
  }

  private static int toScaledCoordinate(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Coordinate must not be null");
    }
    return (int) Math.round(value * 10_000_000.0d);
  }
}