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

package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.config.network.impl.UdpConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionManager;
import io.mapsmessaging.network.io.impl.udp.session.UDPSessionState;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HmacUDPEndPoint extends UDPEndPoint {

  private final Map<String, NodeSecurity> securityMap;
  private final UDPSessionManager<PacketIntegrity> cacheMap;

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
    long cacheExpiryTime = ((UdpConfig)getConfig().getEndPointConfig()).getHmacHostLookupCacheExpiry();
    cacheMap = new UDPSessionManager<>(cacheExpiryTime);
  }

  @Override
  public void close() throws IOException {
    super.close();
    cacheMap.close();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if (packetIntegrity == null) {
      packet.clear();
      return 0;
    }
    packet = packetIntegrity.secure(packet);
    return super.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    int res = super.readPacket(packet);
    packet.flip();
    PacketIntegrity packetIntegrity = lookup((InetSocketAddress) packet.getFromAddress());
    if (packetIntegrity == null) {
      packet.clear();
      res = 0;
    } else {
      if (packet.hasRemaining() && !packetIntegrity.isSecure(packet)) {
        packet.clear();
        return 0;
      }
    }
    return res;
  }

  @Override
  public String getProtocol() {
    return "hmac";
  }

  private PacketIntegrity lookup(InetSocketAddress address) {
    if (address == null) {
      return null;
    }
    UDPSessionState<PacketIntegrity> state = cacheMap.getState(address);
    if (state != null) {
      PacketIntegrity packetIntegrity = state.getContext();
      if (packetIntegrity != null) {
        return packetIntegrity;
      }
    }
    List<String> potentialKeys = new ArrayList<>();
    potentialKeys.add(address.getAddress().getHostName() + ":" + address.getPort());
    potentialKeys.add(address.getAddress().getHostName() + ":0");
    potentialKeys.add(address.getAddress().getHostAddress() + ":" + address.getPort());
    potentialKeys.add(address.getAddress().getHostAddress() + ":0");

    for (String key : potentialKeys) {
      PacketIntegrity packetIntegrity = lookup(key);
      if (packetIntegrity != null) {
        cacheMap.addState(address, new UDPSessionState<>(packetIntegrity));
        return packetIntegrity;
      }
    }
    return null;
  }


  private PacketIntegrity lookup(String key) {
    NodeSecurity lookup = securityMap.get(key);
    if (lookup != null) {
      return lookup.getPacketIntegrity();
    }
    return null;
  }
}
