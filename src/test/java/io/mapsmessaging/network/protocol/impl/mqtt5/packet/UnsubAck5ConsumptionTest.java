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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnsubAck5ConsumptionTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3})
  void consumes_all_reason_codes_and_leaves_next_frame_untouched(int reasonCount) throws Exception {
    byte[] bytes = new byte[4 + reasonCount];
    bytes[1] = 7;
    for (int index = 0; index < reasonCount; index++) {
      bytes[3 + index] = (byte) 0x80;
    }
    bytes[bytes.length - 1] = (byte) 0x55;
    Packet packet = new Packet(ByteBuffer.wrap(bytes));
    UnsubAck5 acknowledgement = new UnsubAck5((byte) 0xB0, 3 + reasonCount, packet);
    assertEquals("MQTTv5 UnsubAck[Packet Id:7]", acknowledgement.toString());
    assertEquals(3 + reasonCount, packet.position());
    assertEquals((byte) 0x55, packet.get());
  }

  @org.junit.jupiter.api.Test
  void truncated_reason_payload_still_fails() {
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{0, 7, 0}));
    assertThrows(java.nio.BufferUnderflowException.class,
        () -> new UnsubAck5((byte) 0xB0, 4, packet));
  }
}
