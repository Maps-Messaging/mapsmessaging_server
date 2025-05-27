package io.mapsmessaging.network.protocol.impl.proxy;

import io.mapsmessaging.network.io.Packet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ProxyProtocolV1 extends ProxyProtocol {

  @Override
  public boolean matches(Packet packet) {
    byte[] prefix = new byte[6];
    packet.getRawBuffer().mark();
    packet.get(prefix);
    packet.getRawBuffer().reset();
    return new String(prefix, StandardCharsets.US_ASCII).equals("PROXY ");
  }

  @Override
  public ProxyProtocolInfo parse(Packet packet) {
    packet.getRawBuffer().mark();
    byte[] lineBytes = new byte[108]; // Max PROXY line length
    int i = 0;
    while (packet.hasRemaining() && i < lineBytes.length) {
      byte b = packet.get();
      lineBytes[i++] = b;
      if (i >= 2 && lineBytes[i - 2] == '\r' && lineBytes[i - 1] == '\n') {
        break;
      }
    }
    String header = new String(lineBytes, 0, i, StandardCharsets.US_ASCII).trim();
    String[] parts = header.split(" ");
    if (parts.length < 6) {
      throw new IllegalArgumentException("Invalid PROXY v1 header: " + header);
    }

    InetSocketAddress source = new InetSocketAddress(parts[2], Integer.parseInt(parts[4]));
    InetSocketAddress destination = new InetSocketAddress(parts[3], Integer.parseInt(parts[5]));
    return new ProxyProtocolInfo(source, destination, "v1");
  }
}
