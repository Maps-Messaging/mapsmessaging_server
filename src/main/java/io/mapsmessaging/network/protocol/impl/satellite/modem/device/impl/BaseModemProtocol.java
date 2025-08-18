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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.satellite.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class BaseModemProtocol {

  protected final Modem modem;
  protected final boolean isOgx;

  protected BaseModemProtocol(Modem modem, boolean isOgx) {
    this.modem = modem;
    this.isOgx = isOgx;
  }

  /*
  Handle outgoing messages
   */
  public abstract CompletableFuture<List<SendMessageState>> listSentMessages();

  public abstract void sendMessage(ModemSatelliteMessage modemSatelliteMessage);

  public abstract CompletableFuture<Boolean> deleteSentMessages(String msgName);

  /*
  Handle incoming messages
   */
  public abstract CompletableFuture<List<IncomingMessageDetails>> listIncomingMessages();

  public abstract CompletableFuture<ModemSatelliteMessage> getMessage(IncomingMessageDetails details);

  public abstract CompletableFuture<Boolean> deleteIncomingMessage(String name);

  /*
  Generic helpers
   */
  public abstract String getType();


  protected List<SendMessageState> parseOutgoingMessageList(String resp) {
    List<SendMessageState> states = new ArrayList<>();
    String[] lines = resp.split("\n");
    for (String line : lines) {
      line = line.trim();
      if (!line.isEmpty() &&
          !line.equalsIgnoreCase("ok") &&
          !line.equalsIgnoreCase("error")) {
        states.add(new SendMessageState(line, isOgx));
      }
    }
    return states;
  }


  protected List<IncomingMessageDetails> parseIncomingListResponse(String resp) {
    List<IncomingMessageDetails> response = new ArrayList<>();
    String[] lines = resp.split("\r\n");
    for (String line : lines) {
      line = line.trim();
      if (!line.isEmpty() &&
          !line.equalsIgnoreCase("ok") &&
          !line.equalsIgnoreCase("error")) {
        if(line.toLowerCase().startsWith("%mgfn:") || line.toLowerCase().startsWith("%mtqs:")) {
          line = line.substring(6).trim(); // same size
        }
        if(!line.isEmpty()) {
          response.add(new IncomingMessageDetails(line, isOgx));
        }
      }
    }
    return response;
  }

  protected ModemSatelliteMessage parseIncomingMessageResponse(String resp) {
    String[] lines = resp.split("\r\n");
    for (String line : lines) {
      line = line.trim();
      if (!line.isEmpty() &&
          !line.equalsIgnoreCase("ok") &&
          !line.equalsIgnoreCase("error")) {
        return new ModemSatelliteMessage(line, isOgx);
      }
    }
    return null;
  }

}
