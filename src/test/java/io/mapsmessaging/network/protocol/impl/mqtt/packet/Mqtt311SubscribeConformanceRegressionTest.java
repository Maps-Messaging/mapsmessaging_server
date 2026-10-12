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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Mqtt311SubscribeConformanceRegressionTest {

  @Test
  void rejectsZeroPacketIdentifier() {
    assertThrows(MalformedException.class,
        () -> parseSubscribe(0, "sensor/temperature", 0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "sport/#/ranking", "sport+", "sport/+east",
      "sport/#east", "sport/a#"})
  void rejectsMalformedFilters(String filter) {
    assertThrows(MalformedException.class, () -> parseSubscribe(9, filter, 0));
  }

  @ParameterizedTest
  @ValueSource(ints = {0x04, 0x80, 0xff})
  void rejectsReservedSubscribeOptions(int options) {
    assertThrows(MalformedException.class,
        () -> parseSubscribe(9, "sensor/temp", options));
  }

  @ParameterizedTest
  @ValueSource(strings = {"sensor/+", "sensor/#", "sensor//temp", "+/status",
      "sensor/温度", "$SYS/#"})
  void preservesValidFilters(String filter) throws Exception {
    assertEquals(filter, parseSubscribe(9, filter, 1).getSubscriptionList()
        .get(0).getTopicName());
  }

  private static Subscribe parseSubscribe(int packetId, String filter, int options)
      throws MalformedException {
    byte[] utf8 = filter.getBytes(StandardCharsets.UTF_8);
    ByteBuffer bytes = ByteBuffer.allocate(5 + utf8.length);
    bytes.putShort((short) packetId);
    bytes.putShort((short) utf8.length);
    bytes.put(utf8);
    bytes.put((byte) options);
    bytes.flip();
    return new Subscribe((byte) 0x82, bytes.remaining(), new Packet(bytes));
  }
}
