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

/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-20
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import lombok.Getter;

import java.util.Map;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.EXTENDED_SYS_STATE;

@Getter
public class ExtendedSysStatePacket implements MavlinkPacket {

  private final int landedState;
  private final int vtolState;
  private final boolean valid;

  public ExtendedSysStatePacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.landedState = getInt(fields, "landed_state");
    this.vtolState = getInt(fields, "vtol_state");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return EXTENDED_SYS_STATE;
  }

  public String getLandedStateName() {
    return switch (landedState) {
      case 1 -> "ON_GROUND";
      case 2 -> "IN_AIR";
      case 3 -> "TAKEOFF";
      case 4 -> "LANDING";
      default -> "UNDEFINED";
    };
  }

  public String getVtolStateName() {
    return switch (vtolState) {
      case 1 -> "UNDEFINED";
      case 2 -> "TRANSITION_TO_FW";
      case 3 -> "TRANSITION_TO_MC";
      case 4 -> "MC";
      case 5 -> "FW";
      default -> "UNDEFINED";
    };
  }

  private int getInt(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return -1;
    }
    return ((Number) value).intValue();
  }
}