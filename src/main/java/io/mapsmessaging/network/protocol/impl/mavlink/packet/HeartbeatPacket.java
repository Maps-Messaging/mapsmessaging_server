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

package io.mapsmessaging.network.protocol.impl.mavlink.packet;


import io.mapsmessaging.mavlink.ProcessedFrame;
import lombok.Getter;

import java.util.Map;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.HEARTBEAT;

@Getter
public class HeartbeatPacket extends MavlinkPacket {

  private static final int MAV_MODE_FLAG_SAFETY_ARMED = 128;

  private final int type;
  private final int autopilot;
  private final int baseMode;
  private final long customMode;
  private final int systemStatus;
  private final boolean mavlinkVersionPresent;
  private final int mavlinkVersion;
  private final boolean valid;

  public HeartbeatPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.type = getInt(fields, "type");
    this.autopilot = getInt(fields, "autopilot");
    this.baseMode = getInt(fields, "base_mode");
    this.customMode = getLong(fields, "custom_mode");
    this.systemStatus = getInt(fields, "system_status");
    this.mavlinkVersionPresent = fields.containsKey("mavlink_version");
    this.mavlinkVersion = getInt(fields, "mavlink_version");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return HEARTBEAT;
  }

  public boolean isArmed() {
    return (baseMode & MAV_MODE_FLAG_SAFETY_ARMED) != 0;
  }

  public String getFlightMode() {
    return Long.toUnsignedString(customMode);
  }

  public int getVehicleClass() {
    return type;
  }
}