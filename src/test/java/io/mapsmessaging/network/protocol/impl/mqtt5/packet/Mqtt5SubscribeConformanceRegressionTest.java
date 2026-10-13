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

  // MQTT 5 sections 3.8.2.1.2, 3.8.3.1 and 4.8.2: preserve filter bytes and options.
  @ParameterizedTest
  @ValueSource(strings = {"sensor/温度", "$share/Gr温度/sensor//温度/+", "$SHARE/group/+", "$shareholder/+"})
  void filter_and_subscription_identifier_survive_encoding(String filter) throws Exception {
    Subscribe5 original = parseSubscribe(65535, new byte[] {0x0b, 0x05}, filter, 0x2a);
    Packet encoded = new Packet(512, false);
    original.packFrame(encoded);
    encoded.flip();
    byte fixed = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    assertEquals(encoded.available(), remaining);
    Subscribe5 restored = new Subscribe5(fixed, remaining, encoded);
    var before = original.getSubscriptionList().get(0);
    var after = restored.getSubscriptionList().get(0);
    assertEquals(before.getTopicName(), after.getTopicName());
    assertEquals(before.getSharedSubscriptionName(), after.getSharedSubscriptionName());
    assertEquals(before.getQualityOfService(), after.getQualityOfService());
    assertEquals(before.getRetainHandling(), after.getRetainHandling());
    assertEquals(before.isRetainAsPublished(), after.isRetainAsPublished());
    assertEquals(5, after.getSubscriptionIdentifier().getSubscriptionIdentifier());
    assertEquals(0, encoded.available());
  }

  @Test
  void accepts_every_valid_non_shared_option_combination() throws Exception {
    for (int qos = 0; qos < 3; qos++) {
      for (int retain = 0; retain < 3; retain++) {
        for (int flags = 0; flags < 4; flags++) {
          int options = qos | (retain << 4) | (flags << 2);
          var info = parseSubscribe(1, new byte[0], "温度/+", options)
              .getSubscriptionList().get(0);
          assertEquals(qos, info.getQualityOfService().getLevel());
          assertEquals(retain, info.getRetainHandling().getHandler());
          assertEquals((flags & 1) != 0, info.noLocalMessages());
          assertEquals((flags & 2) != 0, info.isRetainAsPublished());
        }
      }
    }
  }

  @Test
  void maximum_subscription_identifier_is_valid() throws Exception {
    var info = parseSubscribe(65535,
        new byte[] {0x0b, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x7f}, "温度/#", 2)
        .getSubscriptionList().get(0);
    assertEquals(268435455, info.getSubscriptionIdentifier().getSubscriptionIdentifier());
  }

  @ParameterizedTest
  @ValueSource(strings = {"$SHARE/group/+", "$shareholder/+"})
  void similar_prefixes_are_ordinary_filters_and_allow_no_local(String filter) throws Exception {
    var info = parseSubscribe(1, new byte[0], filter, 4).getSubscriptionList().get(0);
    assertFalse(info.isSharedSubscription());
    assertTrue(info.noLocalMessages());
    assertEquals(filter, info.getTopicName());
  }

  // MQTT 5 section 2.2.2.1: Subscription Identifier must stay inside Properties.
  @Test
  void subscription_identifier_cannot_overrun_the_declared_property_length() {
    Packet packet = new Packet(ByteBuffer.wrap(new byte[] {0, 1, 1, 0x0b, 1, 0, 1, 'a', 0}));
    assertThrows(MalformedException.class, () -> new Subscribe5((byte) 0x82, 9, packet));
  }

  @Test
  void invalid_filter_precedes_invalid_options() {
    MalformedException failure = assertThrows(MalformedException.class,
        () -> parseSubscribe(1, new byte[0], "topic+", 0xff));
    assertEquals("Single-level wildcard must occupy a whole level", failure.getMessage());
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
