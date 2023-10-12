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

package io.mapsmessaging.network.protocol.impl.loragateway;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.StreamEndPoint;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;

public class LoRaProtocolEndPoint extends EndPoint {

  private final EndPoint physicalEndPoint;
  private LoRaProtocol protocol;

  LoRaProtocolEndPoint(EndPoint physical) {
    super(1, physical.getServer());
    physicalEndPoint = physical;
    if (physicalEndPoint instanceof StreamEndPoint) {
      ((StreamEndPoint) physicalEndPoint).setStreamHandler(new LoRaStreamHandler(logger));
    }
    jmxParentPath = physicalEndPoint.getJMXTypePath();
  }

  @Override
  public String getProtocol() {
    return "LoRa MQTT-SN";
  }

  void setProtocol(LoRaProtocol protocol) {
    this.protocol = protocol;
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    // Intercept the packet here and add what ever we need to send via the Serial End Point
    return protocol.handlePacket(packet);
  }

  int sendPackedPacket(Packet packet) throws IOException {
    return physicalEndPoint.sendPacket(packet);
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return physicalEndPoint.readPacket(packet);
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return physicalEndPoint.register(selectionKey, runner);
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return null;
  }

  @Override
  public String getName() {
    return "LoRa MQTT-SN";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(LoRaProtocolEndPoint.class);
  }
}
