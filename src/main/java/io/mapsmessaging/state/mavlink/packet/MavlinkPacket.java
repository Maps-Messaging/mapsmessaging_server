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

import java.util.Map;

public abstract class MavlinkPacket {

  protected int[] getIntArray(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return new int[0];
    }

    if (value instanceof int[] data) {
      return data.clone();
    }

    if (value instanceof short[] data) {
      int[] result = new int[data.length];
      for (int index = 0; index < data.length; index++) {
        result[index] = data[index] & 0xFFFF;
      }
      return result;
    }

    if (value instanceof byte[] data) {
      int[] result = new int[data.length];
      for (int index = 0; index < data.length; index++) {
        result[index] = data[index] & 0xFF;
      }
      return result;
    }

    if (value instanceof Number number) {
      return new int[] {number.intValue()};
    }

    return new int[0];
  }

  protected int getInt(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return -1;
    }
    return ((Number) value).intValue();
  }

  protected double getDouble(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return Double.NaN;
    }
    return ((Number) value).doubleValue();
  }

  protected double getDilution(Map<String, Object> fields, String key) {
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


  protected long getLong(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return -1L;
    }
    return ((Number) value).longValue();
  }

  protected String getString(Map<String, Object> fields, String key) {
    Object value = fields.get(key);
    if (value == null) {
      return null;
    }

    if (value instanceof byte[] data) {
      StringBuilder stringBuilder = new StringBuilder();
      for (byte datum : data) {
        if (datum == 0) {
          break;
        }
        stringBuilder.append((char) (datum & 0xFF));
      }
      return stringBuilder.toString();
    }

    if (value instanceof int[] data) {
      StringBuilder stringBuilder = new StringBuilder();
      for (int datum : data) {
        if (datum == 0) {
          break;
        }
        stringBuilder.append((char) (datum & 0xFF));
      }
      return stringBuilder.toString();
    }

    return String.valueOf(value);
  }
}
