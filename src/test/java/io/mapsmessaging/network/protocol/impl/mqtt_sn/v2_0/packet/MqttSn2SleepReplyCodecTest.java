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

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2SleepReplyCodecTest {

  @Test
  void decodesSleepRequestAndEncodesResponse() throws Exception {
    MqttSn2SleepCodec.SleepRequest request = MqttSn2SleepCodec.decodeRequest(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{9, 19, 1, 0, 7, 0, 0, 0, 30})));
    assertTrue(request.retainAliases());
    assertEquals(7, request.packetIdentifier());
    assertEquals(30, request.durationSeconds());

    MqttSn2SleepCodec.SleepResponse reply =
        new MqttSn2SleepCodec.SleepResponse(7, 25L, 0);
    ByteBuffer encoded = MqttSn2SleepCodec.encodeResponse(reply);
    assertEquals(reply, MqttSn2SleepCodec.decodeResponse(MqttSn2FrameCodec.decode(encoded)));
  }

  @Test
  void wakeupHasNoPayload() throws Exception {
    ByteBuffer encoded = MqttSn2SleepCodec.encodeWakeup();
    assertArrayEquals(new byte[]{2, 21}, toBytes(encoded));
    assertNotNull(MqttSn2SleepCodec.decodeWakeup(MqttSn2FrameCodec.decode(encoded)));
    assertThrows(IOException.class, () -> MqttSn2SleepCodec.decodeWakeup(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{3, 21, 1}))));
  }

  @Test
  void subscriptionAcknowledgementRoundTrips() throws Exception {
    MqttSn2ReplyCodec.SubAck ack = new MqttSn2ReplyCodec.SubAck(0, 17, 8, 0);
    ByteBuffer encoded = MqttSn2ReplyCodec.encodeSubAck(ack);
    assertEquals(ack, MqttSn2ReplyCodec.decodeSubAck(MqttSn2FrameCodec.decode(encoded)));
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2ReplyCodec.encodeSubAck(new MqttSn2ReplyCodec.SubAck(3, 1, 1, null)));
  }

  private static byte[] toBytes(ByteBuffer input) {
    ByteBuffer source = input.asReadOnlyBuffer();
    byte[] data = new byte[source.remaining()];
    source.get(data);
    return data;
  }
}
