package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;

public class UDPPacketTransport implements Closeable {

  private final DatagramChannel datagramChannel;


  public UDPPacketTransport(InetSocketAddress inetSocketAddress) throws IOException {
    datagramChannel = DatagramChannel.open();
    datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    datagramChannel.configureBlocking(false);
    datagramChannel.setOption(StandardSocketOptions.SO_BROADCAST, true);
    datagramChannel.socket().bind(inetSocketAddress);
  }

  public void close() throws IOException {
    datagramChannel.close();
  }

  public int readPacket(Packet packet) throws IOException {
    int pos = packet.position();
    packet.setFromAddress(datagramChannel.receive(packet.getRawBuffer()));
    return packet.position() - pos;
  }

  public int sendPacket(Packet packet) throws IOException {
    return datagramChannel.send(packet.getRawBuffer(), packet.getFromAddress());
  }
}
