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

class MqttSn2DisconnectEncapsulationTest {

  @Test
  void disconnectRoundTripsOptionalFields() throws Exception {
    MqttSn2DisconnectCodec.Disconnect packet =
        new MqttSn2DisconnectCodec.Disconnect(5, 0, 120L, "closing");
    ByteBuffer encoded = MqttSn2DisconnectCodec.encode(packet);
    assertEquals(packet, MqttSn2DisconnectCodec.decode(MqttSn2FrameCodec.decode(encoded)));
    assertThrows(IOException.class, () -> MqttSn2DisconnectCodec.decode(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{3, 14, (byte) 0x80}))));
  }

  @Test
  void connectionEncapsulationEnforcesAllowedPacketTypes() throws Exception {
    byte[] ping = {4, 12, 0, 1};
    MqttSn2EncapsulationCodec.Connection packet =
        new MqttSn2EncapsulationCodec.Connection("sensor-1", ping);
    ByteBuffer encoded = MqttSn2EncapsulationCodec.encodeConnection(packet);
    MqttSn2EncapsulationCodec.Connection decoded =
        MqttSn2EncapsulationCodec.decodeConnection(MqttSn2FrameCodec.decode(encoded));
    assertEquals("sensor-1", decoded.clientIdentifier());
    assertArrayEquals(ping, decoded.embeddedPacket());

    assertThrows(IOException.class, () -> MqttSn2EncapsulationCodec.encodeConnection(
        new MqttSn2EncapsulationCodec.Connection("x", new byte[]{2, 2})));
  }

  @Test
  void forwarderEncapsulationRoundTripsAndCopiesPayload() throws Exception {
    byte[] address = {4, 5, 6};
    byte[] packet = {2, 21};
    MqttSn2EncapsulationCodec.Forwarder forwarder =
        new MqttSn2EncapsulationCodec.Forwarder(address, packet);
    address[0] = 99;
    ByteBuffer encoded = MqttSn2EncapsulationCodec.encodeForwarder(forwarder);
    MqttSn2EncapsulationCodec.Forwarder decoded =
        MqttSn2EncapsulationCodec.decodeForwarder(MqttSn2FrameCodec.decode(encoded));
    assertArrayEquals(new byte[]{4, 5, 6}, decoded.addressing());
    assertArrayEquals(packet, decoded.embeddedPacket());
  }
}
