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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Mqtt5SubscribeConformanceRegressionTest {

  @Test
  void rejectsZeroPacketIdentifier() {
    assertThrows(MalformedException.class, () -> parseSubscribe(0, new byte[0], "test/a", 0));
  }

  @Test
  void rejectsZeroSubscriptionIdentifier() {
    assertThrows(MalformedException.class, () ->
        parseSubscribe(42, new byte[] {0x0b, 0x00}, "test/a", 0));
  }

  @Test
  void rejectsDuplicateSubscriptionIdentifiers() {
    assertThrows(MalformedException.class, () ->
        parseSubscribe(42, new byte[] {0x0b, 0x01, 0x0b, 0x02}, "test/a", 0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "sport/#/ranking", "sport+", "+east/sport",
      "$share//test", "$share/group/", "$share/gr+oup/test"})
  void rejectsMalformedFilters(String filter) {
    assertThrows(MalformedException.class, () ->
        parseSubscribe(42, new byte[0], filter, 0));
  }

  @ParameterizedTest
  @ValueSource(ints = {0x03, 0x30, 0xc0})
  void rejectsInvalidSubscriptionOptions(int options) {
    assertThrows(MalformedException.class, () ->
        parseSubscribe(42, new byte[0], "test/a", options));
  }

  @Test
  void rejectsNoLocalOnSharedSubscription() {
    assertThrows(MalformedException.class, () ->
        parseSubscribe(42, new byte[0], "$share/group/test/a", 0x04));
  }

  @Test
  void preservesValidSharedSubscriptionAndIdentifier() throws Exception {
    Subscribe5 subscribe = parseSubscribe(42,
        new byte[] {0x0b, 0x05}, "$share/group/test/+", 0);
    assertEquals("group", subscribe.getSubscriptionList().get(0).getSharedSubscriptionName());
    assertEquals("test/+", subscribe.getSubscriptionList().get(0).getTopicName());
    SubscriptionIdentifier id = subscribe.getSubscriptionList().get(0).getSubscriptionIdentifier();
    assertEquals(5L, id.getSubscriptionIdentifier());
  }

  @ParameterizedTest
  @ValueSource(strings = {"test/+", "test/#", "test//a", "test/温度", "$SYS/#"})
  void acceptsValidTopicFilters(String filter) throws Exception {
    assertFalse(parseSubscribe(42, new byte[0], filter, 0).getSubscriptionList().isEmpty());
  }

  private static Subscribe5 parseSubscribe(int packetId, byte[] properties, String filter,
                                           int options) throws Exception {
    byte[] utf8 = filter.getBytes(StandardCharsets.UTF_8);
    ByteBuffer bytes = ByteBuffer.allocate(6 + properties.length + utf8.length);
    bytes.putShort((short) packetId);
    bytes.put((byte) properties.length);
    bytes.put(properties);
    bytes.putShort((short) utf8.length);
    bytes.put(utf8);
    bytes.put((byte) options);
    bytes.flip();
    return new Subscribe5((byte) 0x82, bytes.remaining(), new Packet(bytes));
  }
}
