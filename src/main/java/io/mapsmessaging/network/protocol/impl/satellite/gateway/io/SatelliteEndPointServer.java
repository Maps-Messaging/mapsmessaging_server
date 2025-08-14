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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.io;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.ReturnMessage;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.SubmitMessage;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.protocol.OrbCommOgwsProtocol;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import static io.mapsmessaging.logging.ServerLogMessages.OGWS_EXCEPTION_PROCESSING_MESSAGE;
import static io.mapsmessaging.logging.ServerLogMessages.OGWS_NO_CONFIGURATION_FOUND;

public class SatelliteEndPointServer extends EndPointServer implements IncomingMessageHandler {

  private final ProtocolConfigDTO protocolConfigDTO;

  private final GatewayManager gatewayManager;


  protected SatelliteEndPointServer(AcceptHandler accept, EndPointURL url, EndPointServerConfigDTO config) throws IOException {
    super(accept, url, config);
    protocolConfigDTO = config.getProtocolConfig("satellite");
    if (!(protocolConfigDTO instanceof SatelliteConfigDTO satelliteConfigDTO)) {
      logger.log(OGWS_NO_CONFIGURATION_FOUND);
      throw new IOException("no configuration found");
    }
    gatewayManager = new GatewayManager(satelliteConfigDTO, this);
  }


  @Override
  public void register() throws IOException {
    // These are no ops
  }

  @Override
  public void deregister() throws IOException {
    // These are no ops
  }

  @Override
  public void start() throws IOException {
    gatewayManager.start();
  }

  @Override
  public void close() throws IOException {
    super.close();
    gatewayManager.stop();
  }

  @Override
  protected Logger createLogger(String url) {
    return logger;
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    //
  }

  private SatelliteEndPoint locateOrCreateEndPoint(RemoteDeviceInfo terminalInfo) throws IOException, LoginException {
    for (EndPoint endPoint : getActiveEndPoints()) {
      SatelliteEndPoint satelliteEndPoint = (SatelliteEndPoint) endPoint;
      if (satelliteEndPoint.getTerminalInfo().getUniqueId().equals(terminalInfo.getUniqueId())) {
        return satelliteEndPoint;
      }
    }
    // Wire up a new endpoint and bind the Orbcomm OGWS Protocol to it, this will handle specific events to from this client
    SatelliteEndPoint endPoint = new SatelliteEndPoint(generateID(), this, terminalInfo);
    OrbCommOgwsProtocol protocol = new OrbCommOgwsProtocol(endPoint, protocolConfigDTO);
    endPoint.setBoundProtocol(protocol);
    handleNewEndPoint(endPoint);
    return endPoint;
  }

  @Override
  public void handleIncomingMessage(Queue<ReturnMessage> incomingQueue) {
    while (!incomingQueue.isEmpty()) {
      ReturnMessage event = incomingQueue.poll();
      RemoteDeviceInfo info = gatewayManager.getTerminal(event.getMobileId());
      try {
        SatelliteEndPoint endPoint = locateOrCreateEndPoint(info);
        ((OrbCommOgwsProtocol) endPoint.getBoundProtocol()).handleIncomingMessage(event);
      } catch (IOException | LoginException | ExecutionException e) {
        logger.log(OGWS_EXCEPTION_PROCESSING_MESSAGE);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void sendClientMessage(String primeId, SubmitMessage submitMessage) {
    gatewayManager.sendClientMessage(primeId, submitMessage);
  }
}
