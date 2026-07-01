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

public final class MavlinkMissionItemIntFactory {

  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;

  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_LOITER_TIME = 19;
  public static final int MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;

  public static final int MAV_MISSION_TYPE_MISSION = 0;

  private static final float DEFAULT_WAYPOINT_ACCEPTANCE_RADIUS_METERS = 2.0f;

  private MavlinkMissionItemIntFactory() {
  }

  public static MavlinkMissionItemInt waypoint(
      int targetSystem,
      int targetComponent,
      int sequence,
      GeoPosition position) {
    return waypoint(
        targetSystem,
        targetComponent,
        sequence,
        position,
        0.0f,
        DEFAULT_WAYPOINT_ACCEPTANCE_RADIUS_METERS,
        0.0f,
        Float.NaN);
  }

  public static MavlinkMissionItemInt waypoint(
      int targetSystem,
      int targetComponent,
      int sequence,
      GeoPosition position,
      float holdTimeSeconds,
      float acceptanceRadiusMeters,
      float passRadiusMeters,
      float yawDegrees) {
    MavlinkMissionItemInt missionItem = positionMissionItem(
        targetSystem,
        targetComponent,
        sequence,
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
      int sequence,
      GeoPosition position,
      double radiusMeters) {
    MavlinkMissionItemInt missionItem = positionMissionItem(
        targetSystem,
        targetComponent,
        sequence,
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
      int sequence,
      GeoPosition position,
      long durationSeconds,
      double radiusMeters) {
    MavlinkMissionItemInt missionItem = positionMissionItem(
        targetSystem,
        targetComponent,
        sequence,
        MAV_CMD_NAV_LOITER_TIME,
        position);

    missionItem.setParam1(durationSeconds);
    missionItem.setParam2(0.0f);
    missionItem.setParam3((float) radiusMeters);
    missionItem.setParam4(Float.NaN);

    return missionItem;
  }

  public static MavlinkMissionItemInt returnToLaunch(
      int targetSystem,
      int targetComponent,
      int sequence) {
    MavlinkMissionItemInt missionItem = baseMissionItem(
        targetSystem,
        targetComponent,
        sequence,
        MAV_CMD_NAV_RETURN_TO_LAUNCH);

    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(0.0f);
    missionItem.setLatitude(0);
    missionItem.setLongitude(0);
    missionItem.setAltitude(0.0f);

    return missionItem;
  }

  private static MavlinkMissionItemInt positionMissionItem(
      int targetSystem,
      int targetComponent,
      int sequence,
      int command,
      GeoPosition position) {
    if (position == null) {
      throw new IllegalArgumentException("Position must not be null");
    }

    MavlinkMissionItemInt missionItem = baseMissionItem(targetSystem, targetComponent, sequence, command);
    missionItem.setLatitude(toScaledCoordinate(position.getLatitude()));
    missionItem.setLongitude(toScaledCoordinate(position.getLongitude()));

    Double altitudeMeters = position.getPreferredAltitudeMeters();
    if (altitudeMeters != null) {
      missionItem.setAltitude(altitudeMeters.floatValue());
    }

    return missionItem;
  }

  private static MavlinkMissionItemInt baseMissionItem(
      int targetSystem,
      int targetComponent,
      int sequence,
      int command) {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setTargetSystem(targetSystem);
    missionItem.setTargetComponent(targetComponent);
    missionItem.setSequence(sequence);
    missionItem.setFrame(MAV_FRAME_GLOBAL_RELATIVE_ALT_INT);
    missionItem.setCommand(command);
    missionItem.setCurrent(0);
    missionItem.setAutocontinue(1);
    missionItem.setMissionType(MAV_MISSION_TYPE_MISSION);
    return missionItem;
  }

  private static int toScaledCoordinate(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Coordinate must not be null");
    }
    return (int) Math.round(value * 10_000_000.0d);
  }
}