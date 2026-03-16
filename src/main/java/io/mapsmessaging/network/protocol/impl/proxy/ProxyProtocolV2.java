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

package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ProxyProtocolV2 extends ProxyProtocol {

  private static final byte[] SIGNATURE = {
      0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  private static final int CMD_LOCAL = 0x0;
  private static final int CMD_PROXY = 0x1;

  private static final int FAMILY_UNSPEC = 0x0;
  private static final int FAMILY_INET4 = 0x1;
  private static final int FAMILY_INET6 = 0x2;

  private static final int TRANSPORT_UNSPEC = 0x0;
  private static final int TRANSPORT_STREAM = 0x1; // TCP
  private static final int TRANSPORT_DGRAM = 0x2;  // UDP

  private static final int MAX_PROXY_DATA_LENGTH = 512;


  @Override
  public boolean matches(Packet packet) {
    int remaining = packet.getRawBuffer().remaining();
    if (remaining == 0) {
      return false;
    }

    int toRead = Math.min(remaining, SIGNATURE.length);

    packet.getRawBuffer().mark();
    byte[] prefix = new byte[toRead];
    packet.get(prefix);
    packet.getRawBuffer().reset();

    for (int i = 0; i < toRead; i++) {
      if (prefix[i] != SIGNATURE[i]) {
        return false;
      }
    }

    if (remaining < SIGNATURE.length) {
      throw new BufferUnderflowException();
    }

    return true;
  }

  @Override
  public ProxyProtocolInfo parse(Packet packet) throws ProxyProtocolParseException {
    try {
      if (packet.getRawBuffer().remaining() < 16) {
        throw new BufferUnderflowException();
      }

      int startPos = packet.position();
      packet.position(startPos + 12); // Skip signature

      byte verCmd = packet.get();
      byte protoFam = packet.get();

      int len = Short.toUnsignedInt(packet.getRawBuffer().getShort());

      int version = (verCmd >> 4) & 0x0F;
      if (version != 0x2) {
        throw new ProxyProtocolParseException("Not PROXY protocol v2 (bad version nibble)");
      }

      if (len > MAX_PROXY_DATA_LENGTH) {
        throw new ProxyProtocolParseException("PROXY v2 length too large: " + len);
      }

      int command = verCmd & 0x0F;
      int transport = (protoFam >> 4) & 0x0F;
      int family = protoFam & 0x0F;

      if (packet.getRawBuffer().remaining() < len) {
        throw new BufferUnderflowException();
      }

      ByteBuffer data = packet.getRawBuffer().slice();
      data.limit(len);

      packet.position(packet.position() + len); // Advance past PROXY data (addresses + TLVs)

      if (command == CMD_LOCAL) {
        return new ProxyProtocolInfo(null, null, "local");
      }

      if (command != CMD_PROXY) {
        throw new ProxyProtocolParseException("Unsupported PROXY v2 command: " + command);
      }

      if (family == FAMILY_UNSPEC) {
        return new ProxyProtocolInfo(null, null, buildVersionString(transport, family));
      }

      InetSocketAddress source;
      InetSocketAddress destination;

      if (family == FAMILY_INET4) {
        int required = 4 + 4 + 2 + 2;
        if (data.remaining() < required) {
          throw new BufferUnderflowException();
        }

        byte[] src = new byte[4];
        byte[] dst = new byte[4];
        data.get(src);
        data.get(dst);

        int srcPort = Short.toUnsignedInt(data.getShort());
        int dstPort = Short.toUnsignedInt(data.getShort());

        source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
        destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
      } else if (family == FAMILY_INET6) {
        int required = 16 + 16 + 2 + 2;
        if (data.remaining() < required) {
          throw new BufferUnderflowException();
        }

        byte[] src = new byte[16];
        byte[] dst = new byte[16];
        data.get(src);
        data.get(dst);

        int srcPort = Short.toUnsignedInt(data.getShort());
        int dstPort = Short.toUnsignedInt(data.getShort());

        source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
        destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
      } else {
        throw new ProxyProtocolParseException("Unsupported PROXY v2 address family: " + family);
      }

      return new ProxyProtocolInfo(source, destination, buildVersionString(transport, family));
    } catch (BufferUnderflowException e) {
      throw e;
    } catch (UnknownHostException e) {
      throw new ProxyProtocolParseException("Failed to parse PROXY v2 addresses", e);
    } catch (ProxyProtocolParseException e) {
      throw e;
    } catch (Exception e) {
      throw new ProxyProtocolParseException("Failed to parse PROXY v2 header", e);
    }
  }

  private String buildVersionString(int transport, int family) {
    String transportString;
    if (transport == TRANSPORT_STREAM) {
      transportString = "tcp";
    } else if (transport == TRANSPORT_DGRAM) {
      transportString = "udp";
    } else if (transport == TRANSPORT_UNSPEC) {
      transportString = "unspec";
    } else {
      transportString = "t" + transport;
    }

    String familyString;
    if (family == FAMILY_INET4) {
      familyString = "4";
    } else if (family == FAMILY_INET6) {
      familyString = "6";
    } else if (family == FAMILY_UNSPEC) {
      familyString = "";
    } else {
      familyString = "f" + family;
    }

    return transportString + familyString;
  }
}
