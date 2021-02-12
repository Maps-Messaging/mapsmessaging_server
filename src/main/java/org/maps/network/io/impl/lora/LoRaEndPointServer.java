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

package org.maps.network.io.impl.lora;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointManagerJMX;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.EndPointServer;
import org.maps.network.io.InterfaceInformation;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;
import org.maps.network.io.impl.lora.device.LoRaDevice;
import org.maps.network.io.impl.lora.device.LoRaDeviceManager;
import org.maps.network.protocol.ProtocolFactory;
import org.maps.network.protocol.ProtocolImplFactory;

public class LoRaEndPointServer extends EndPointServer {
  private final EndPointManagerJMX managerMBean;

  private final ProtocolFactory protocolFactory;
  private final int gatewayId;

  public LoRaEndPointServer(AcceptHandler accept, EndPointURL url, NetworkConfig config, EndPointManagerJMX managerMBean)  {
    super(accept, url, config);
    protocolFactory = new ProtocolFactory(config.getProtocols());
    gatewayId = url.getPort();
    this.managerMBean = managerMBean;
  }

  @Override
  public void register() {
    // There is nothing to be done here
  }

  @Override
  public void deregister() {
    // There is nothing to be done here
  }

  @Override
  public void start() throws IOException {
    ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
    LoRaDevice loRaDevice = LoRaDeviceManager.getInstance().getDevice(getUrl());
    if(loRaDevice != null) {
      LoRaEndPoint endPoint = new LoRaEndPoint(loRaDevice, gatewayId, this, managerMBean);
      InetSocketAddress socketAddress = (InetSocketAddress) endPoint.getSocketAddress(0xff);
      InterfaceInformation interfaceInformation = new LoRaInterfaceInformation(endPoint.getDatagramSize(), socketAddress.getAddress());
      protocolImplFactory.create(endPoint, interfaceInformation);
    }
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(LoRaEndPointServer.class);
  }

  @Override
  public void close() {
    // There is nothing to be done here
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // There is nothing to be done here
  }
}
