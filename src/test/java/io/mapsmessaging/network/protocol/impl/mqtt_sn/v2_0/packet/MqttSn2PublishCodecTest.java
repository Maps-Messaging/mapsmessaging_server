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

class MqttSn2PublishCodecTest {

  @Test
  void decodeQoSOneTopicNameAndPayload() throws Exception {
    ByteBuffer bytes = ByteBuffer.wrap(new byte[]{
        12, 3, 0x23, 0, 42, 0, 2, 'a', 'b', 1, 2, 3
    });
    MqttSn2PublishCodec.Publish publish =
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(bytes));
    assertEquals(1, publish.qos());
    assertEquals(42, publish.packetIdentifier());
    assertEquals("ab", publish.topicName());
    assertArrayEquals(new byte[]{1, 2, 3}, publish.payload());
    publish.payload()[0] = 99;
    assertArrayEquals(new byte[]{1, 2, 3}, publish.payload());
  }

  @Test
  void decodeQoSZeroAliasAndPubWos() throws Exception {
    MqttSn2PublishCodec.Publish publish = MqttSn2PublishCodec.decode(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{6, 3, 0, 0, 15, 99})));
    assertEquals(0, publish.packetIdentifier());
    assertEquals(15, publish.topicAlias());
    assertEquals(0, publish.topicType());

    MqttSn2PublishCodec.Publish wos = MqttSn2PublishCodec.decode(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{6, 18, 1, 0, 7, 42})));
    assertTrue(wos.withoutSession());
    assertEquals(7, wos.topicAlias());
  }

  @Test
  void rejectInvalidFlagsLengthsAndAliases() {
    byte[][] frames = {
        {5, 3, 0x0C, 0, 1},
        {5, 3, 1, 0, 0},
        {5, 3, 3, 0, 1},
        {5, 3, 0x20, 0, 1},
        {6, 3, 3, 0, 4, 'a'},
        {5, 18, 0, 0, 1}
    };
    for (byte[] frame : frames) {
      assertThrows(IOException.class, () ->
          MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(ByteBuffer.wrap(frame))));
    }
  }
}
