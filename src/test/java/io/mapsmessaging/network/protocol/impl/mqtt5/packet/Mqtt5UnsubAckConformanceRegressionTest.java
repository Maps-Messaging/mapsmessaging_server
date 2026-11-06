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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.UnsubscribeListener5;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class Mqtt5UnsubAckConformanceRegressionTest {

  @Test
  void oneFilterProducesOneSuccessReasonCode() throws Exception {
    Packet encoded = pack(new UnsubAck5(73, new StatusCode[] {StatusCode.SUCCESS}));
    assertArrayEquals(new byte[] {(byte) 0xb0, 0x04, 0, 73, 0, 0}, wire(encoded));
  }

  @Test
  void multiFilterReasonsPreserveOrderAndPacketIdentifier() throws Exception {
    UnsubAck5 original = new UnsubAck5(257,
        new StatusCode[] {StatusCode.SUCCESS, StatusCode.NO_SUBSCRIPTION_EXISTED,
            StatusCode.NOT_AUTHORISED});
    Packet encoded = pack(original);
    byte[] frame = wire(encoded);
    assertArrayEquals(new byte[] {(byte) 0xb0, 0x06, 0x01, 0x01, 0,
        0, 0x11, (byte) 0x87}, frame);
    Packet body = new Packet(ByteBuffer.wrap(frame, 2, frame.length - 2));
    UnsubAck5 restored = new UnsubAck5(frame[0], frame[1], body);
    assertEquals(257, restored.getPacketId());
    assertEquals(List.of(StatusCode.SUCCESS, StatusCode.NO_SUBSCRIPTION_EXISTED,
        StatusCode.NOT_AUTHORISED), restored.getReasonCodes());
  }

  @Test
  void rejectsMissingReasonCodeInIncomingUnsubAck() {
    Packet body = new Packet(ByteBuffer.wrap(new byte[] {0, 73, 0}));
    assertThrows(MalformedException.class, () -> new UnsubAck5((byte) 0xb0, 3, body));
  }

  @Test
  void listenerReturnsOneReasonForEveryUnsubscribeFilter() {
    Session session = mock(Session.class);
    Unsubscribe5 request = new Unsubscribe5(List.of("a/b", "c/d", "e/f"));
    request.setMessageId(77);

    UnsubAck5 response = (UnsubAck5) new UnsubscribeListener5()
        .handlePacket(request, session, null, null);

    assertEquals(77, response.getPacketId());
    assertEquals(3, response.getReasonCodes().size());
    assertEquals(List.of(StatusCode.SUCCESS, StatusCode.SUCCESS, StatusCode.SUCCESS),
        response.getReasonCodes());
    verify(session).removeSubscription("a/b");
    verify(session).removeSubscription("c/d");
    verify(session).removeSubscription("e/f");
  }

  @Test
  void handlesMultiByteVariableLengthHeaderForLargeReasonList() throws Exception {
    StatusCode[] codes = new StatusCode[130];
    java.util.Arrays.fill(codes, StatusCode.SUCCESS);
    Packet encoded = pack(new UnsubAck5(44, codes));
    assertEquals((byte) 0xb0, encoded.get());
    assertEquals(133L, MQTTPacket.readVariableInt(encoded));
  }

  private static Packet pack(UnsubAck5 ack) {
    Packet packet = new Packet(512, false);
    ack.packFrame(packet);
    packet.flip();
    return packet;
  }

  private static byte[] wire(Packet packet) {
    ByteBuffer copy = packet.getRawBuffer().duplicate();
    byte[] bytes = new byte[copy.remaining()];
    copy.get(bytes);
    return bytes;
  }
}
