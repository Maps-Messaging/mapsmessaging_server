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
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.Item;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MobileOriginatedResponse;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MobileTerminatedSubmitRequest;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MobileTerminatedSubmitResponse;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteClient;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.StateManager;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.MessageData;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;

import java.io.IOException;
import java.util.*;

public class InmarsatClient implements SatelliteClient {

  private final InmarsatSession inmarsatSession;
  private InmarsatSession.MailboxSession mailboxSession;
  private String lastMoTimeUTC;

  public InmarsatClient(SatelliteConfigDTO satelliteConfigDTO) {
    inmarsatSession = new InmarsatSession(satelliteConfigDTO);
    lastMoTimeUTC  = StateManager.loadLastMessageUtc(inmarsatSession.clientId, inmarsatSession.clientSecret);
    if(lastMoTimeUTC == null) {
      lastMoTimeUTC = java.time.ZonedDateTime.now()
          .withZoneSameInstant(java.time.ZoneOffset.UTC)
          .minusDays(1)
          .toInstant()
          .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
          .toString();
    }
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
  public List<RemoteDeviceInfo> getTerminals(String deviceId) {
    return new ArrayList<>(mailboxSession.listDevices(deviceId));
  }

  @Override
  public Queue<MessageData> scanForIncoming() {
    Queue<MessageData> queue = new LinkedList<>();
    MobileOriginatedResponse moResponse = mailboxSession.pollMO(lastMoTimeUTC);
    if(moResponse != null){
      if(moResponse.getNextStartTime() != null){
        lastMoTimeUTC = moResponse.getNextStartTime();
        StateManager.saveLastMessageUtc(inmarsatSession.clientId, inmarsatSession.clientSecret, lastMoTimeUTC);
      }
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
  public void processPendingMessages(List<MessageData> pendingMessages) {
    List<Item> messageList = new ArrayList<>();
    for(MessageData msg : pendingMessages){
      String payload = Base64.getEncoder().encodeToString(msg.getPayload());
      Item item = new Item(msg.getUniqueId(), null, payload, null);
      messageList.add(item);
    }
    MobileTerminatedSubmitRequest req = new MobileTerminatedSubmitRequest(messageList);
    MobileTerminatedSubmitResponse response = mailboxSession.submitMT(req);
    System.err.println("Response: " + response);
  }

}
