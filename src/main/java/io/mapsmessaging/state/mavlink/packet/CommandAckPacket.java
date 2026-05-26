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
import java.util.Map;
import lombok.Getter;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.COMMAND_ACK;

/**
 * MAVLink COMMAND_ACK mapped to a typed packet.
 */
@Getter
public class CommandAckPacket extends MavlinkPacket {

  public static final int MAV_RESULT_ACCEPTED = 0;
  public static final int MAV_RESULT_TEMPORARILY_REJECTED = 1;
  public static final int MAV_RESULT_DENIED = 2;
  public static final int MAV_RESULT_UNSUPPORTED = 3;
  public static final int MAV_RESULT_FAILED = 4;
  public static final int MAV_RESULT_IN_PROGRESS = 5;
  public static final int MAV_RESULT_CANCELLED = 6;
  public static final int MAV_RESULT_COMMAND_LONG_ONLY = 7;
  public static final int MAV_RESULT_COMMAND_INT_ONLY = 8;
  public static final int MAV_RESULT_COMMAND_UNSUPPORTED_MAV_FRAME = 9;

  private final int command;
  private final int result;

  private final boolean progressPresent;
  private final int progress;

  private final boolean resultParameter2Present;
  private final int resultParameter2;

  private final boolean targetSystemPresent;
  private final int targetSystem;

  private final boolean targetComponentPresent;
  private final int targetComponent;

  private final boolean valid;

  public CommandAckPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.command = getInt(fields, "command");
    this.result = getInt(fields, "result");

    this.progressPresent = fields.containsKey("progress");
    this.progress = getInt(fields, "progress");

    this.resultParameter2Present = fields.containsKey("result_param2");
    this.resultParameter2 = getInt(fields, "result_param2");

    this.targetSystemPresent = fields.containsKey("target_system");
    this.targetSystem = getInt(fields, "target_system");

    this.targetComponentPresent = fields.containsKey("target_component");
    this.targetComponent = getInt(fields, "target_component");

    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return COMMAND_ACK;
  }

  public boolean isAccepted() {
    return result == MAV_RESULT_ACCEPTED;
  }

  public boolean isInProgress() {
    return result == MAV_RESULT_IN_PROGRESS;
  }

  public boolean isRejected() {
    return result == MAV_RESULT_TEMPORARILY_REJECTED
        || result == MAV_RESULT_DENIED
        || result == MAV_RESULT_UNSUPPORTED
        || result == MAV_RESULT_FAILED
        || result == MAV_RESULT_CANCELLED
        || result == MAV_RESULT_COMMAND_LONG_ONLY
        || result == MAV_RESULT_COMMAND_INT_ONLY
        || result == MAV_RESULT_COMMAND_UNSUPPORTED_MAV_FRAME;
  }

  public boolean isTerminal() {
    return result != MAV_RESULT_IN_PROGRESS;
  }

  public String getResultName() {
    return switch (result) {
      case MAV_RESULT_ACCEPTED -> "ACCEPTED";
      case MAV_RESULT_TEMPORARILY_REJECTED -> "TEMPORARILY_REJECTED";
      case MAV_RESULT_DENIED -> "DENIED";
      case MAV_RESULT_UNSUPPORTED -> "UNSUPPORTED";
      case MAV_RESULT_FAILED -> "FAILED";
      case MAV_RESULT_IN_PROGRESS -> "IN_PROGRESS";
      case MAV_RESULT_CANCELLED -> "CANCELLED";
      case MAV_RESULT_COMMAND_LONG_ONLY -> "COMMAND_LONG_ONLY";
      case MAV_RESULT_COMMAND_INT_ONLY -> "COMMAND_INT_ONLY";
      case MAV_RESULT_COMMAND_UNSUPPORTED_MAV_FRAME -> "COMMAND_UNSUPPORTED_MAV_FRAME";
      default -> "UNKNOWN";
    };
  }
}