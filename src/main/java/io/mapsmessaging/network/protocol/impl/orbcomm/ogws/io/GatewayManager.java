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

import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.OrbcommOgwsClient;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.*;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class GatewayManager {

  private final OrbcommOgwsClient ogwsClient;
  private final IncomingMessageHandler handler;
  private final int pollInterval;


  private final Map<String, TerminalInfo> knownTerminals;
  private String lastMessageUtc;

  @Getter
  private boolean authenticated;
  @Getter
  private final Queue<ReturnMessage> incomingEvents;

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
      if(authenticated) {
        GetTerminalsInfoResponse response = ogwsClient.getTerminals();
        if(response != null && response.isSuccess()) {
          for(TerminalInfo terminal : response.getTerminals()) {
            knownTerminals.put(terminal.getPrimeId(), terminal);
          }
        }
        SimpleTaskScheduler.getInstance().schedule(this::pollGateway, pollInterval, TimeUnit.SECONDS);
        // Log an issue here
      }
    } catch (IOException e) {
      // log the issue
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public TerminalInfo getTerminal(String id) {
    return knownTerminals.get(id);
  }

  public void pollGateway() {
    try {
      FromMobileMessagesResponse response = ogwsClient.getFromMobileMessages(lastMessageUtc);
      if(response != null && response.isSuccess()){
        lastMessageUtc = response.getNextFromUtc();
        incomingEvents.addAll(response.getMessages());
        handler.handleIncomingMessage();
      }
      else{
        // log the error
      }
    } catch (Exception e) {
      // log it and continue
    }
  }

  public void sendClientMessage(String primeId, CommonMessage commonMessage) {
    SubmitMessage submitMessage = new SubmitMessage();
    submitMessage.setPayload(commonMessage);
    submitMessage.setDestinationId(primeId);
    try {
      ogwsClient.submitMessage(List.of(submitMessage) );
    } catch (Exception e) {
      // log this
    }
  }
}
