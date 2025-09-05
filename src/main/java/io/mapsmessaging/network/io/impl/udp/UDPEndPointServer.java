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

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.NetworkInfoHelper;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class UDPEndPointServer extends EndPointServer {

  protected final EndPointManagerJMX managerMBean;
  protected final SelectorLoadManager selectorLoadManager;
  private final ProtocolFactory protocolFactory;
  protected final String authenticationConfig;
  private final List<UDPInterfaceInformation> udpInterfaceInformations;
  private final List<UDPEndPoint> bondedEndPoints;
  private final int port;

  public UDPEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url, SelectorLoadManager selectorLoadManager,
      EndPointManagerJMX managerMBean, EndPointServerConfigDTO config)
      throws SocketException {
    super(null, url, config);
    this.managerMBean = managerMBean;
    this.selectorLoadManager = selectorLoadManager;
    this.protocolFactory = protocolFactory;
    bondedEndPoints = new ArrayList<>();
    authenticationConfig = config.getAuthenticationRealm();
    port = url.getPort();
    udpInterfaceInformations = createInterfaceList(inetSocketAddress);
  }

  protected List<UDPInterfaceInformation> createInterfaceList(InetSocketAddress inetSocketAddress) throws SocketException {
    NetworkInterface inetAddress = NetworkInterface.getByInetAddress(inetSocketAddress.getAddress());
    if (inetAddress != null) {
      List<UDPInterfaceInformation> result = new ArrayList<>();
      result.add(new UDPInterfaceInformation(inetAddress));
      return result;
    }
    return NetworkInfoHelper.createInterfaceList();
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
        UDPEndPoint endPoint = createEndPoint(bonded);
        UDPInterfaceInformation nInfo = new UDPInterfaceInformation(info, interfaceAddress.getBroadcast());
        protocolImplFactory.create(endPoint, nInfo);
        bondedEndPoints.add(endPoint);
      }
    }
  }


  protected UDPEndPoint createEndPoint(InetSocketAddress bonded) throws IOException {
    return new UDPEndPoint(
        bonded,
        selectorLoadManager.allocate(),
        1,
        this,
        authenticationConfig,
        managerMBean
    );
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(UDPEndPointServer.class.getName() + "_" + url);
  }

  @Override
  public void close() {
    for (UDPEndPoint endPoint : bondedEndPoints) {
      try {
        endPoint.close();
      } catch (IOException e) {
        // We can ignore since we are closing
      }
    }
  }


  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    activeEndPoints.put(endPoint.getId(), endPoint);
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Not required
  }
}
