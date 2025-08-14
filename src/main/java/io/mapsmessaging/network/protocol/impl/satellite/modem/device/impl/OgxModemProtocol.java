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
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.Message;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OgxModemProtocol extends BaseModemProtocol {
  public OgxModemProtocol(Modem modem) {
    super(modem);
  }

  public void sendMessage(Message message) {
    modem.sendATCommand("AT%MOMT=" + message.toOgxCommand());
  }

  //region Outgoing message functions
  public CompletableFuture<List<SendMessageState>> listSentMessages() {
    return modem.sendATCommand("AT%MOQS")
        .thenApply(resp -> {
          List<String> tmpList = Arrays.stream(resp.split("\r\n"))
              .map(String::trim)
              .filter(s -> !s.equals("OK") && !s.equals("%MOQS:"))
              .toList();
          List<SendMessageState> states = new ArrayList<>();
          for (String s : tmpList) {
            states.add(new SendMessageState(s));
          }
          return states;
        });
  }

  public CompletableFuture<Void> deleteSentMessages(String msgId) {
    return modem.sendATCommand("AT%MOMD=" + msgId).thenApply(x -> null);
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
    return modem.sendATCommand("AT%MTQS")
        .thenApply(resp -> {
          return Arrays.stream(resp.split("\r\n"))
              .map(String::trim)
              .map(line -> line.replaceAll("^\"|\"$", "")) // remove surrounding quotes
              .toList();
        });
  }

  public CompletableFuture<byte[]> getMessage(String metaLine, MessageFormat format) {
    String name = metaLine.trim();
    if (name.startsWith("%MTQS:")) {
      name = name.substring("%MTQS:".length());
    }
    name = name.split(",")[0].replace("\"", "").trim();
    if (name.isEmpty() || name.equals("OK")) {
      return CompletableFuture.completedFuture(null);
    }
    String command = "AT%MTMG=" + name + "," + format.getCode();
    return modem.sendATCommand(command)
        .thenApply(resp -> {
          for (String line : resp.split("\r\n")) {
            line = line.trim();
            if (line.startsWith("%MTMG:")) {
              String[] parts = line.split(",");
              if (parts.length >= 5) {
                String encoded = parts[parts.length - 1].replace("\"", "").trim();
                return format.decode(encoded);
              }
            }
          }
          throw new IllegalStateException("Unable to parse payload from: " + resp);
        });
  }

  public CompletableFuture<Void> markMessageRetrieved(String metaLine) {
    String name = metaLine.trim();
    if (name.startsWith("%MTQS:")) {
      name = name.substring("%MTQS:".length());
    }
    name = name.split(",")[0].replace("\"", "").trim();
    if (name.isEmpty() || name.equals("OK")) {
      return CompletableFuture.completedFuture(null);
    }
    return modem.sendATCommand("AT%MTMD=" + name).thenApply(x -> null);
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
    return "OGx mode modem";
  }
}
