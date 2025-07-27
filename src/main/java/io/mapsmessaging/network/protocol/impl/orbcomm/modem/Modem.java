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

package io.mapsmessaging.network.protocol.impl.orbcomm.modem;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.messages.OutboundMessage;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.values.GnssTrackingMode;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.values.MessageFormat;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.values.ModemMessageStatusFlag;
import io.mapsmessaging.network.protocol.impl.orbcomm.modem.values.PositioningMode;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class Modem {

  private final Consumer<Packet> packetSender;
  private final Queue<Command> commandQueue = new ArrayDeque<>();
  private final StringBuilder responseBuffer = new StringBuilder();

  private ModemLineHandler currentHandler;
  private Command currentCommand = null;

  public Modem(Consumer<Packet> packetSender) {
    this.packetSender = packetSender;
    currentHandler = new TextResponseHandler(this::handleLine);
  }

  public void process(Packet packet) {
    if (currentHandler != null) {
      currentHandler.onData(packet);
    } else {
      // Fallback buffer accumulation (e.g. for early boot noise)
    }
  }

  public CompletableFuture<Void> initializeModem() {
    return sendATCommand("ATE0;&W").thenApply(x -> null);
  }

  //region Modem Status functions
  public void queryModemInfo() {
    sendATCommand("ATI0;+GMM;+GMR;+GMR;+GMI");
  }

  public CompletableFuture<String> getTemperature() {
    return sendATCommand("ATS85?");
  }

  public CompletableFuture<List<ModemMessageStatusFlag>> getActiveStatuses(){
    return getActiveStatuses("ATS88?");
  }

  public CompletableFuture<List<ModemMessageStatusFlag>> getStatusChanges(){
    return getActiveStatuses("ATS89?");
  }

  private CompletableFuture<List<ModemMessageStatusFlag>> getActiveStatuses(String command) {
    return sendATCommand(command)
        .thenApply(response -> {
          int value;
          try {
            value = Integer.parseInt(response.trim());
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ATS88 response: " + response, e);
          }

          List<ModemMessageStatusFlag> flags = new ArrayList<>();
          for (ModemMessageStatusFlag flag : ModemMessageStatusFlag.values()) {
            if (flag.isSet(value)) {
              flags.add(flag);
            }
          }
          return flags;
        });
  }
  //endregion

  //region Positioning controls
  public CompletableFuture<String> setTrackingMode(GnssTrackingMode mode) {
    return sendATCommand("ATS80=" + mode.getCode());
  }

  public CompletableFuture<Integer> getJammingIndicator() {
    return sendATCommand("ATS57?")
        .thenApply(resp -> {
          String[] lines = resp.split("\r\n");
          for (String line : lines) {
            line = line.trim();
            if (!line.equalsIgnoreCase("OK") && !line.startsWith("ERROR") && !line.isEmpty()) {
              try {
                return Integer.parseInt(line);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ATS57 response: " + line, e);
              }
            }
          }
          throw new IllegalStateException("No valid response line found in ATS57 result: " + resp);
        });
  }

  public CompletableFuture<String> requestPosition() {
    return sendATCommand("AT%GPS=1,180");
  }

  public CompletableFuture<String> requestPositionReport() {
    return sendATCommand("AT%POSR");
  }

  public CompletableFuture<String> setPositioningMode(PositioningMode mode) {
    return sendATCommand("ATS39=" + mode.getCode());
  }
  //endregion


  public void listOutgoingMessages() {
    sendATCommand("AT%MGRL"); // From-Mobile Message List
  }

  public void sendMessage(String name, int priority, int sin, int min, String payload) {
    OutboundMessage outboundMessage = new OutboundMessage();
    outboundMessage.setName(name);
    outboundMessage.setPriority(priority);
    outboundMessage.setPayload(payload);
    outboundMessage.setMIN(min);
    outboundMessage.setSIN(sin);

    outboundMessage.getPayload();

  }

  //region Incoming message functions
  public CompletableFuture<List<String>> listIncomingMessages() {
    return sendATCommand("AT%MGFN")
        .thenApply(resp -> Arrays.stream(resp.split("\r\n"))
            .filter(line -> line.startsWith("FM")) // FMaa.ss or FMaa.ss0
            .map(String::trim)
            .toList());
  }

  public CompletableFuture<byte[]> getMessage(String name, MessageFormat format) {
    String quotedName = "\"" + name + "\"";
    return sendATCommand("AT%MGFG=" + quotedName + "," + format.getCode())
        .thenApply(resp -> {
          for (String line : resp.split("\r\n")) {
            line = line.trim();
            if (line.startsWith("+MGFG:")) {
              int start = line.indexOf('"');
              int end = line.lastIndexOf('"');
              if (start >= 0 && end > start) {
                String encoded = line.substring(start + 1, end);
                return (format == MessageFormat.HEX)
                    ? hexDecode(encoded)
                    : Base64.getDecoder().decode(encoded);
              }
            }
          }
          throw new IllegalStateException("Unable to parse payload from: " + resp);
        });
  }

  public CompletableFuture<Void> markMessageRetrieved(String name) {
    return sendATCommand("AT%MGFM=" + name).thenApply(x -> null);
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
  //endregion

  public void factoryReset() {
    sendATCommand("AT&F;Z");
  }

  public void shutdownModem() {
    sendATCommand("AT%OFF");
  }

  public void enableTraceLog() {
    sendATCommand("AT%TRK=1");
  }

  public void disableTraceLog() {
    sendATCommand("AT%TRK=0");
  }

  public CompletableFuture<List<String>> querySyslog() {
    return sendATCommand("AT%SYSL")
        .thenApply(response -> Arrays.stream(response.split("\r\n"))
            .filter(line -> !line.isBlank())
            .toList());
  }

  protected synchronized CompletableFuture<String> sendATCommand(String cmd) {
    CompletableFuture<String> future = new CompletableFuture<>();
    commandQueue.add(new Command(cmd, future));
    if (currentCommand == null) {
      sendNextCommand();
    }
    return future;
  }

  private synchronized void sendNextCommand() {
    currentCommand = commandQueue.poll();
    if (currentCommand != null) {
      packetSender.accept(packetWith(currentCommand.command));
    }
  }

  private Packet packetWith(String cmd) {
    byte[] data = (cmd + "\r").getBytes(StandardCharsets.US_ASCII);
    Packet packet = new Packet(data.length, false);
    packet.put(data).flip();
    return packet;
  }

  private synchronized void handleLine(String line) {
    if (currentCommand == null) {
      // Unsolicited line, forward to listener
      handleUnsolicitedLine(line);
      return;
    }

    if (line.equalsIgnoreCase("OK") || line.startsWith("ERROR")) {
      String response = responseBuffer.toString().trim();
      if (!response.isEmpty()) {
        response += "\r\n";
      }
      response += line;

      currentCommand.future.complete(response);
      currentCommand = null;
      responseBuffer.setLength(0);
      sendNextCommand();
    } else {
      responseBuffer.append(line).append("\r\n");
    }
  }

  private byte[] hexDecode(String hex) {
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
          + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
  }


  private void handleUnsolicitedLine(String line) {
    if (line.startsWith("%MGU:")) {
      String msgName = line.substring("%MGU:".length()).trim();
      System.out.println("New to-mobile message available: " + msgName);
      // Optionally auto-fetch: listIncomingMessages() → getMessage(name)

    } else if (line.startsWith("%SYSE:")) {
      String error = line.substring("%SYSE:".length()).trim();
      System.err.println("System error: " + error);

    } else if (line.startsWith("%PWRDWN")) {
      System.out.println("Modem is shutting down.");

    } else if (line.startsWith("%TRK:")) {
      System.out.println("Trace event: " + line);

    } else if (line.startsWith("%POSR:") || line.startsWith("%GPSPOS:")) {
      System.out.println("Position report: " + line);

    } else if (line.startsWith("%RING")) {
      System.out.println("Incoming event: %RING");

    } else {
      System.out.println("Unhandled unsolicited: " + line);
    }
  }


}
