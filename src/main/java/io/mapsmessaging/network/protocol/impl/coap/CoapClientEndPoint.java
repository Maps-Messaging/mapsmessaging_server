/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.FutureTask;
import javax.security.auth.Subject;

public class CoapClientEndPoint extends EndPoint{

  private final Logger logger = LoggerFactory.getLogger(CoapClientEndPoint.class);
  private final EndPoint physicalEndPoint;


  public CoapClientEndPoint (EndPoint endpoint) {
    super(endpoint.getId(), endpoint.getServer());
    physicalEndPoint = endpoint;
  }

  @Override
  public void close() throws IOException {
    physicalEndPoint.close();
  }

  @Override
  public boolean isClient(){
    return physicalEndPoint.isClient();
  }

  @Override
  public EndPointServerConfigDTO getConfig() {
    return physicalEndPoint.getConfig();
  }

  @Override
  public List<String> getJMXTypePath() {
    return physicalEndPoint.getJMXTypePath();
  }

  @Override
  public boolean isUDP() {
    return physicalEndPoint.isUDP();
  }

  @Override
  public Subject getEndPointSubject() {
    return physicalEndPoint.getEndPointSubject();
  }

  @Override
  public Principal getEndPointPrincipal() {
    return physicalEndPoint.getEndPointPrincipal();
  }

  @Override
  public boolean isSSL() {
    return physicalEndPoint.isSSL();
  }


  @Override
  public String getProtocol() {
    return physicalEndPoint.getProtocol();
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
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
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return physicalEndPoint.deregister(selectionKey);
  }

  @Override
  public String getAuthenticationConfig() {
    return physicalEndPoint.getAuthenticationConfig();
  }

  @Override
  public String getName() {
    return physicalEndPoint.getName();
  }

  @Override
  protected Logger createLogger() {
    return logger;
  }
}
