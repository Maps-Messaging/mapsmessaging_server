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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

/**
 * CSD01 3.6.1 and 4.3.1: PUBWOS does not require a virtual connection,
 * carries no packet identifier, and is delivered at QoS 0 without a reply.
 */
class MqttSn2PubwosConformanceTest {

  private static MqttSn2PublishCodec.Publish pubwos(int type, int alias, String topic) {
    return new MqttSn2PublishCodec.Publish(true, 0, false, false,
        type, 0, topic, alias, new byte[]{1, 2, 3});
  }

  private static MqttSn2PublishCodec.Publish roundTrip(MqttSn2PublishCodec.Publish publish)
      throws IOException {
    return MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(MqttSn2PublishCodec.encode(publish)));
  }

  @Test void happyNameTopicWithoutSessionHasNoPacketIdentifier() throws Exception {
    MqttSn2PublishCodec.Publish decoded = roundTrip(pubwos(3, 0, "sensors/room/temperature"));
    assertTrue(decoded.withoutSession());
    assertEquals(0, decoded.qos());
    assertEquals(0, decoded.packetIdentifier());
    assertEquals("sensors/room/temperature", decoded.topicName());
    assertArrayEquals(new byte[]{1, 2, 3}, decoded.payload());
    assertEquals(MqttSn2PacketType.PUBWOS,
        MqttSn2FrameCodec.decode(MqttSn2PublishCodec.encode(decoded)).type());
  }

  @Test void happyPredefinedAliasWithoutSessionRoundTrips() throws Exception {
    MqttSn2PublishCodec.Publish decoded = roundTrip(pubwos(1, 27, null));
    assertEquals(1, decoded.topicType());
    assertEquals(27, decoded.topicAlias());
    assertTrue(decoded.withoutSession());
  }

  @Test void happyRetainFlagRoundTrips() throws Exception {
    MqttSn2PublishCodec.Publish request = new MqttSn2PublishCodec.Publish(
        true, 0, false, true, 3, 0, "retained/topic", 0, new byte[0]);
    assertTrue(roundTrip(request).retained());
  }

  @Test void sadRejectsSessionAliasAndReservedType() {
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2PublishCodec.encode(pubwos(0, 1, null)));
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2PublishCodec.encode(pubwos(2, 1, null)));
  }

  @Test void sadRejectsQosOrDuplicateOnPubwos() {
    assertThrows(IllegalArgumentException.class, () -> MqttSn2PublishCodec.encode(
        new MqttSn2PublishCodec.Publish(true, 1, false, false, 3, 0, "topic", 0, new byte[0])));
    assertThrows(IllegalArgumentException.class, () -> MqttSn2PublishCodec.encode(
        new MqttSn2PublishCodec.Publish(true, 0, true, false, 3, 0, "topic", 0, new byte[0])));
  }

  @Test void murphyInvalidFlagsRejectedOnDecode() throws Exception {
    byte[] bytes = bytes(MqttSn2PublishCodec.encode(pubwos(3, 0, "topic")));
    bytes[2] |= (byte) 0x80; // PUBWOS duplicate bit is forbidden.
    assertThrows(IOException.class, () ->
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes))));
  }

  @Test void murphyTruncatedTopicNameRejected() throws Exception {
    byte[] bytes = bytes(MqttSn2PublishCodec.encode(pubwos(3, 0, "topic")));
    // A valid frame whose inner name-length claims more bytes than remain.
    bytes[3] = 0;
    bytes[4] = 100;
    assertThrows(IOException.class, () ->
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes))));
  }

  @Test void murphyDecoderDoesNotAllowZeroAlias() throws Exception {
    byte[] bytes = bytes(MqttSn2PublishCodec.encode(pubwos(1, 9, null)));
    bytes[3] = 0;
    bytes[4] = 0;
    assertThrows(IOException.class, () ->
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(bytes))));
  }

  private static byte[] bytes(ByteBuffer buffer) {
    ByteBuffer copy = buffer.asReadOnlyBuffer();
    byte[] output = new byte[copy.remaining()];
    copy.get(output);
    return output;
  }
}
