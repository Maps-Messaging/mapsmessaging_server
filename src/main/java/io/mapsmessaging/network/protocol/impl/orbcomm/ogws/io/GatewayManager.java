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

package io.mapsmessaging.network.protocol.impl.orbcomm.ogws.io;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.OrbcommOgwsClient;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.*;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.*;

public class GatewayManager {

  private final Logger logger = LoggerFactory.getLogger(GatewayManager.class);
  private final OrbcommOgwsClient ogwsClient;
  private final IncomingMessageHandler handler;
  private final int pollInterval;
  private ScheduledFuture<?> scheduledFuture;


  private final Map<String, TerminalInfo> knownTerminals;
  @Getter
  private final Queue<ReturnMessage> incomingEvents;
  private String lastMessageUtc;
  @Getter
  private boolean authenticated;

  public GatewayManager(OrbcommOgwsClient ogwsClient, int pollInterval, IncomingMessageHandler handler) {
    this.ogwsClient = ogwsClient;
    this.pollInterval = pollInterval;
    this.handler = handler;
    authenticated = false;
    lastMessageUtc = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    incomingEvents = new ConcurrentLinkedQueue<>();
    knownTerminals = new ConcurrentHashMap<>();
  }

  public void start() {
    try {
      authenticated = ogwsClient.authenticate();
      if (authenticated) {
        GetTerminalsInfoResponse response = ogwsClient.getTerminals();
        if (response != null && response.isSuccess()) {
          for (TerminalInfo terminal : response.getTerminals()) {
            knownTerminals.put(terminal.getPrimeId(), terminal);
          }
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

  public TerminalInfo getTerminal(String id) {
    return knownTerminals.get(id);
  }

  public void pollGateway() {
    scanForIncoming();
    ogwsClient.submitMessage();
  }

  private void scanForIncoming(){
    try {
      FromMobileMessagesResponse response = ogwsClient.getFromMobileMessages(lastMessageUtc);
      if (response != null && response.isSuccess()) {
        if (!response.getMessages().isEmpty()) {
          lastMessageUtc = response.getNextFromUtc();
          incomingEvents.addAll(response.getMessages());
          handler.handleIncomingMessage();
        }
      } else {
        logger.log(OGWS_FAILED_POLL, response != null ? response.getErrorId() : "<null error>");
      }
    } catch (Exception e) {
      logger.log(OGWS_REQUEST_FAILED, e);
    }
  }

  public void sendClientMessage(String primeId, SubmitMessage submitMessage) {
    submitMessage.setDestinationId(primeId);
    try {
      ogwsClient.submitMessage(submitMessage);
    } catch (Throwable e) {
      logger.log(OGWS_REQUEST_FAILED, e);
    }
  }
}
