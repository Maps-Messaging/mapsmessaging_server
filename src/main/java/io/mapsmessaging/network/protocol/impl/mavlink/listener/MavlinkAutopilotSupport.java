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

package io.mapsmessaging.network.protocol.impl.mavlink.listener;

import io.mapsmessaging.state.drone.model.autopilot.ArduPilotAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.AutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.GenericAutopilotState;
import io.mapsmessaging.state.drone.model.autopilot.Px4AutopilotState;

public final class MavlinkAutopilotSupport {

  private static final int MAV_AUTOPILOT_ARDUPILOTMEGA = 3;
  private static final int MAV_AUTOPILOT_PX4 = 12;

  private MavlinkAutopilotSupport() {
  }

  public static AutopilotState resolveAutopilotState(AutopilotState currentState, int autopilotType) {
    if (autopilotType == MAV_AUTOPILOT_PX4) {
      if (currentState instanceof Px4AutopilotState) {
        return currentState;
      }
      return new Px4AutopilotState();
    }

    if (autopilotType == MAV_AUTOPILOT_ARDUPILOTMEGA) {
      if (currentState instanceof ArduPilotAutopilotState) {
        return currentState;
      }
      return new ArduPilotAutopilotState();
    }

    if (currentState != null) {
      return currentState;
    }
    return new GenericAutopilotState();
  }

  public static String resolveAutopilotTypeName(int autopilotType) {
    return switch (autopilotType) {
      case MAV_AUTOPILOT_PX4 -> "PX4";
      case MAV_AUTOPILOT_ARDUPILOTMEGA -> "ARDUPILOT";
      default -> "GENERIC";
    };
  }

  public static String decodeFlightSoftwareVersion(long rawVersion) {
    if (rawVersion <= 0) {
      return null;
    }

    long major = (rawVersion >> 24) & 0xFFL;
    long minor = (rawVersion >> 16) & 0xFFL;
    long patch = (rawVersion >> 8) & 0xFFL;

    return major + "." + minor + "." + patch;
  }

  public static void populateHeartbeatFields(AutopilotState autopilotState,
                                             int autopilotType,
                                             int baseMode,
                                             long customMode,
                                             int systemStatus,
                                             int mavlinkVersion
                                             ) {
    autopilotState.setAutopilotType(resolveAutopilotTypeName(autopilotType));
    autopilotState.setBaseMode(baseMode);
    autopilotState.setCustomMode(customMode);
    autopilotState.setSystemStatus(systemStatus);
    autopilotState.setMavlinkVersion(mavlinkVersion);

    if (autopilotState instanceof Px4AutopilotState px4AutopilotState) {
      int mainMode = extractPx4MainMode(customMode);
      int subMode = extractPx4SubMode(customMode);

      px4AutopilotState.setMainMode(resolvePx4MainMode(mainMode));
      px4AutopilotState.setSubMode(resolvePx4SubMode(mainMode, subMode));
    }

    if (autopilotState instanceof ArduPilotAutopilotState arduPilotAutopilotState) {
      arduPilotAutopilotState.setModeNumber((int) customMode);
    }
  }

  public static void populateAutopilotVersionFields(AutopilotState autopilotState,
                                                    long uid,
                                                    long flightSoftwareVersionRaw,
                                                    long middlewareSoftwareVersionRaw,
                                                    long osSoftwareVersionRaw,
                                                    long capabilities) {
    autopilotState.setUid(uid);
    autopilotState.setFlightSoftwareVersionRaw(flightSoftwareVersionRaw);
    autopilotState.setFlightSoftwareVersion(decodeFlightSoftwareVersion(flightSoftwareVersionRaw));
    autopilotState.setMiddlewareSoftwareVersionRaw(middlewareSoftwareVersionRaw);
    autopilotState.setMiddlewareSoftwareVersion(decodeFlightSoftwareVersion(middlewareSoftwareVersionRaw));
    autopilotState.setOsSoftwareVersionRaw(osSoftwareVersionRaw);
    autopilotState.setOsSoftwareVersion(decodeFlightSoftwareVersion(osSoftwareVersionRaw));
    autopilotState.setCapabilities(capabilities);
  }

  private static int extractPx4MainMode(long customMode) {
    return (int) ((customMode >> 16) & 0xFFL);
  }

  private static int extractPx4SubMode(long customMode) {
    return (int) ((customMode >> 24) & 0xFFL);
  }

  private static String resolvePx4MainMode(int mainMode) {
    return switch (mainMode) {
      case 1 -> "MANUAL";
      case 2 -> "ALTCTL";
      case 3 -> "POSCTL";
      case 4 -> "AUTO";
      case 5 -> "ACRO";
      case 6 -> "OFFBOARD";
      case 7 -> "STABILIZED";
      case 8 -> "RATTITUDE";
      default -> null;
    };
  }

  private static String resolvePx4SubMode(int mainMode, int subMode) {
    if (mainMode != 4) {
      return null;
    }

    return switch (subMode) {
      case 1 -> "READY";
      case 2 -> "TAKEOFF";
      case 3 -> "LOITER";
      case 4 -> "MISSION";
      case 5 -> "RTL";
      case 6 -> "LAND";
      case 7 -> "RTGS";
      case 8 -> "FOLLOW_TARGET";
      case 9 -> "PRECLAND";
      default -> null;
    };
  }
}