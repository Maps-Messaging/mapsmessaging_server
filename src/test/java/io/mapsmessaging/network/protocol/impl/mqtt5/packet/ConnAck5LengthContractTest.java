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
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnAck5LengthContractTest {

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4})
  void declared_length_preserves_strict_short_and_tolerated_long_handling(int length) {
    Packet packet = new Packet(ByteBuffer.wrap(new byte[]{0, 0, 0}));
    if (length == 2) {
      assertThrows(MalformedException.class, () -> new ConnAck5((byte) 0x20, length, packet));
    } else {
      assertDoesNotThrow(() -> new ConnAck5((byte) 0x20, length, packet));
    }
  }
}
