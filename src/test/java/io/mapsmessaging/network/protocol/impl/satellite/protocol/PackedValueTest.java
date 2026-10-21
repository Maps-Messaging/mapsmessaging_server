/*
 *
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

package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PackedValueTest {
  @Test
  void equal_payload_content_is_equal_and_deduplicates() {
    var first = new MessageQueuePacker.Packed(new byte[] {1, 2}, true, 3);
    var second = new MessageQueuePacker.Packed(new byte[] {1, 2}, true, 3);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertEquals(1, new HashSet<>(java.util.List.of(first, second)).size());
    assertEquals(first.toString(), second.toString());
    assertTrue(first.toString().contains("[1, 2]"));
  }

  @Test
  void different_payload_flags_or_transformer_are_distinct() {
    var first = new MessageQueuePacker.Packed(new byte[] {1, 2}, true, 3);
    assertNotEquals(first, new MessageQueuePacker.Packed(new byte[] {1, 3}, true, 3));
    assertNotEquals(first, new MessageQueuePacker.Packed(new byte[] {1, 2}, false, 3));
    assertNotEquals(first, new MessageQueuePacker.Packed(new byte[] {1, 2}, true, 4));
    assertNotEquals(first, null);
    assertNotEquals(first, "other");
  }

  @Test
  void absent_payload_is_equal_and_printable() {
    var first = new MessageQueuePacker.Packed(null, false, 0);
    var second = new MessageQueuePacker.Packed(null, false, 0);
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertTrue(first.toString().contains("data=null"));
  }
}
