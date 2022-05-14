package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import java.io.IOException;
import java.net.InetSocketAddress;

public class HmacUDPEndPoint extends UDPEndPoint {

  private final PacketIntegrity packetIntegrity;

  public HmacUDPEndPoint(
      InetSocketAddress inetSocketAddress,
      Selector selector,
      long id,
      EndPointServer server,
      String authConfig,
      EndPointManagerJMX managerMBean,
      PacketIntegrity packetIntegrity
  ) throws IOException {
    super(inetSocketAddress, selector, id, server, authConfig, managerMBean);
    this.packetIntegrity = packetIntegrity;
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    packet = packetIntegrity.secure(packet);
    return super.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int res = super.readPacket(packet);
    packet.flip();
    if(packet.hasRemaining()) {
      if (!packetIntegrity.isSecure(packet)) {
        packet.clear();
        return 0;
      }
    }
    return res;
  }

}
