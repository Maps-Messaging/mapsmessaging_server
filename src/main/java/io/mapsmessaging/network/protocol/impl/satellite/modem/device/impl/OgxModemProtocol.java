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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OgxModemProtocol extends BaseModemProtocol {
  public OgxModemProtocol(Modem modem) {
    super(modem, true);
  }

  //region Outgoing message functions
  public CompletableFuture<List<SendMessageState>> listSentMessages() {
    return modem.sendATCommand("AT%MOQS").thenApply(super::parseOutgoingMessageList);
  }

  public void sendMessage(ModemSatelliteMessage modemSatelliteMessage) {
    if(modemSatelliteMessage.getPayload().length > 1020){
      ModemSatelliteMessage.XmodemData data =  modemSatelliteMessage.toOgxXModemCommand();
      CompletableFuture<byte[]> future = new CompletableFuture<>();
      modem.setOneShotResponse(true);
      modem.sendATCommand("AT%MOMT=" + data.getCommand());
      modem.getStreamHandler().startXModemTransmit(data.getData(), data.getCrc(), future);
      future.join();
    }
    else {
      modem.sendATCommand("AT%MOMT=" + modemSatelliteMessage.toOgxCommand());
    }
  }

  public CompletableFuture<Boolean> deleteSentMessages(String msgId) {
    return modem.sendATCommand("AT%MOMD=" + msgId).thenApply(x -> x.equalsIgnoreCase("ok"));
  }
  //endregion


  //region Incoming message functions
  public CompletableFuture<List<IncomingMessageDetails>> listIncomingMessages() {
    return modem.sendATCommand("AT%MTQS")
        .thenApply(super::parseIncomingListResponse);
  }

  public CompletableFuture<ModemSatelliteMessage> getMessage(IncomingMessageDetails details) {
    if (details.getBytes() > 1024) {
      MessageFormat format = MessageFormat.DATA;
      String command = "AT%MTMG=" + details.getId() + "," + format.getCode();
      modem.setOneShotResponse(true); // respond before the OK is received, we just want the first line
      return modem.sendATCommand(command)
          .thenCompose(resp -> processXmodemRequest(resp)
              .thenApply(x -> {
                if (x == null) {
                  return null;
                }
                byte[] payload = Arrays.copyOfRange(x, 2, x.length);
                ModemSatelliteMessage modemSatelliteMessage = new ModemSatelliteMessage();
                modemSatelliteMessage.setPayload(payload);
                modemSatelliteMessage.setFormat(format);
                modemSatelliteMessage.setName(details.getId());
                modemSatelliteMessage.setMin(x[1]);
                modemSatelliteMessage.setSin(x[0]);
                return modemSatelliteMessage;
              }));
    } else {
      MessageFormat format = MessageFormat.BASE64;
      String command = "AT%MTMG=" + details.getId() + "," + format.getCode();
      return modem.sendATCommand(command).thenApply(super::parseIncomingMessageResponse);
    }
  }

  public CompletableFuture<Boolean> deleteIncomingMessage(String name) {
    return modem.sendATCommand("AT%MTMD=" + name).thenApply(x -> x.equalsIgnoreCase("ok"));
  }

  //endregion

  public CompletableFuture<byte[]> processXmodemRequest(String request) {
    if (request.startsWith("%MTMG:")) {
      request = request.substring("%MTMG:".length());
    }
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    if (request.equalsIgnoreCase("ok")) {
      future.complete(null);
    } else {
      request = request.trim();
      String[] parts = request.split(",");
      int length = Integer.parseInt(parts[2]);
      long crc = Long.parseLong(parts[4], 16);
      modem.getStreamHandler().startXModemReceive(length, crc, future);
    }
    return future;
  }

  @Override
  public String getType() {
    return "OGx mode modem";
  }
}
