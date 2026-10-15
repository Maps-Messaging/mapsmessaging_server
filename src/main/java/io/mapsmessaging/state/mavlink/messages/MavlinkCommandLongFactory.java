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

public final class MavlinkCommandLongFactory {

  public static final int MAV_CMD_NAV_WAYPOINT = 16;
  public static final int MAV_CMD_NAV_LOITER_UNLIM = 17;
  public static final int MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;
  public static final int MAV_CMD_DO_ORBIT = 34;
  public static final int MAV_CMD_DO_SET_MODE = 176;
  public static final int MAV_CMD_DO_PAUSE_CONTINUE = 193;
  public static final int MAV_CMD_DO_SET_MISSION_CURRENT = 224;
  public static final int MAV_CMD_MISSION_START = 300;
  public static final int MAV_CMD_COMPONENT_ARM_DISARM = 400;
  public static final int MAV_CMD_REQUEST_MESSAGE = 512;

  public static final float ARM = 1.0f;
  public static final float DISARM = 0.0f;
  public static final float NORMAL_ARM_DISARM = 0.0f;
  public static final float FORCE_ARM_DISARM = 21196.0f;

  public static final float PAUSE = 0.0f;
  public static final float CONTINUE = 1.0f;

  public static final float MAV_MODE_FLAG_CUSTOM_MODE_ENABLED = 1.0f;

  public static final float ARDUPLANE_MODE_TAKEOFF = 13.0f;
  public static final float ARDUPLANE_MODE_GUIDED = 15.0f;

  private MavlinkCommandLongFactory() {
  }

  public static MavlinkCommandLong arm(int targetSystem, int targetComponent, int sequence) {
    return arm(targetSystem, targetComponent, sequence, false);
  }

  public static MavlinkCommandLong forceArm(int targetSystem, int targetComponent, int sequence) {
    return arm(targetSystem, targetComponent, sequence, true);
  }

  public static MavlinkCommandLong disarm(int targetSystem, int targetComponent, int sequence) {
    return disarm(targetSystem, targetComponent, sequence, false);
  }

  public static MavlinkCommandLong forceDisarm(int targetSystem, int targetComponent, int sequence) {
    return disarm(targetSystem, targetComponent, sequence, true);
  }

  public static MavlinkCommandLong arm(
      int targetSystem,
      int targetComponent,
      int sequence,
      boolean force) {

    return armDisarm(targetSystem, targetComponent, sequence, ARM, force);
  }

  public static MavlinkCommandLong disarm(
      int targetSystem,
      int targetComponent,
      int sequence,
      boolean force) {

    return armDisarm(targetSystem, targetComponent, sequence, DISARM, force);
  }

  public static MavlinkCommandLong guidedMode(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return ardupilotPlaneMode(
        targetSystem,
        targetComponent,
        sequence,
        ARDUPLANE_MODE_GUIDED);
  }

  public static MavlinkCommandLong takeoffMode(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return ardupilotPlaneMode(
        targetSystem,
        targetComponent,
        sequence,
        ARDUPLANE_MODE_TAKEOFF);
  }

  public static MavlinkCommandLong missionStart(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return command(targetSystem, targetComponent, MAV_CMD_MISSION_START, sequence);
  }

  public static MavlinkCommandLong missionStart(
      int targetSystem,
      int targetComponent,
      int sequence,
      int firstMissionItem,
      int lastMissionItem) {

    MavlinkCommandLong commandLong =
        missionStart(targetSystem, targetComponent, sequence);

    commandLong.setParam1(firstMissionItem);
    commandLong.setParam2(lastMissionItem);
    return commandLong;
  }

  public static MavlinkCommandLong requestMessage(int targetSystem, int targetComponent, int sequence, int messageId) {
    MavlinkCommandLong commandLong = command(targetSystem, targetComponent, MAV_CMD_REQUEST_MESSAGE, sequence);
    commandLong.setParam1(messageId);
    return commandLong;
  }

  public static MavlinkCommandLong setMissionCurrent(
      int targetSystem,
      int targetComponent,
      int sequence,
      int missionSequence,
      boolean resetMission) {

    if (missionSequence < -1) {
      throw new IllegalArgumentException("missionSequence must be -1 or greater");
    }

    MavlinkCommandLong commandLong =
        command(
            targetSystem,
            targetComponent,
            MAV_CMD_DO_SET_MISSION_CURRENT,
            sequence);

    commandLong.setParam1(missionSequence);
    commandLong.setParam2(resetMission ? 1.0f : 0.0f);
    return commandLong;
  }

  public static MavlinkCommandLong orbit(
      int targetSystem,
      int targetComponent,
      int sequence,
      double radiusMeters,
      float velocityMetersPerSecond,
      float yawBehaviour,
      double latitude,
      double longitude,
      float altitudeMeters) {

    MavlinkCommandLong commandLong =
        command(targetSystem, targetComponent, MAV_CMD_DO_ORBIT, sequence);

    commandLong.setParam1((float) radiusMeters);
    commandLong.setParam2(velocityMetersPerSecond);
    commandLong.setParam3(yawBehaviour);
    commandLong.setParam4(0.0f);
    commandLong.setParam5((float) latitude);
    commandLong.setParam6((float) longitude);
    commandLong.setParam7(altitudeMeters);

    return commandLong;
  }

  public static MavlinkCommandLong returnToLaunch(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return command(
        targetSystem,
        targetComponent,
        MAV_CMD_NAV_RETURN_TO_LAUNCH,
        sequence);
  }

  public static MavlinkCommandLong loiterUnlimited(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return command(
        targetSystem,
        targetComponent,
        MAV_CMD_NAV_LOITER_UNLIM,
        sequence);
  }

  public static MavlinkCommandLong standby(
      int targetSystem,
      int targetComponent,
      int sequence) {

    return pause(targetSystem, targetComponent, sequence);
  }

  public static MavlinkCommandLong pause(
      int targetSystem,
      int targetComponent,
      int sequence) {

    MavlinkCommandLong commandLong =
        command(
            targetSystem,
            targetComponent,
            MAV_CMD_DO_PAUSE_CONTINUE,
            sequence);

    commandLong.setParam1(PAUSE);
    return commandLong;
  }

  public static MavlinkCommandLong resume(
      int targetSystem,
      int targetComponent,
      int sequence) {

    MavlinkCommandLong commandLong =
        command(
            targetSystem,
            targetComponent,
            MAV_CMD_DO_PAUSE_CONTINUE,
            sequence);

    commandLong.setParam1(CONTINUE);
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

    MavlinkCommandLong commandLong =
        command(
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
    commandLong.setConfirmation(0);
    return commandLong;
  }

  private static MavlinkCommandLong ardupilotPlaneMode(
      int targetSystem,
      int targetComponent,
      int sequence,
      float customMode) {

    MavlinkCommandLong commandLong =
        command(
            targetSystem,
            targetComponent,
            MAV_CMD_DO_SET_MODE,
            sequence);

    commandLong.setParam1(MAV_MODE_FLAG_CUSTOM_MODE_ENABLED);
    commandLong.setParam2(customMode);
    return commandLong;
  }

  private static MavlinkCommandLong armDisarm(
      int targetSystem,
      int targetComponent,
      int sequence,
      float armState,
      boolean force) {

    MavlinkCommandLong commandLong =
        command(
            targetSystem,
            targetComponent,
            MAV_CMD_COMPONENT_ARM_DISARM,
            sequence);

    commandLong.setParam1(armState);
    commandLong.setParam2(
        force ? FORCE_ARM_DISARM : NORMAL_ARM_DISARM);
    return commandLong;
  }
}
