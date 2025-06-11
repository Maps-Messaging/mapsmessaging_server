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

package io.mapsmessaging.network.io.impl.dtls;

import io.mapsmessaging.config.Config;
import io.mapsmessaging.config.network.impl.DtlsConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.impl.udp.UDPEndPoint;
import io.mapsmessaging.network.io.impl.udp.UDPEndPointServer;
import io.mapsmessaging.network.io.impl.udp.UDPInterfaceInformation;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.security.ssl.SslHelper;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

public class DTLSEndPointServer extends UDPEndPointServer {

  private final ProtocolFactory protocolFactory;
  private final List<UDPInterfaceInformation> udpInterfaceInformations;
  private final List<DTLSSessionManager> bondedEndPoints;
  private final int port;
  private final SSLContext sslContext;

  public DTLSEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url,
      SelectorLoadManager selectorLoadManager, AcceptHandler acceptHandler, EndPointManagerJMX managerMBean,
                            EndPointServerConfigDTO config) throws IOException {
    super(inetSocketAddress, protocolFactory, url, selectorLoadManager, managerMBean, config);
    this.acceptHandler = acceptHandler;
    this.protocolFactory = protocolFactory;
    DtlsConfig dtls = (DtlsConfig)config.getEndPointConfig();
    sslContext = SslHelper.createContext(dtls.getSslConfig().getContext(), ((Config)dtls.getSslConfig()).toConfigurationProperties(), logger);
    bondedEndPoints = new ArrayList<>();
    port = url.getPort();
    udpInterfaceInformations = createInterfaceList(inetSocketAddress);
  }

  @Override
  public void register() {
    // Not required
  }

  @Override
  public void deregister() {
    // Not required
  }

  @Override
  public void start() throws IOException {
    ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
    for (UDPInterfaceInformation info : udpInterfaceInformations) {
      for (InterfaceAddress interfaceAddress : info.getInterfaces()) {
        InetSocketAddress bonded = new InetSocketAddress(interfaceAddress.getAddress(), port);
        NetworkInterface inetAddress = NetworkInterface.getByInetAddress(interfaceAddress.getAddress());

        UDPEndPoint udpEndPoint = new UDPEndPoint(bonded, selectorLoadManager.allocate(), 1, this, authenticationConfig, managerMBean);
        DTLSSessionManager endPoint = new DTLSSessionManager(udpEndPoint, inetAddress, this, protocolImplFactory, sslContext, acceptHandler, managerMBean);
        bondedEndPoints.add(endPoint);
      }
    }
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(UDPEndPointServer.class.getName() + "_" + url);
  }

  @Override
  public void close() {
    for (DTLSSessionManager endPoint : bondedEndPoints) {
      endPoint.close();
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Not required
  }
}
