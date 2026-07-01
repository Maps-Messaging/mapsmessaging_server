/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package io.mapsmessaging.state.mavlink.packet;

import io.mapsmessaging.mavlink.ProcessedFrame;
import java.util.Map;
import lombok.Getter;

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.NAMED_VALUE_FLOAT;

/**
 * MAVLink NAMED_VALUE_FLOAT mapped to a typed packet.
 */
@Getter
public class NamedValueFloatPacket extends MavlinkPacket {

  private static final int NAME_MAX_LENGTH = 10;

  private final long timeBootMs;
  private final String name;
  private final double value;
  private final boolean valid;

  public NamedValueFloatPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.timeBootMs = getLong(fields, "time_boot_ms");
    this.name = getName(fields);
    this.value = getDouble(fields, "value");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return NAMED_VALUE_FLOAT;
  }

  public boolean hasName() {
    return name != null && !name.isBlank();
  }

  public boolean hasValue() {
    return !Double.isNaN(value);
  }

  private String getName(Map<String, Object> fields) {
    String value = getString(fields, "name");
    if (value == null) {
      return null;
    }

    if (value.length() <= NAME_MAX_LENGTH) {
      return value;
    }

    return value.substring(0, NAME_MAX_LENGTH);
  }
}