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

import io.mapsmessaging.dto.rest.config.protocol.impl.SatelliteConfigDTO;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.ErrorDef;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteClient;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.model.RemoteDeviceInfo;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.FromMobileMessagesResponse;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.ReturnMessage;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.ogws.data.SubmitMessage;

import java.io.IOException;
import java.util.*;

public class InmarsatClient implements SatelliteClient {

  private final InmarsatSession inmarsatSession;
  private InmarsatSession.MailboxSession mailboxSession;


  public InmarsatClient(SatelliteConfigDTO satelliteConfigDTO) {
    inmarsatSession = new InmarsatSession(satelliteConfigDTO);

  }

  @Override
  public void close(){
  }

  @Override
  public boolean authenticate() throws IOException, InterruptedException {
    mailboxSession  = inmarsatSession.openMailbox();
    for (ErrorDef errorDef: inmarsatSession.getErrorCodes()){
      System.err.println(errorDef);
    }
    return true;
  }

  @Override
  public List<RemoteDeviceInfo> getTerminals() {
    return new ArrayList<>(mailboxSession.listDevices());
  }

  @Override
  public Queue<ReturnMessage> scanForIncoming() {
    return new LinkedList<>();
  }

  @Override
  public FromMobileMessagesResponse getFromMobileMessages(String lastMessageUtc) throws IOException {
    return null;
  }

  @Override
  public void queueMessagesForDelivery(SubmitMessage submitMessage) {

  }

  @Override
  public void processPendingMessages() {

  }
}
