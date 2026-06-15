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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MavlinkPacketTest {

  private final TestPacket packet = new TestPacket();

  @Test
  void getIntArray_convertsUnsignedByteAndShortArrays() {
    Map<String, Object> fields = Map.of(
        "bytes", new byte[] {-1, 0, 127},
        "shorts", new short[] {-1, 0, 32767}
    );

    assertArrayEquals(new int[] {255, 0, 127}, packet.intArray(fields, "bytes"));
    assertArrayEquals(new int[] {65535, 0, 32767}, packet.intArray(fields, "shorts"));
  }

  @Test
  void getIntArray_clonesIntArrayAndConvertsNumber() {
    int[] original = {1, 2, 3};
    Map<String, Object> fields = Map.of("array", original, "number", 42L);

    int[] result = packet.intArray(fields, "array");

    assertArrayEquals(original, result);
    assertNotSame(original, result);
    assertArrayEquals(new int[] {42}, packet.intArray(fields, "number"));
  }

  @Test
  void getIntArray_missingOrUnsupportedValue_returnsEmptyArray() {
    assertArrayEquals(new int[0], packet.intArray(Map.of(), "missing"));
    assertArrayEquals(new int[0], packet.intArray(Map.of("value", "unsupported"), "value"));
  }

  @Test
  void numericHelpers_missingValues_returnSentinels() {
    Map<String, Object> fields = Map.of();

    assertEquals(-1, packet.integer(fields, "missing"));
    assertEquals(-1L, packet.longValue(fields, "missing"));
    assertTrue(Double.isNaN(packet.doubleValue(fields, "missing")));
    assertTrue(Double.isNaN(packet.dilution(fields, "missing")));
  }

  @Test
  void getDilution_scalesValidValueAndTreatsUnknownSentinelAsNaN() {
    Map<String, Object> fields = Map.of("valid", 123, "unknown", 65535);

    assertEquals(1.23d, packet.dilution(fields, "valid"), 0.000001d);
    assertTrue(Double.isNaN(packet.dilution(fields, "unknown")));
  }

  @Test
  void getString_decodesUnsignedArraysAndStopsAtNullTerminator() {
    Map<String, Object> fields = new HashMap<>();
    fields.put("bytes", new byte[] {'A', (byte) 0xFF, 0, 'B'});
    fields.put("ints", new int[] {'C', 0x144, 0, 'D'});

    assertEquals("A\u00ff", packet.string(fields, "bytes"));
    assertEquals("CD", packet.string(fields, "ints"));
    assertNull(packet.string(fields, "missing"));
  }

  private static final class TestPacket extends MavlinkPacket {

    int[] intArray(Map<String, Object> fields, String key) {
      return getIntArray(fields, key);
    }

    int integer(Map<String, Object> fields, String key) {
      return getInt(fields, key);
    }

    long longValue(Map<String, Object> fields, String key) {
      return getLong(fields, key);
    }

    double doubleValue(Map<String, Object> fields, String key) {
      return getDouble(fields, key);
    }

    double dilution(Map<String, Object> fields, String key) {
      return getDilution(fields, key);
    }

    String string(Map<String, Object> fields, String key) {
      return getString(fields, key);
    }
  }
}
