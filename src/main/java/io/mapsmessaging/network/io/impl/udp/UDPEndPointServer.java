/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.udp;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.io.security.PacketIntegrity;
import io.mapsmessaging.network.io.security.PacketIntegrityFactory;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class UDPEndPointServer extends EndPointServer {

  private final EndPointManagerJMX managerMBean;
  private final SelectorLoadManager selectorLoadManager;
  private final ProtocolFactory protocolFactory;
  private final String authenticationConfig;
  private final List<UDPInterfaceInformation> udpInterfaceInformations;
  private final List<UDPEndPoint> bondedEndPoints;
  private final int port;

  public UDPEndPointServer(InetSocketAddress inetSocketAddress, ProtocolFactory protocolFactory, EndPointURL url, SelectorLoadManager selectorLoadManager,
      EndPointManagerJMX managerMBean, NetworkConfig config)
      throws SocketException {
    super(null, url, config);
    this.managerMBean = managerMBean;
    this.selectorLoadManager = selectorLoadManager;
    this.protocolFactory = protocolFactory;
    bondedEndPoints = new ArrayList<>();
    authenticationConfig = config.getAuthConfig();
    port = url.getPort();
    udpInterfaceInformations = createInterfaceList(inetSocketAddress);
  }

  protected List<UDPInterfaceInformation> createInterfaceList(InetSocketAddress inetSocketAddress) throws SocketException {
    List<UDPInterfaceInformation> result = new ArrayList<>();

    NetworkInterface inetAddress = NetworkInterface.getByInetAddress(inetSocketAddress.getAddress());
    if (inetAddress != null) {
      result.add(new UDPInterfaceInformation(inetAddress));
    } else {
      Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
      while (enumeration.hasMoreElements()) {
        NetworkInterface networkInterface = enumeration.nextElement();
        if (!networkInterface.getInterfaceAddresses().isEmpty()) {
          result.add(new UDPInterfaceInformation(networkInterface));
        }
      }
    }
    return result;
  }

  @Override
  public void register() {
    // Not required
  }

  @Override
  public void deregister()  {
    // Not required
  }

  @Override
  public void start() throws IOException {
    ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
    for (UDPInterfaceInformation info : udpInterfaceInformations) {
      for (InterfaceAddress interfaceAddress : info.getInterfaces()) {
        InetSocketAddress bonded = new InetSocketAddress(interfaceAddress.getAddress(), port);
        PacketIntegrity packetIntegrity = PacketIntegrityFactory.getInstance().createPacketIntegrity( getConfig().getProperties() );
        UDPEndPoint endPoint;
        if(packetIntegrity != null){
          endPoint = new HmacUDPEndPoint(
              bonded,
              selectorLoadManager.allocate(),
              1,
              this,
              authenticationConfig,
              managerMBean,
              packetIntegrity
          );
        }
        else{
          endPoint = new UDPEndPoint(
              bonded,
              selectorLoadManager.allocate(),
              1,
              this,
              authenticationConfig,
              managerMBean
          );
        }
        UDPInterfaceInformation nInfo = new UDPInterfaceInformation(info, interfaceAddress.getBroadcast());
        protocolImplFactory.create(endPoint, nInfo);
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
    for (UDPEndPoint endPoint : bondedEndPoints) {
      endPoint.close();
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Not required
  }
}
