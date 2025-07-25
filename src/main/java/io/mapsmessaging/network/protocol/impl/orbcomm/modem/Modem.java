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

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class Modem {
  private ModemLineHandler currentHandler;

  private final Map<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
  private final Consumer<Packet> packetSender;

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

  public CompletableFuture<String> getJammingIndicator() {
    return sendATCommand("ATS57=<>");
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

  public void sendMessage(String name, int priority, int sin, int min, byte[] payload) {
    // Implement this once you define how you store message payloads and names
    // Most likely needs XMODEM + %MGRT initiation
  }

  public CompletableFuture<List<String>> listIncomingMessages() {
    return sendATCommand("AT%MGFN")
        .thenApply(resp -> Arrays.stream(resp.split("\r\n"))
            .filter(line -> line.startsWith("FM")) // FMaa.ss or FMaa.ss0
            .map(String::trim)
            .toList());
  }

  public CompletableFuture<byte[]> getMessage(String name) {
    return sendATCommand("AT%MGFG=" + name)
        .thenApply(resp -> {
          // Response may contain both meta and payload, depends on verbose mode
          int idx = resp.indexOf("\r\n");
          if (idx >= 0 && idx + 2 < resp.length()) {
            return resp.substring(idx + 2).getBytes(StandardCharsets.US_ASCII);
          } else {
            return resp.getBytes(StandardCharsets.US_ASCII);
          }
        });
  }
  public CompletableFuture<Void> markMessageRetrieved(String name) {
    return sendATCommand("AT%MGFM=" + name).thenApply(x -> null);
  }

  public CompletableFuture<List<byte[]>> fetchAllMessages() {
    return listIncomingMessages()
        .thenCompose(names -> {
          List<CompletableFuture<byte[]>> futures = names.stream()
              .map(this::getMessage)
              .toList();
          return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
              .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
        });
  }

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

  protected CompletableFuture<String> sendATCommand(String cmd) {
    String key = UUID.randomUUID().toString();
    CompletableFuture<String> future = new CompletableFuture<>();
    pending.put(key, future);
    // Store key if needed for matching later
    packetSender.accept(packetWith(cmd));
    return future;
  }

  private Packet packetWith(String cmd) {
    byte[] data = (cmd + "\r").getBytes(StandardCharsets.US_ASCII);
    Packet packet = new Packet(data.length, false);
    packet.put(data).flip();
    return packet;
  }

  private void handleLine(String line) {
    if (line.equalsIgnoreCase("OK") || line.startsWith("ERROR")) {
      // Complete the first pending future with the result
      pending.values().stream().findFirst().ifPresent(f -> f.complete(line));
      pending.clear(); // Assuming single outstanding command at a time
    } else {
      System.out.println("MODEM <<< " + line); // Debug or logging line
    }
  }

}
