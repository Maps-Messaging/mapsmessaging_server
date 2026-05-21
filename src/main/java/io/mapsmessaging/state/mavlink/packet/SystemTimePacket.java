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

import static io.mapsmessaging.state.mavlink.packet.MavlinkMessageIds.SYSTEM_TIME;

@Getter
public class SystemTimePacket extends MavlinkPacket {

  private final long unixTimeEpochMillis;
  private final long bootTimeMillis;
  private final boolean valid;

  public SystemTimePacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.unixTimeEpochMillis = getUnixTimeMillis(fields, "time_unix_usec");
    this.bootTimeMillis = getLong(fields, "time_boot_ms");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return SYSTEM_TIME;
  }

  public boolean hasValidUnixTime() {
    return unixTimeEpochMillis > 0;
  }

  public boolean hasValidBootTime() {
    return bootTimeMillis >= 0;
  }

  private long getUnixTimeMillis(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return -1L;
    }

    long micros = ((Number) value).longValue();
    if (micros <= 0) {
      return -1L;
    }
    return micros / 1000L;
  }
}