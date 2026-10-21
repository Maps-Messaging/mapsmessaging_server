/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.network.protocol.impl.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class ConnectKeepAliveTest {

  @Test
  void packFrame_usesConfiguredKeepAlive() throws Exception {
    Connect outbound = new Connect();
    outbound.setSessionId("server-link");
    outbound.setUsername("user");
    outbound.setPassword("password".toCharArray());
    outbound.setKeepAlive(45_000);

    Packet packet = new Packet(ByteBuffer.allocate(256));
    outbound.packFrame(packet);
    packet.flip();

    byte fixedHeader = packet.get();
    long remainingLength = MQTTPacket.readVariableInt(packet);
    Connect inbound = new Connect(fixedHeader, remainingLength, packet);

    assertEquals(45_000, inbound.getKeepAlive());
  }
}
