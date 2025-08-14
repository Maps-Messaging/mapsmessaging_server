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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat;

import com.google.gson.JsonObject;
import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MobileOriginatedResponse;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteClient;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;

import java.io.IOException;
import java.util.*;

public class InmarsatClient implements SatelliteClient {

  private final InmarsatSession inmarsatSession;
  private InmarsatSession.MailboxSession mailboxSession;
  private final List<MessageData> pendingMessages;
  private String lastMoTimeUTC;

  public InmarsatClient(SatelliteConfigDTO satelliteConfigDTO) {
    inmarsatSession = new InmarsatSession(satelliteConfigDTO);
    pendingMessages = new ArrayList<>();
    lastMoTimeUTC = null;
  }

  @Override
  public void close(){
  }

  @Override
  public boolean authenticate() throws IOException, InterruptedException {
    mailboxSession  = inmarsatSession.openMailbox();
    return mailboxSession.getMailbox().getEnabled();
  }

  @Override
  public List<RemoteDeviceInfo> getTerminals() {
    return new ArrayList<>(mailboxSession.listDevices());
  }

  @Override
  public Queue<MessageData> scanForIncoming() {
    Queue<MessageData> queue = new LinkedList<>();
    MobileOriginatedResponse moResponse = mailboxSession.pollMO(lastMoTimeUTC);
    if(moResponse != null){
      lastMoTimeUTC = moResponse.getNextStartTime();
      List<JsonObject> msgs = moResponse.getMessages();
      for (JsonObject msg : msgs) {
        if (msg.has("payloadRaw") && !msg.get("payloadRaw").isJsonNull()) {
          String rawBase64 = msg.get("payloadRaw").getAsString();
          byte[] payload = Base64.getDecoder().decode(rawBase64);
          MessageData data = new MessageData();
          data.setPayload(payload);
          if (msg.has("deviceId")) {
            data.setUniqueId(msg.get("deviceId").getAsString());
          }
          queue.add(data);
        }
      }
    }
    return queue;
  }

  @Override
  public void processPendingMessages() {
    List<MessageData> tmp;
    synchronized (pendingMessages) {
      tmp = new ArrayList<>(pendingMessages);
      pendingMessages.clear();
    }
    // now send the messages to the remote server for delivery
  }

  public void queueMessagesForDelivery(MessageData submitMessages) {
    synchronized (pendingMessages) {
      pendingMessages.add(submitMessages);
    }
  }

}
