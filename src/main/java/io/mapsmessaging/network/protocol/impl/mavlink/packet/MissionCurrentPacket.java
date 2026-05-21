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

import java.util.Map;

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.MISSION_CURRENT;

public class MissionCurrentPacket extends MavlinkPacket {

  private final int sequence;
  private final boolean valid;

  public MissionCurrentPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.sequence = getInt(fields, "seq");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return MISSION_CURRENT;
  }

  public boolean isValid() {
    return valid;
  }

  public int getSequence() {
    return sequence;
  }

}