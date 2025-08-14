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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.BaseModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.IdpModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.OgxModemProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.Message;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.STOGI_RECEIVED_AT_MESSAGE;
import static io.mapsmessaging.logging.ServerLogMessages.STOGI_SEND_AT_MESSAGE;


public class Modem {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Consumer<Packet> packetSender;
  private final Queue<Command> commandQueue = new ArrayDeque<>();
  private final StringBuilder responseBuffer = new StringBuilder();
  private final ModemLineHandler currentHandler;
  private final long modemTimeout;

  private ScheduledFuture<?> future;

  private Command currentCommand = null;
  private BaseModemProtocol modemProtocol = null;

  public Modem(Consumer<Packet> packetSender, long modemTimeout) {
    this.packetSender = packetSender;
    currentHandler = new TextResponseHandler(this::handleLine);
    if(modemTimeout < 10000 || modemTimeout > 120000){
      modemTimeout = 15000;
    }
    this.modemTimeout = modemTimeout;

    future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::scanTimeouts, modemTimeout, modemTimeout, TimeUnit.MILLISECONDS);
  }

  public void close(){
    for(Command command : commandQueue){
      command.future.completeExceptionally(new IOException("Modem has been closed"));
    }
    commandQueue.clear();
    if(future != null){
      future.cancel(false);
      future = null;
    }
  }

  private void scanTimeouts(){
    long time = System.currentTimeMillis();
    if(currentCommand != null && currentCommand.timeout < time){
      close();
      currentCommand.future.completeExceptionally(new IOException("Modem has been closed"));
    }
  }

  public void process(Packet packet) {
    currentHandler.onData(packet);
  }

  public CompletableFuture<String> initializeModem() {
    return sendATCommand("ATE0;&W;I5").thenApply(response -> {
      if (response.startsWith("8")) {
        modemProtocol = new IdpModemProtocol(this);
      } else {
        modemProtocol = new OgxModemProtocol(this);
      }
      return response;
    });
  }

  //region Modem Status functions
  public CompletableFuture<String> queryModemInfo() {
    return sendATCommand("ATI0;+GMM;+GMR;+GMR;+GMI");
  }

  public CompletableFuture<String> enableLocation() {
    return sendATCommand("AT%TRK=10,1");
  }

  public CompletableFuture<List<String>> getLocation() {
    return sendATCommand("AT%GPS=15,1,\"GGA\",\"RMC\",\"GSV\"").thenApply(resp ->
        java.util.Arrays.stream(resp.split("\\R"))
            .map(String::trim)
            .map(s -> s.replaceFirst("^%GPS:\\s*", "")) // strip leading "%GPS: "
            .filter(s -> s.startsWith("$")) // keep only NMEA lines
            .toList()
    );
  }

  public CompletableFuture<String> getTemperature() {
    return sendATCommand("ATS85?");
  }

  //endregion

  //region Positioning controls
  public CompletableFuture<Integer> getJammingStatus() {
    return sendATCommand("ATS56?")
        .thenApply(resp -> {
          String[] lines = resp.split("\r\n");
          for (String line : lines) {
            line = line.trim();
            if (!line.equalsIgnoreCase("OK") && !line.startsWith("ERROR") && !line.isEmpty()) {
              try {
                return Integer.parseInt(line);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ATS56 response: " + line, e);
              }
            }
          }
          throw new IllegalStateException("No valid response line found in ATS56 result: " + resp);
        });
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

  //endregion

  //region Outgoing message functions

  //region Sent message functions
  public CompletableFuture<List<SendMessageState>> listSentMessages() {
    return modemProtocol.listSentMessages();
  }

  public CompletableFuture<Void> deleteSentMessages(String msgName) {
    return modemProtocol.deleteSentMessages(msgName);
  }


  public CompletableFuture<Void> markSentMessageRead(String name) {
    return modemProtocol.markSentMessageRead(name);
  }

//endregion


  public void listOutgoingMessages() {
    modemProtocol.listOutgoingMessages();
  }

  public void sendMessage(int priority, int sin, int min, byte[] payload) {
    Message message = new Message();
    message.setPriority(priority);
    message.setPayload(payload);
    message.setMin(min);
    message.setSin(sin);
    message.setFormat(MessageFormat.BASE64);

    modemProtocol.sendMessage(message);
  }
  //endregion

  //region Incoming message functions
  public CompletableFuture<List<String>> listIncomingMessages() {
    return modemProtocol.listIncomingMessages();
  }

  public CompletableFuture<byte[]> getMessage(String metaLine, MessageFormat format) {
    return modemProtocol.getMessage(metaLine, format);
  }

  public CompletableFuture<Void> markMessageRetrieved(String metaLine) {
    return modemProtocol.markMessageRetrieved(metaLine);
  }

  public CompletableFuture<List<byte[]>> fetchAllMessages(MessageFormat format) {
    return modemProtocol.fetchAllMessages(format);
  }
  //endregion

  public synchronized CompletableFuture<String> sendATCommand(String cmd) {
    logger.log(STOGI_SEND_AT_MESSAGE, cmd);
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
      currentCommand.timeout = System.currentTimeMillis()+modemTimeout;
      packetSender.accept(packetWith(currentCommand.cmd));
    }
  }

  private Packet packetWith(String cmd) {
    byte[] data = (cmd + "\r\n").getBytes(StandardCharsets.US_ASCII);
    Packet packet = new Packet(data.length, false);
    packet.put(data).flip();
    return packet;
  }

  private synchronized void handleLine(String line) {
    logger.log(STOGI_RECEIVED_AT_MESSAGE, line);
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


  public String getType() {
    return modemProtocol.getType();
  }

}
