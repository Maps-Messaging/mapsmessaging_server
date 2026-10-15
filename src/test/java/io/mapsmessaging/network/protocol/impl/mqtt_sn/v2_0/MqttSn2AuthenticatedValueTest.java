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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import java.nio.ByteBuffer;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MqttSn2AuthenticatedValueTest {
  @Test
  void equal_sender_content_is_equal_and_deduplicates() {
    var first = authenticated(new byte[] {1, 2});
    var second = authenticated(new byte[] {1, 2});
    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertEquals(1, new HashSet<>(java.util.List.of(first, second)).size());
    assertEquals(first.toString(), second.toString());
    assertTrue(first.toString().contains("[1, 2]"));
  }

  @Test
  void different_sender_or_plaintext_is_distinct() {
    var first = authenticated(new byte[] {1, 2});
    assertNotEquals(first, authenticated(new byte[] {1, 3}));
    assertNotEquals(first, new MqttSn2ProtectionPolicy.Authenticated(
        ByteBuffer.wrap(new byte[] {9}), new byte[] {1, 2}));
    assertNotEquals(first, null);
    assertNotEquals(first, "other");
  }

  @Test
  void absent_sender_preserves_compatibility_value_semantics() {
    assertEquals(authenticated(null), authenticated(null));
    assertEquals(authenticated(null).hashCode(), authenticated(null).hashCode());
  }

  @Test
  void sender_is_copied_on_construction_and_access() {
    byte[] sender = {1, 2};
    var value = authenticated(sender);
    sender[0] = 9;
    byte[] returned = value.senderIdentifier();
    returned[1] = 9;
    assertArrayEquals(new byte[] {1, 2}, value.senderIdentifier());
  }

  private MqttSn2ProtectionPolicy.Authenticated authenticated(byte[] sender) {
    return new MqttSn2ProtectionPolicy.Authenticated(ByteBuffer.wrap(new byte[] {3, 4}), sender);
  }
}
