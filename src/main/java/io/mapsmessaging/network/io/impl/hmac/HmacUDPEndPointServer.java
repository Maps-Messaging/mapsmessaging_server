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
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.HmacConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServer;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.protocol.ProtocolFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Map;

public class HmacUDPEndPointServer extends UDPEndPointServer {

  private final Map<String, NodeSecurity> securityMap;

  public HmacUDPEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url,
      SelectorLoadManager selectorLoadManager, EndPointManagerJMX managerMBean,
                               EndPointServerConfigDTO config) throws SocketException {
    super(inetSocketAddress, protocolFactory, url, selectorLoadManager, managerMBean, config);
    securityMap = new LinkedHashMap<>();
    loadNodeConfig((UdpConfig) config.getEndPointConfig());
  }

  private void loadNodeConfig(UdpConfig udpConfig) {
    for (HmacConfigDTO node : udpConfig.getHmacConfigList()) {
      PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().createPacketIntegrity(node);
      securityMap.put(node.getHost() + ":" + node.getPort(), new NodeSecurity(node.getHost(), node.getPort(), packetIntegrity));
    }
  }

  @Override
  protected UDPEndPoint createEndPoint(InetSocketAddress bonded) throws IOException {
    return new HmacUDPEndPoint(
        bonded,
        selectorLoadManager.allocate(),
        1,
        this,
        authenticationConfig,
        managerMBean,
        securityMap
    );
  }
}
