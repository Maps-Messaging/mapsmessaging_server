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

import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.charset.StandardCharsets;

public class ProxyProtocolV1 extends ProxyProtocol {

  private static final int MAX_LINE_LENGTH = 108;

  @Override
  public boolean matches(Packet packet) {
    if (packet.getRawBuffer().remaining() < 6) {
      throw new BufferUnderflowException();
    }

    byte[] prefix = new byte[6];
    packet.getRawBuffer().mark();
    packet.get(prefix);
    packet.getRawBuffer().reset();
    return new String(prefix, StandardCharsets.US_ASCII).equals("PROXY ");
  }

  @Override
  public ProxyProtocolInfo parse(Packet packet) throws ProxyProtocolParseException {
    byte[] lineBytes = scanForCrLf(packet);
    return parseRequest(lineBytes);
  }

  private ProxyProtocolInfo parseRequest(byte[] lineBytes) throws ProxyProtocolParseException {
    String header = new String(lineBytes, StandardCharsets.US_ASCII);
    String[] parts = header.split("\\s+");
    if (parts.length < 2) {
      throw new ProxyProtocolParseException("Invalid PROXY v1 header: " + header);
    }

    if (!"PROXY".equals(parts[0])) {
      throw new ProxyProtocolParseException("Invalid PROXY v1 header: " + header);
    }

    String protocolToken = parts[1];

    if ("UNKNOWN".equals(protocolToken)) {
      return new ProxyProtocolInfo(null, null, "v1");
    }

    boolean protocolSupported = "TCP4".equals(protocolToken) || "TCP6".equals(protocolToken);
    if (!protocolSupported) {
      throw new ProxyProtocolParseException("Invalid PROXY v1 header: unsupported protocol " + protocolToken);
    }

    if (parts.length < 6) {
      throw new ProxyProtocolParseException("Invalid PROXY v1 header: " + header);
    }

    String sourceAddress = parts[2];
    String destinationAddress = parts[3];

    int sourcePort;
    int destinationPort;

    try {
      sourcePort = Integer.parseInt(parts[4]);
      destinationPort = Integer.parseInt(parts[5]);
    } catch (NumberFormatException e) {
      throw new ProxyProtocolParseException("Invalid PROXY v1 header: invalid port in " + header, e);
    }

    InetSocketAddress source = new InetSocketAddress(sourceAddress, sourcePort);
    InetSocketAddress destination = new InetSocketAddress(destinationAddress, destinationPort);

    return new ProxyProtocolInfo(source, destination, "v1");
  }

  private byte[] scanForCrLf(Packet packet) throws ProxyProtocolParseException {
    int bytesRead = 0;
    byte[] lineBytes = new byte[MAX_LINE_LENGTH];

    while (bytesRead < lineBytes.length) {
      if (!packet.hasRemaining()) {
        throw new BufferUnderflowException();
      }

      byte value = packet.get();
      lineBytes[bytesRead] = value;
      bytesRead++;

      if (bytesRead >= 2) {
        boolean isCrlf = lineBytes[bytesRead - 2] == '\r' && lineBytes[bytesRead - 1] == '\n';
        if (isCrlf) {
          int headerLength = bytesRead - 2;
          byte[] request = new byte[headerLength];
          System.arraycopy(lineBytes, 0, request, 0, headerLength);
          return request;
        }
      }
    }

    throw new ProxyProtocolParseException("Invalid PROXY v1 header: missing CRLF or exceeds max length");
  }
}
