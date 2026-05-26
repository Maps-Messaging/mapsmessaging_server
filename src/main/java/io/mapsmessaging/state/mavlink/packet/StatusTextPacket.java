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

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.STATUSTEXT;

/**
 * MAVLink STATUSTEXT mapped to a typed packet.
 */
@Getter
public class StatusTextPacket extends MavlinkPacket {

  private final int severity;
  private final String text;

  private final boolean idPresent;
  private final int id;

  private final boolean chunkSequencePresent;
  private final int chunkSequence;

  private final boolean valid;

  public StatusTextPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.severity = getInt(fields, "severity");
    this.text = getString(fields, "text");

    this.idPresent = fields.containsKey("id");
    this.id = getInt(fields, "id");

    this.chunkSequencePresent = fields.containsKey("chunk_seq");
    this.chunkSequence = getInt(fields, "chunk_seq");

    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return STATUSTEXT;
  }

  public boolean isEmergency() {
    return severity == 0;
  }

  public boolean isAlert() {
    return severity == 1;
  }

  public boolean isCritical() {
    return severity == 2;
  }

  public boolean isError() {
    return severity == 3;
  }

  public boolean isWarning() {
    return severity == 4;
  }

  public boolean isNotice() {
    return severity == 5;
  }

  public boolean isInfo() {
    return severity == 6;
  }

  public boolean isDebug() {
    return severity == 7;
  }

  public boolean isProblem() {
    return severity >= 0 && severity <= 4;
  }
}