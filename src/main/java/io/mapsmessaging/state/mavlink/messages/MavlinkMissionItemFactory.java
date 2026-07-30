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

public final class MavlinkMissionItemFactory {

  public static final int MAV_FRAME_GLOBAL = 0;
  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT = 3;
  public static final int MAV_FRAME_GLOBAL_TERRAIN_ALT = 10;

  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_MISSION_TYPE_MISSION = 0;

  private static final int GUIDED_WAYPOINT_MISSION_SEQUENCE = 0;
  private static final int CURRENT_GUIDED_WAYPOINT = 2;
  private static final int AUTOCONTINUE = 1;

  private MavlinkMissionItemFactory() {
  }

  public static MavlinkMissionItem guidedWaypoint(int targetSystem, int targetComponent, GeoPosition position) {
    validatePosition(position);
    return guidedWaypoint(targetSystem, targetComponent, position, resolvePositionFrame(position), resolveAltitude(position));
  }

  public static MavlinkMissionItem guidedWaypointRelativeAltitude(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      double relativeAltitudeMeters) {
    validatePosition(position);
    requireFinite(relativeAltitudeMeters, "relativeAltitudeMeters");
    return guidedWaypoint(targetSystem, targetComponent, position, MAV_FRAME_GLOBAL_RELATIVE_ALT, (float) relativeAltitudeMeters);
  }

  private static MavlinkMissionItem guidedWaypoint(
      int targetSystem,
      int targetComponent,
      GeoPosition position,
      int frame,
      float altitudeMeters) {
    MavlinkMissionItem missionItem = new MavlinkMissionItem();
    missionItem.setTargetSystem(targetSystem);
    missionItem.setTargetComponent(targetComponent);
    missionItem.setMissionSequence(GUIDED_WAYPOINT_MISSION_SEQUENCE);
    missionItem.setFrame(frame);
    missionItem.setCommand(MAV_CMD_NAV_WAYPOINT);
    missionItem.setCurrent(CURRENT_GUIDED_WAYPOINT);
    missionItem.setAutocontinue(AUTOCONTINUE);
    missionItem.setMissionType(MAV_MISSION_TYPE_MISSION);

    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(0.0f);

    missionItem.setLatitude(position.getLatitude().floatValue());
    missionItem.setLongitude(position.getLongitude().floatValue());
    missionItem.setAltitude(altitudeMeters);
    return missionItem;
  }

  private static int resolvePositionFrame(GeoPosition position) {
    if (position.getAltitudeMslMeters() != null) {
      return MAV_FRAME_GLOBAL;
    }
    if (position.getAltitudeAglMeters() != null) {
      return MAV_FRAME_GLOBAL_TERRAIN_ALT;
    }
    return MAV_FRAME_GLOBAL_RELATIVE_ALT;
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
}
