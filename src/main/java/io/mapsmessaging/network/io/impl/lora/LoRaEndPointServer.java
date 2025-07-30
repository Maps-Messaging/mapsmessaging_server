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

package io.mapsmessaging.network.io.impl.lora;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.config.network.impl.LoRaSerialDeviceConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.lora.device.LoRaChipDevice;
import io.mapsmessaging.network.io.impl.lora.serial.LoRaSerialDevice;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortListener;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class LoRaEndPointServer extends EndPointServer implements SerialPortListener {

  private static final AtomicInteger counter = new AtomicInteger(0);
  private final EndPointManagerJMX managerMBean;
  private final ProtocolFactory protocolFactory;
  private final int gatewayId;
  private LoRaProtocol loRaProtocol = null;
  private SerialConfigDTO serialConfig = null;

  public LoRaEndPointServer(AcceptHandler accept, EndPointURL url, EndPointServerConfigDTO config, EndPointManagerJMX managerMBean) {
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
    if (loRaDevice instanceof LoRaChipDevice) {
      LoRaEndPoint endPoint = new LoRaEndPoint((LoRaChipDevice) loRaDevice, gatewayId, this, managerMBean);
      InetSocketAddress socketAddress = (InetSocketAddress) endPoint.getSocketAddress(0xff);
      InterfaceInformation interfaceInformation = new LoRaInterfaceInformation(endPoint.getDatagramSize(), socketAddress.getAddress());
      protocolImplFactory.create(endPoint, interfaceInformation);
    }
    if (loRaDevice instanceof LoRaSerialDevice) {
      LoRaSerialDeviceConfig serialDeviceConfig = (LoRaSerialDeviceConfig) loRaDevice.getConfig();
      serialConfig = serialDeviceConfig.getSerialConfig();
      String portName = serialConfig.getPort();
      String serialNumber = serialConfig.getSerialNo();
      SerialPort port;
      if (serialNumber != null && !serialNumber.trim().isEmpty()) {
        port = SerialPortScanner.getInstance().addBySerial(serialNumber, this);
      } else {
        port = SerialPortScanner.getInstance().add(portName, this);
      }
      if (port != null) {
        bind(port);
      }
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

  @Override
  public void bind(SerialPort port) throws IOException {
    if (loRaProtocol == null) {
      LoRaDevice loRaDevice = LoRaDeviceManager.getInstance().getDevice(getUrl());
      SerialEndPoint serialEndPoint = new SerialEndPoint(counter.incrementAndGet(), this, port, serialConfig, managerMBean.getTypePath());
      loRaProtocol = new LoRaProtocol(serialEndPoint);
      ((LoRaSerialDevice)loRaDevice).setProtocol(loRaProtocol);
      activeEndPoints.put((long) loRaProtocol.getAddress(), serialEndPoint);
    }
  }

  @Override
  public void unbind(SerialPort port) throws IOException {
    if (loRaProtocol == null) {
      loRaProtocol.getEndPoint().close();
      loRaProtocol = null;
    }
  }
}
