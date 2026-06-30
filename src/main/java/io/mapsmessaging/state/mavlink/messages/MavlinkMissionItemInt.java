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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.mavlink.GsonFactory;
import io.mapsmessaging.state.drone.model.GeoPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MavlinkMissionItemInt {

  public static final int MESSAGE_ID_MISSION_ITEM_INT = 73;

  public static final int MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6;

  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;

  public static final int MAV_MISSION_TYPE_MISSION = 0;

  private String messageType = "MISSION_ITEM_INT";
  private int messageId = MESSAGE_ID_MISSION_ITEM_INT;
  private int targetSystem;
  private int targetComponent;
  private int sequence;
  private int frame = MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
  private int command = MAV_CMD_NAV_WAYPOINT;
  private int current = 0;
  private int autocontinue = 1;
  private float param1 = 0.0f;
  private float param2 = 2.0f;
  private float param3 = 0.0f;
  private float param4 = Float.NaN;
  private int latitude;
  private int longitude;
  private float altitude;
  private int missionType = MAV_MISSION_TYPE_MISSION;

  private static final Gson gson = GsonFactory.createStrictJsonWithSafeFloats();

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
        2.0f,
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
    MavlinkMissionItemInt missionItem = createPositionMissionItem(
        targetSystem,
        targetComponent,
        sequence,
        position,
        MAV_CMD_NAV_WAYPOINT);

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
      GeoPosition position) {
    MavlinkMissionItemInt missionItem = createPositionMissionItem(
        targetSystem,
        targetComponent,
        sequence,
        position,
        MAV_CMD_NAV_LOITER_UNLIM);

    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(Float.NaN);

    return missionItem;
  }

  public static MavlinkMissionItemInt returnToLaunch(
      int targetSystem,
      int targetComponent,
      int sequence) {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setTargetSystem(targetSystem);
    missionItem.setTargetComponent(targetComponent);
    missionItem.setSequence(sequence);
    missionItem.setCommand(MAV_CMD_NAV_RETURN_TO_LAUNCH);
    missionItem.setParam1(0.0f);
    missionItem.setParam2(0.0f);
    missionItem.setParam3(0.0f);
    missionItem.setParam4(0.0f);
    missionItem.setLatitude(0);
    missionItem.setLongitude(0);
    missionItem.setAltitude(0.0f);

    return missionItem;
  }

  public JsonObject toMavlinkJsonObject(int systemId, int componentId) {
    JsonObject root = new JsonObject();
    JsonObject header = new JsonObject();
    JsonObject payload = new JsonObject();

    header.addProperty("version", "V2");
    header.addProperty("systemId", systemId);
    header.addProperty("componentId", componentId);
    header.addProperty("sequence", sequence);
    header.addProperty("messageId", messageId);
    header.addProperty("signed", false);
    header.addProperty("incompatibilityFlags", 0);
    header.addProperty("compatibilityFlags", 0);

    payload.addProperty("target_system", targetSystem);
    payload.addProperty("target_component", targetComponent);
    payload.addProperty("seq", sequence);
    payload.addProperty("frame", frame);
    payload.addProperty("command", command);
    payload.addProperty("current", current);
    payload.addProperty("autocontinue", autocontinue);
    payload.addProperty("param1", param1);
    payload.addProperty("param2", param2);
    payload.addProperty("param3", param3);
    payload.addProperty("param4", param4);
    payload.addProperty("x", latitude);
    payload.addProperty("y", longitude);
    payload.addProperty("z", altitude);
    payload.addProperty("mission_type", missionType);

    root.add("header", header);
    root.add("payload", payload);

    return root;
  }

  private static MavlinkMissionItemInt createPositionMissionItem(
      int targetSystem,
      int targetComponent,
      int sequence,
      GeoPosition position,
      int command) {
    MavlinkMissionItemInt missionItem = new MavlinkMissionItemInt();
    missionItem.setTargetSystem(targetSystem);
    missionItem.setTargetComponent(targetComponent);
    missionItem.setSequence(sequence);
    missionItem.setCommand(command);
    missionItem.setLatitude(toScaledCoordinate(position.getLatitude()));
    missionItem.setLongitude(toScaledCoordinate(position.getLongitude()));

    Double altitudeMeters = position.getPreferredAltitudeMeters();
    if (altitudeMeters != null) {
      missionItem.setAltitude(altitudeMeters.floatValue());
    }

    return missionItem;
  }

  private static int toScaledCoordinate(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Coordinate must not be null");
    }
    return (int) Math.round(value * 10_000_000.0d);
  }
}