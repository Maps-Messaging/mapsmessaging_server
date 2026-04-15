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

import static io.mapsmessaging.network.protocol.impl.mavlink.packet.MavlinkMessageIds.GPS_RAW_INT;

@Getter
public class GpsRawIntPacket implements MavlinkPacket {

  private final int fixType;
  private final int satellitesVisible;
  private final double hdop;
  private final double vdop;
  private final boolean valid;

  public GpsRawIntPacket(ProcessedFrame frame) {
    Map<String, Object> fields = frame.getFields();

    this.fixType = getInt(fields, "fix_type");
    this.satellitesVisible = getInt(fields, "satellites_visible");
    this.hdop = getDilution(fields, "eph");
    this.vdop = getDilution(fields, "epv");
    this.valid = frame.isValid();
  }

  public int getMessageId() {
    return GPS_RAW_INT;
  }

  public boolean hasValidFix() {
    return fixType >= 2;
  }

  public String getFixTypeName() {
    return switch (fixType) {
      case 0 -> "NO_GPS";
      case 1 -> "NO_FIX";
      case 2 -> "2D";
      case 3 -> "3D";
      case 4 -> "DGPS";
      case 5 -> "RTK_FLOAT";
      case 6 -> "RTK_FIXED";
      case 7 -> "STATIC";
      case 8 -> "PPP";
      default -> "UNKNOWN";
    };
  }

  private int getInt(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return -1;
    }
    return ((Number) value).intValue();
  }

  private double getDilution(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return Double.NaN;
    }

    double raw = ((Number) value).doubleValue();
    if (raw == 65535) {
      return Double.NaN;
    }

    return raw / 100.0;
  }
}