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

package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ProxyProtocolV2 extends ProxyProtocol {

  private static final byte[] SIGNATURE = {
      0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  @Override
  public boolean matches(Packet packet) {
    if (packet.getRawBuffer().remaining() < 12) return false;
    packet.getRawBuffer().mark();
    byte[] sig = new byte[12];
    packet.get(sig);
    packet.getRawBuffer().reset();
    return Arrays.equals(sig, SIGNATURE);
  }

  @Override
  public ProxyProtocolInfo parse(Packet packet) throws UnknownHostException {
    int startPos = packet.position();
    packet.position(startPos + 12); // Skip signature

    byte verCmd = packet.get();
    byte protoFam = packet.get();
    int len = Short.toUnsignedInt(packet.getRawBuffer().getShort());

    if ((verCmd >> 4) != 0x2) {
      throw new IllegalArgumentException("Not PROXY protocol v2");
    }

    int transport = (protoFam >> 4) & 0x0F;
    int family = protoFam & 0x0F;

    ByteBuffer addrBuf = packet.getRawBuffer().slice();
    addrBuf.limit(len);
    packet.position(packet.position() + len); // Advance past PROXY data

    InetSocketAddress source, destination;

    if (family == 0x1) { // IPv4
      byte[] src = new byte[4];
      byte[] dst = new byte[4];
      addrBuf.get(src);
      addrBuf.get(dst);
      int srcPort = Short.toUnsignedInt(addrBuf.getShort());
      int dstPort = Short.toUnsignedInt(addrBuf.getShort());
      source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
      destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
    } else if (family == 0x2) { // IPv6
      byte[] src = new byte[16];
      byte[] dst = new byte[16];
      addrBuf.get(src);
      addrBuf.get(dst);
      int srcPort = Short.toUnsignedInt(addrBuf.getShort());
      int dstPort = Short.toUnsignedInt(addrBuf.getShort());
      source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
      destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
    } else {
      throw new IllegalArgumentException("Unsupported PROXY v2 address family: " + family);
    }

    // Optional: you can include the transport string (e.g., "udp4", "tcp6")
    String version = (transport == 0x2 ? "udp" : "tcp") + (family == 0x1 ? "4" : "6");

    return new ProxyProtocolInfo(source, destination, version);
  }
}
