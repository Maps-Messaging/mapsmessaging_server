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
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IdpModemProtocol extends BaseModemProtocol {

  public IdpModemProtocol(Modem modem) {
    super(modem, false);
  }


  //region outgoing message functions
  public void sendMessage(ModemSatelliteMessage modemSatelliteMessage) {
    modem.sendATCommand("AT%MGRT=" + modemSatelliteMessage.toATCommand());
  }

  public CompletableFuture<List<SendMessageState>> listSentMessages() {
    return modem.sendATCommand("AT%MGRL").thenApply(super::parseOutgoingMessageList);
  }

  public CompletableFuture<Boolean> deleteSentMessages(String msgName) {
    if(!msgName.startsWith("\"")){
      msgName = "\"" + msgName;
    }
    if(!msgName.endsWith("\"")){
      msgName = msgName + "\"";
    }
    return modem.sendATCommand("AT%MGRD=" + msgName).thenApply(x -> true);
  }
  //endregion


  //region Incoming message functions
  public CompletableFuture<List<IncomingMessageDetails>> listIncomingMessages() {
    return modem.sendATCommand("AT%MGFN").thenApply(super::parseIncomingListResponse);
  }

  public CompletableFuture<ModemSatelliteMessage> getMessage(IncomingMessageDetails details) {
    String command = "AT%MGFG=" + details.getId() + "," + MessageFormat.BASE64.getCode();
    return modem.sendATCommand(command).thenApply(super::parseIncomingMessageResponse);
  }

  public CompletableFuture<Boolean> deleteIncomingMessage(String name) {
    return modem.sendATCommand("AT%MGFM=" + name).thenApply(x -> x.equalsIgnoreCase("ok"));
  }
  //endregion

  @Override
  public String getType() {
    return "IDG mode modem";
  }
}
