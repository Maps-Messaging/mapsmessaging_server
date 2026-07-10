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

package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import lombok.Getter;

import java.util.Map;

/**
 * MAVLink MISSION_ACK mapped to a typed packet.
 */
@Getter
public final class MissionAckPacket extends MavlinkPacket {

  public static final int MAV_MISSION_ACCEPTED = 0;
  public static final int MAV_MISSION_ERROR = 1;
  public static final int MAV_MISSION_UNSUPPORTED_FRAME = 2;
  public static final int MAV_MISSION_UNSUPPORTED = 3;
  public static final int MAV_MISSION_NO_SPACE = 4;
  public static final int MAV_MISSION_INVALID = 5;
  public static final int MAV_MISSION_INVALID_PARAM1 = 6;
  public static final int MAV_MISSION_INVALID_PARAM2 = 7;
  public static final int MAV_MISSION_INVALID_PARAM3 = 8;
  public static final int MAV_MISSION_INVALID_PARAM4 = 9;
  public static final int MAV_MISSION_INVALID_PARAM5_X = 10;
  public static final int MAV_MISSION_INVALID_PARAM6_Y = 11;
  public static final int MAV_MISSION_INVALID_PARAM7 = 12;
  public static final int MAV_MISSION_INVALID_SEQUENCE = 13;
  public static final int MAV_MISSION_DENIED = 14;
  public static final int MAV_MISSION_OPERATION_CANCELLED = 15;

  private final int targetSystem;
  private final int targetComponent;
  private final int type;
  private final int missionType;
  private final boolean missionTypePresent;
  private final boolean valid;

  public MissionAckPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.targetSystem = getInt(fields, "target_system");
    this.targetComponent = getInt(fields, "target_component");
    this.type = getInt(fields, "type");

    this.missionTypePresent = fields.containsKey("mission_type");
    this.missionType = getInt(fields, "mission_type");

    this.valid = frame.isValid();
  }

  public boolean isAccepted() {
    return type == MAV_MISSION_ACCEPTED;
  }

  public boolean isRejected() {
    return type != MAV_MISSION_ACCEPTED;
  }

  public String getTypeName() {
    return switch (type) {
      case MAV_MISSION_ACCEPTED -> "ACCEPTED";
      case MAV_MISSION_ERROR -> "ERROR";
      case MAV_MISSION_UNSUPPORTED_FRAME -> "UNSUPPORTED_FRAME";
      case MAV_MISSION_UNSUPPORTED -> "UNSUPPORTED";
      case MAV_MISSION_NO_SPACE -> "NO_SPACE";
      case MAV_MISSION_INVALID -> "INVALID";
      case MAV_MISSION_INVALID_PARAM1 -> "INVALID_PARAM1";
      case MAV_MISSION_INVALID_PARAM2 -> "INVALID_PARAM2";
      case MAV_MISSION_INVALID_PARAM3 -> "INVALID_PARAM3";
      case MAV_MISSION_INVALID_PARAM4 -> "INVALID_PARAM4";
      case MAV_MISSION_INVALID_PARAM5_X -> "INVALID_PARAM5_X";
      case MAV_MISSION_INVALID_PARAM6_Y -> "INVALID_PARAM6_Y";
      case MAV_MISSION_INVALID_PARAM7 -> "INVALID_PARAM7";
      case MAV_MISSION_INVALID_SEQUENCE -> "INVALID_SEQUENCE";
      case MAV_MISSION_DENIED -> "DENIED";
      case MAV_MISSION_OPERATION_CANCELLED -> "OPERATION_CANCELLED";
      default -> "UNKNOWN";
    };
  }
}