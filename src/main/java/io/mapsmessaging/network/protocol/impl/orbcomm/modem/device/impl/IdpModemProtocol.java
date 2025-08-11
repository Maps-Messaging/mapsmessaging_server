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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.impl;

import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.Modem;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.Message;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.device.values.MessageFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IdpModemProtocol extends BaseModemProtocol {

  public IdpModemProtocol(Modem modem) {
    super(modem);
  }


  public void sendMessage(Message message) {
    modem.sendATCommand("AT%MGRT=" + message.toATCommand());
  }

  //region outgoing message functions
  public CompletableFuture<List<SendMessageState>> listSentMessages() {
    return modem.sendATCommand("AT%MGRS")
        .thenApply(resp -> {
          List<String> tmpList = Arrays.stream(resp.split("\r\n"))
              .map(String::trim)
              .filter(s -> !s.equals("OK") && !s.equals("%MGRS:"))
              .toList();
          List<SendMessageState> states = new ArrayList<>();
          for (String s : tmpList) {
            states.add(new SendMessageState(s));
          }
          return states;
        });
  }

  public CompletableFuture<Void> deleteSentMessages(String msgName) {
    return modem.sendATCommand("AT%MGRD="+msgName).thenApply(x -> null);
  }


  public CompletableFuture<Void> markSentMessageRead(String name) {
    return modem.sendATCommand("AT%MGFR=" + name).thenApply(x -> null);
  }

  public void listOutgoingMessages() {
    modem.sendATCommand("AT%MGRL"); // From-Mobile Message List
  }

//endregion


  //region Incoming message functions
  public CompletableFuture<List<String>> listIncomingMessages() {
    return modem.sendATCommand("AT%MGFN")
        .thenApply(resp -> Arrays.stream(resp.split("\r\n"))
            .filter(line -> line.contains("FM"))
            .map(String::trim)
            .map(line -> line.replaceAll("^\"|\"$", "")) // remove surrounding quotes
            .toList());
  }

  public CompletableFuture<byte[]> getMessage(String metaLine, MessageFormat format) {
    String name = metaLine.trim();
    if (name.startsWith("%MGFN:")) {
      name = name.substring(name.indexOf('"') + 1);
    }
    name = name.split(",")[0].replace("\"", "").trim(); // Extract FMxxx

    String command = "AT%MGFG=\"" + name + "\"," + format.getCode();

    return modem.sendATCommand(command)
        .thenApply(resp -> {
          for (String line : resp.split("\r\n")) {
            line = line.trim();
            if (line.startsWith("%MGFG:")) {
              String[] parts = line.split(",");
              if (parts.length >= 8) {
                String encoded = parts[7].replace("\"", "").trim();
                return format.decode(encoded);
              }
            }
          }

          throw new IllegalStateException("Unable to parse payload from: " + resp);
        });
  }

  public CompletableFuture<Void> markMessageRetrieved(String metaLine) {
    String name = metaLine.trim();
    if (name.startsWith("%MGFN:")) {
      name = name.substring(name.indexOf('"') + 1);
    }
    name = name.split(",")[0].replace("\"", "").trim(); // Extract FMxxx
    return modem.sendATCommand("AT%MGFM=\"" + name + "\"").thenApply(x -> null);
  }

  public CompletableFuture<List<byte[]>> fetchAllMessages(MessageFormat format) {
    return listIncomingMessages()
        .thenCompose(names -> {
          List<CompletableFuture<byte[]>> futures = names.stream()
              .map(name -> getMessage(name, format)
                  .thenCompose(data -> markMessageRetrieved(name).thenApply(v -> data)))
              .toList();

          return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
              .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
        });
  }

  @Override
  public String getType() {
    return "IDG mode modem";
  }
  //endregion
}
