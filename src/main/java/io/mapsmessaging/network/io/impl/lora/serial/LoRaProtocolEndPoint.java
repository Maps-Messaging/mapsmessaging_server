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

package io.mapsmessaging.network.io.impl.lora.serial;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.StreamEndPoint;
import io.mapsmessaging.network.protocol.impl.loragateway.LoRaProtocol;
import lombok.Setter;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class LoRaProtocolEndPoint extends EndPoint {

  private static AtomicInteger count = new AtomicInteger(0);
  private final EndPoint physicalEndPoint;

  @Setter
  private LoRaProtocol protocol;

  public LoRaProtocolEndPoint(EndPoint physical) {
    super(count.incrementAndGet(), physical.getServer());
    physicalEndPoint = physical;
    if (physicalEndPoint instanceof StreamEndPoint) {
      ((StreamEndPoint) physicalEndPoint).setStreamHandler(new LoRaStreamHandler(logger));
    }
    jmxParentPath = physicalEndPoint.getJMXTypePath();
    name = "gateway:"+physicalEndPoint.getName();
  }

  @Override
  public String getProtocol() {
    return "LoRaSerial";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return protocol.handlePacket(packet);
  }

  public int sendPackedPacket(Packet packet) throws IOException {
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
  protected Logger createLogger() {
    return LoggerFactory.getLogger(LoRaProtocolEndPoint.class);
  }

}
