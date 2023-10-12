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

package io.mapsmessaging.network.io.impl.lora;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDevice;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class LoRaEndPointServer extends EndPointServer {

  private final EndPointManagerJMX managerMBean;

  private final ProtocolFactory protocolFactory;
  private final int gatewayId;

  public LoRaEndPointServer(AcceptHandler accept, EndPointURL url, NetworkConfig config, EndPointManagerJMX managerMBean) {
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
    if (loRaDevice != null) {
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
