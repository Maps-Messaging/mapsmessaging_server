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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2OutboundPacketTest {

  @Test
  void sendsExactV2BytesAndDestinationWithoutUsingLegacyPackets() {
    InetSocketAddress destination = new InetSocketAddress("127.0.0.1", 1884);
    AtomicInteger completions = new AtomicInteger();
    ByteBuffer wire = MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGRESP,
        ByteBuffer.wrap(new byte[]{0, 42}));
    MqttSn2OutboundPacket outbound = new MqttSn2OutboundPacket(wire, destination,
        completions::incrementAndGet);
    Packet packet = new Packet(4, false);
    assertEquals(4, outbound.packFrame(packet));
    packet.flip();
    assertEquals(destination, outbound.getFromAddress());
    assertEquals(destination, packet.getFromAddress());
    assertArrayEquals(new byte[]{4, 13, 0, 42}, packet.getRawBuffer().array());
    outbound.complete();
    assertEquals(1, completions.get());
  }

  @Test
  void refusesUndersizedOutputBuffer() {
    MqttSn2OutboundPacket outbound = new MqttSn2OutboundPacket(
        ByteBuffer.wrap(new byte[]{4, 13, 0, 42}),
        new InetSocketAddress("127.0.0.1", 1884), null);
    assertThrows(IllegalArgumentException.class, () ->
        outbound.packFrame(new Packet(2, false)));
  }
}
