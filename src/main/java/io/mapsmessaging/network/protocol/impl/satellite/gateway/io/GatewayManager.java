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

import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.*;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class GatewayManager {

  private final Logger logger = LoggerFactory.getLogger(GatewayManager.class);
  private final SatelliteClient satelliteClient;
  private final IncomingMessageHandler handler;
  private final int pollInterval;
  private ScheduledFuture<?> scheduledFuture;


  private final Map<String, RemoteDeviceInfo> knownTerminals;

  @Getter
  private boolean authenticated;

  public GatewayManager(SatelliteConfigDTO satelliteConfigDTO, IncomingMessageHandler handler) {
    this.satelliteClient = ClientFactory.createSatelliteClient(satelliteConfigDTO);
    this.pollInterval = satelliteConfigDTO.getPollInterval();
    this.handler = handler;
    authenticated = false;
    knownTerminals = new ConcurrentHashMap<>();
  }

  public void start() {
    try {
      authenticated = satelliteClient.authenticate();
      if (authenticated) {
        List<RemoteDeviceInfo> response = satelliteClient.getTerminals();
        for (RemoteDeviceInfo terminal : response) {
          System.err.println("Found terminal " + terminal);
          knownTerminals.put(terminal.getUniqueId(), terminal);
        }
        scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::pollGateway, pollInterval, pollInterval, TimeUnit.SECONDS);
      } else {
        logger.log(OGWS_FAILED_AUTHENTICATION);
      }
    } catch (IOException e) {
      logger.log(OGWS_FAILED_AUTHENTICATION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void stop(){
    if(scheduledFuture != null){
      scheduledFuture.cancel(true);
      scheduledFuture = null;
    }
  }

  public RemoteDeviceInfo getTerminal(String id) {
    return knownTerminals.get(id);
  }


  public void sendClientMessage(String primeId, SubmitMessage submitMessage) {
    submitMessage.setDestinationId(primeId);
    try {
      satelliteClient.queueMessagesForDelivery(submitMessage);
    } catch (Throwable e) {
      logger.log(OGWS_REQUEST_FAILED, e);
    }
  }

  // timed task to read /write to the remote server
  protected void pollGateway() {
    handler.handleIncomingMessage(satelliteClient.scanForIncoming());
    satelliteClient.processPendingMessages();
  }
}
