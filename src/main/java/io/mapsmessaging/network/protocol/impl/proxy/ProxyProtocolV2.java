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

    ByteBuffer addrBuf = packet.getRawBuffer().slice();
    addrBuf.limit(len);
    packet.position(packet.position() + len); // Advance past PROXY data

    InetSocketAddress source, destination;
    switch (protoFam & 0x0F) {
      case 0x1: { // TCP over IPv4
        byte[] src = new byte[4];
        byte[] dst = new byte[4];
        addrBuf.get(src);
        addrBuf.get(dst);
        int srcPort = Short.toUnsignedInt(addrBuf.getShort());
        int dstPort = Short.toUnsignedInt(addrBuf.getShort());
        source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
        destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
        break;
      }
      case 0x2: { // TCP over IPv6
        byte[] src = new byte[16];
        byte[] dst = new byte[16];
        addrBuf.get(src);
        addrBuf.get(dst);
        int srcPort = Short.toUnsignedInt(addrBuf.getShort());
        int dstPort = Short.toUnsignedInt(addrBuf.getShort());
        source = new InetSocketAddress(InetAddress.getByAddress(src), srcPort);
        destination = new InetSocketAddress(InetAddress.getByAddress(dst), dstPort);
        break;
      }
      default:
        throw new IllegalArgumentException("Unsupported PROXY v2 address family: " + protoFam);
    }

    return new ProxyProtocolInfo(source, destination, "v2");
  }
}
