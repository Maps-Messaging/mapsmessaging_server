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

class MqttSn2SubscriptionCodecTest {

  @Test
  void decodeSubscribeTopicAndOptions() throws Exception {
    MqttSn2SubscriptionCodec.Request request = decode(new byte[]{
        8, 8, (byte) 0xB7, 0, 23, 'a', '/', 'b'
    });
    assertTrue(request.subscribe());
    assertEquals(23, request.packetIdentifier());
    assertEquals("a/b", request.topic());
    assertEquals(1, request.maximumQos());
    assertTrue(request.noLocal());
    assertTrue(request.retainAsPublished());
    assertEquals(1, request.retainHandling());
  }

  @Test
  void decodeUnsubscribeAlias() throws Exception {
    MqttSn2SubscriptionCodec.Request request = decode(
        new byte[]{7, 10, 0, 0, 17, 0, 9});
    assertFalse(request.subscribe());
    assertEquals(17, request.packetIdentifier());
    assertEquals(9, request.topicAlias());
  }

  @Test
  void rejectsInvalidSubscribeAndUnsubscribe() {
    for (byte[] bytes : new byte[][]{
        {5, 8, 3, 0, 1},
        {5, 8, 0, 0, 0},
        {5, 8, 0x0C, 0, 1},
        {5, 10, 4, 0, 1},
        {5, 10, 2, 0, 1},
        {7, 10, 2, 0, 1, 0, 0}
    }) {
      assertThrows(IOException.class, () -> decode(bytes));
    }
  }

  private static MqttSn2SubscriptionCodec.Request decode(byte[] data) throws IOException {
    return MqttSn2SubscriptionCodec.decode(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(data)));
  }
}
