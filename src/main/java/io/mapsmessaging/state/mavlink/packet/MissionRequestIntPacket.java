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
 * MAVLink MISSION_REQUEST_INT mapped to a typed packet.
 */
@Getter
public final class MissionRequestIntPacket extends MavlinkPacket {

  private final int targetSystem;
  private final int targetComponent;
  private final int sequence;
  private final int missionType;
  private final boolean missionTypePresent;
  private final boolean valid;

  public MissionRequestIntPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.targetSystem = getInt(fields, "target_system");
    this.targetComponent = getInt(fields, "target_component");
    this.sequence = getInt(fields, "seq");

    this.missionTypePresent = fields.containsKey("mission_type");
    this.missionType = getInt(fields, "mission_type");

    this.valid = frame.isValid();
  }
}