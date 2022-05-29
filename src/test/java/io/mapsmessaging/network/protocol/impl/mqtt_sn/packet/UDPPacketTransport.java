package io.mapsmessaging.network.protocol.impl.mqtt_sn.packet;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
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
