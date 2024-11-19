/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

public class UDPPacketTransport implements PacketTransport {

  private final DatagramChannel datagramChannel;
  private final InetSocketAddress serverAddress;

  public UDPPacketTransport(InetSocketAddress clientAddress, InetSocketAddress serverAddress) throws IOException {
    this.serverAddress = serverAddress;
    datagramChannel = DatagramChannel.open();
    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    datagramChannel.configureBlocking(true);
    datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
    datagramChannel.socket().bind(clientAddress);
    datagramChannel.socket().setSoTimeout(15000);
  }

  public void close() throws IOException {
    datagramChannel.close();
  }

  public int readPacket(Packet packet) throws IOException {
    int pos = packet.position();
    datagramChannel.receive(packet.getRawBuffer());
    return packet.position() - pos;
  }

  public int sendPacket(Packet packet) throws IOException {
    return datagramChannel.send(packet.getRawBuffer(), serverAddress);
  }
}
