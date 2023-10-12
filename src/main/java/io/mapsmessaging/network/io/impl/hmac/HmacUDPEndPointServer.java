/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.hmac;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServer;
import io.mapsmessaging.network.io.security.NodeSecurity;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HmacUDPEndPointServer extends UDPEndPointServer {

  private final Map<String, NodeSecurity> securityMap;

  public HmacUDPEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url,
      SelectorLoadManager selectorLoadManager, EndPointManagerJMX managerMBean,
      NetworkConfig config) throws SocketException {
    super(inetSocketAddress, protocolFactory, url, selectorLoadManager, managerMBean, config);
    securityMap = new LinkedHashMap<>();
    ConfigurationProperties props = getConfig().getProperties();
    Object t = props.get("nodeConfiguration");
    loadNodeConfig((List<ConfigurationProperties>) t);
  }

  private void loadNodeConfig(List<ConfigurationProperties> nodes) {
    for (ConfigurationProperties node : nodes) {
      String host = node.getProperty("host");
      int port = node.getIntProperty("port", 0);
      PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().createPacketIntegrity(node);
      securityMap.put(host + ":" + port, new NodeSecurity(host, port, packetIntegrity));
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
