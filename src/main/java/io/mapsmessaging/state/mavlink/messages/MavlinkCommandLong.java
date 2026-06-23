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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MavlinkCommandLong {

  public static final int MESSAGE_ID_COMMAND_LONG = 76;

  public static final int MAV_CMD_COMPONENT_ARM_DISARM = 400;
  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;
  public static final int MAV_CMD_DO_SET_MODE = 176;
  public static final int MAV_CMD_MISSION_START = 300;


  public static final float ARM = 1.0f;
  public static final float DISARM = 0.0f;
  public static final float NORMAL_ARM_DISARM = 0.0f;
  public static final float FORCE_ARM_DISARM = 21196.0f;

  private String messageType = "COMMAND_LONG";
  private int messageId = MESSAGE_ID_COMMAND_LONG;
  private int targetSystem;
  private int targetComponent;
  private int command;
  private int confirmation = 0;
  private int sequence;
  private float param1 = 0.0f;
  private float param2 = 0.0f;
  private float param3 = 0.0f;
  private float param4 = 0.0f;
  private float param5 = 0.0f;
  private float param6 = 0.0f;
  private float param7 = 0.0f;

  private static final Gson gson = GsonFactory.createStrictJsonWithSafeFloats();

  public static MavlinkCommandLong arm(
      int targetSystem,
      int targetComponent,
      int sequence) {
    return arm(targetSystem, targetComponent, sequence, false);
  }

  public static MavlinkCommandLong forceArm(
      int targetSystem,
      int targetComponent,
      int sequence) {
    return arm(targetSystem, targetComponent, sequence, true);
  }

  public static MavlinkCommandLong disarm(
      int targetSystem,
      int targetComponent,
      int sequence) {
    return disarm(targetSystem, targetComponent, sequence, false);
  }

  public static MavlinkCommandLong forceDisarm(
      int targetSystem,
      int targetComponent,
      int sequence) {
    return disarm(targetSystem, targetComponent, sequence, true);
  }

  public static MavlinkCommandLong arm(
      int targetSystem,
      int targetComponent,
      int sequence,
      boolean force) {
    MavlinkCommandLong commandLong = createArmDisarmCommand(
        targetSystem,
        targetComponent,
        sequence,
        ARM,
        force);

    return commandLong;
  }

  public static MavlinkCommandLong disarm(
      int targetSystem,
      int targetComponent,
      int sequence,
      boolean force) {
    MavlinkCommandLong commandLong = createArmDisarmCommand(
        targetSystem,
        targetComponent,
        sequence,
        DISARM,
        force);

    return commandLong;
  }

  public static MavlinkCommandLong missionStart(
      int targetSystem,
      int targetComponent,
      int sequence) {
    MavlinkCommandLong commandLong = command(
        targetSystem,
        targetComponent,
        MAV_CMD_MISSION_START,
        sequence);

    return commandLong;
  }

  public static MavlinkCommandLong missionStart(
      int targetSystem,
      int targetComponent,
      int sequence,
      int firstMissionItem,
      int lastMissionItem) {
    MavlinkCommandLong commandLong = missionStart(
        targetSystem,
        targetComponent,
        sequence);

    commandLong.setParam1(firstMissionItem);
    commandLong.setParam2(lastMissionItem);

    return commandLong;
  }

  public static MavlinkCommandLong returnToLaunch(
      int targetSystem,
      int targetComponent,
      int sequence) {
    MavlinkCommandLong commandLong = command(
        targetSystem,
        targetComponent,
        MAV_CMD_NAV_RETURN_TO_LAUNCH,
        sequence);

    return commandLong;
  }

  public static MavlinkCommandLong loiterUnlimited(
      int targetSystem,
      int targetComponent,
      int sequence) {
    MavlinkCommandLong commandLong = command(
        targetSystem,
        targetComponent,
        MAV_CMD_NAV_LOITER_UNLIM,
        sequence);

    return commandLong;
  }

  public static MavlinkCommandLong waypoint(
      int targetSystem,
      int targetComponent,
      int sequence,
      float holdTimeSeconds,
      float acceptanceRadiusMeters,
      float passRadiusMeters,
      float yawDegrees,
      double latitude,
      double longitude,
      float altitudeMeters) {
    MavlinkCommandLong commandLong = command(
        targetSystem,
        targetComponent,
        MAV_CMD_NAV_WAYPOINT,
        sequence);

    commandLong.setParam1(holdTimeSeconds);
    commandLong.setParam2(acceptanceRadiusMeters);
    commandLong.setParam3(passRadiusMeters);
    commandLong.setParam4(yawDegrees);
    commandLong.setParam5((float) latitude);
    commandLong.setParam6((float) longitude);
    commandLong.setParam7(altitudeMeters);

    return commandLong;
  }

  public static MavlinkCommandLong command(
      int targetSystem,
      int targetComponent,
      int command,
      int sequence) {
    MavlinkCommandLong commandLong = new MavlinkCommandLong();
    commandLong.setTargetSystem(targetSystem);
    commandLong.setTargetComponent(targetComponent);
    commandLong.setCommand(command);
    commandLong.setSequence(sequence);

    return commandLong;
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
    payload.addProperty("command", command);
    payload.addProperty("confirmation", confirmation);
    payload.addProperty("param1", param1);
    payload.addProperty("param2", param2);
    payload.addProperty("param3", param3);
    payload.addProperty("param4", param4);
    payload.addProperty("param5", param5);
    payload.addProperty("param6", param6);
    payload.addProperty("param7", param7);

    root.add("header", header);
    root.add("payload", payload);

    return root;
  }

  private static MavlinkCommandLong createArmDisarmCommand(
      int targetSystem,
      int targetComponent,
      int sequence,
      float armState,
      boolean force) {
    MavlinkCommandLong commandLong = command(
        targetSystem,
        targetComponent,
        MAV_CMD_COMPONENT_ARM_DISARM,
        sequence);

    commandLong.setParam1(armState);

    if (force) {
      commandLong.setParam2(FORCE_ARM_DISARM);
    } else {
      commandLong.setParam2(NORMAL_ARM_DISARM);
    }

    return commandLong;
  }
}