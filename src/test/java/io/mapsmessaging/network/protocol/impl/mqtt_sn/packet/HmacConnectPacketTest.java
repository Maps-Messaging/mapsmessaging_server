/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.io.security.impl.signature.AppenderSignatureManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Assertions;

public class HmacConnectPacketTest extends UDPConnectPacketTest {


  private PacketIntegrity integrity;

  PacketTransport createTransport(String host) throws Exception {
    integrity = PacketIntegrityFactory.getInstance().getPacketIntegrity(
        "HmacSHA512",
        new AppenderSignatureManager(),
        "ThisIsATestKey".getBytes()
    );
    return new UDPPacketTransport(
        new InetSocketAddress(host, 0),
        new InetSocketAddress(host, 1885));
  }


  void sendPacket(PacketTransport packetTransport, Packet packet) throws Exception {
    packet = integrity.secure(packet);
    packetTransport.sendPacket(packet);
  }
  void readPacket(PacketTransport packetTransport, Packet packet) throws IOException {
    packetTransport.readPacket(packet);
    packet.flip();
    Assertions.assertTrue(integrity.isSecure(packet));
  }


}
