package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public class HmacUDPEndPoint extends UDPEndPoint {

  private final Map<String, NodeSecurity> securityMap;

  public HmacUDPEndPoint(
      InetSocketAddress inetSocketAddress,
      Selector selector,
      long id,
      EndPointServer server,
      String authConfig,
      EndPointManagerJMX managerMBean,
      Map<String, NodeSecurity> securityMap
  ) throws IOException {
    super(inetSocketAddress, selector, id, server, authConfig, managerMBean);
    this.securityMap = securityMap;
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if(packetIntegrity == null){
      packet.clear();
      return 0;
//      throw new IOException("No HMAC configuration found for "+packet.getFromAddress());
    }
    packet = packetIntegrity.secure(packet);
    return super.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int res = super.readPacket(packet);
    packet.flip();
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if(packetIntegrity == null){
      packet.clear();
      res = 0;
//      throw new IOException("No HMAC configuration found for "+packet.getFromAddress());
    }
    else {
      if (packet.hasRemaining()) {
        if (!packetIntegrity.isSecure(packet)) {
          packet.clear();
          return 0;
        }
      }
    }
    return res;
  }

  @Override
  public String getProtocol() {
    return "hmac";
  }

  private PacketIntegrity lookup(InetSocketAddress address){
    if(address == null){
      return null;
    }
    String specific = address.getHostName()+":"+address.getPort();
    NodeSecurity lookup = securityMap.get(specific);
    if(lookup != null){
      return lookup.getPacketIntegrity();
    }
    lookup = securityMap.get(address.getHostName()+":0");
    if(lookup != null){
      return lookup.getPacketIntegrity();
    }
    return null;
  }


}
