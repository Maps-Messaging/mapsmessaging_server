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
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.impl.data.NetworkStatus;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.IncomingMessageDetails;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.MessageNameGenerator;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.ModemSatelliteMessage;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.messages.SendMessageState;
import io.mapsmessaging.network.protocol.impl.satellite.modem.device.values.MessageFormat;
import io.mapsmessaging.network.protocol.impl.satellite.modem.protocol.ModemStreamHandler;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static io.mapsmessaging.logging.ServerLogMessages.STOGI_RECEIVED_AT_MESSAGE;
import static io.mapsmessaging.logging.ServerLogMessages.STOGI_SEND_AT_MESSAGE;


public class Modem {

  private static final String OK = "OK";
  private static final String ERROR = "ERROR";
  private static final String EOL = "\r\n";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Consumer<Packet> packetSender;
  private final Queue<Command> commandQueue = new ArrayDeque<>();
  private final StringBuilder responseBuffer = new StringBuilder();
  private final long modemTimeout;

  @Getter
  private final ModemStreamHandler streamHandler;

  private final ModemLineHandler currentHandler;

  private ScheduledFuture<?> future;

  private Command currentCommand = null;

  @Getter
  private BaseModemProtocol modemProtocol = null;
  @Getter
  @Setter
  private boolean oneShotResponse;

  public Modem(Consumer<Packet> packetSender, long modemTimeout, ModemStreamHandler streamHandler) {
    this.streamHandler = streamHandler;
    this.packetSender = packetSender;
    oneShotResponse = false;
    currentHandler = new TextResponseHandler(this::handleLine);
    if (modemTimeout < 10000 || modemTimeout > 120000) {
      modemTimeout = 15000;
    }
    this.modemTimeout = modemTimeout;

    future = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this::scanTimeouts, modemTimeout, modemTimeout, TimeUnit.MILLISECONDS);
  }

  public void close() {
    for (Command command : commandQueue) {
      command.future.completeExceptionally(new IOException("Modem has been closed"));
    }
    commandQueue.clear();
    if (future != null) {
      future.cancel(false);
      future = null;
    }
  }

  private void scanTimeouts() {
    long time = System.currentTimeMillis();
    if (currentCommand != null && currentCommand.timeout < time) {
      close();
      currentCommand.future.completeExceptionally(new IOException("Modem has been closed"));
    }
  }

  public void process(Packet packet) {
    currentHandler.onData(packet);
  }

  public CompletableFuture<BaseModemProtocol> initializeModem() {
    return sendATCommand("ATE0;&W;I5").thenApply(response -> {
      String[] lines = response.split(EOL);
      for(String line : lines) {
        line = line.trim();
        if (line.startsWith("8")) {
          modemProtocol = new IdpModemProtocol(this);
        } else if (line.startsWith("10")) {
          modemProtocol = new OgxModemProtocol(this);
        }
      }
      return modemProtocol;
    });
  }

  public NetworkStatus getNetworkStatus() {
    return modemProtocol.getCurrentNetworkStatus();
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
          String[] lines = resp.split(EOL);
          for (String line : lines) {
            line = line.trim();
            if (!line.equalsIgnoreCase(OK) && !line.startsWith(ERROR) && !line.isEmpty()) {
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
          String[] lines = resp.split(EOL);
          for (String line : lines) {
            line = line.trim();
            if (!line.equalsIgnoreCase(OK) && !line.startsWith(ERROR) && !line.isEmpty()) {
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

  public CompletableFuture<Boolean> deleteSentMessages(String msgName) {
    return modemProtocol.deleteSentMessages(msgName);
  }

//endregion


  public void sendMessage(int priority, int sin, int min, int messageLifeTime, byte[] payload) {
    ModemSatelliteMessage modemSatelliteMessage = new ModemSatelliteMessage();
    modemSatelliteMessage.setName(MessageNameGenerator.incrementString().trim());
    modemSatelliteMessage.setPriority(priority);
    modemSatelliteMessage.setPayload(payload);
    modemSatelliteMessage.setMin(min);
    modemSatelliteMessage.setSin(sin);
    modemSatelliteMessage.setLifeTime(messageLifeTime);
    modemSatelliteMessage.setFormat(MessageFormat.BASE64);
    modemProtocol.sendMessage(modemSatelliteMessage);
  }
  //endregion

  //region Incoming message functions
  public CompletableFuture<List<IncomingMessageDetails>> listIncomingMessages() {
    return modemProtocol.listIncomingMessages();
  }

  public CompletableFuture<ModemSatelliteMessage> getMessage(IncomingMessageDetails details) {
    return modemProtocol.getMessage(details);
  }

  public CompletableFuture<Boolean> markMessageRetrieved(String name) {
    return modemProtocol.deleteIncomingMessage(name);
  }
  //endregion

  public synchronized CompletableFuture<String> sendATCommand(String cmd) {
    logger.log(STOGI_SEND_AT_MESSAGE, cmd);
    CompletableFuture<String> futureResponse = new CompletableFuture<>();
    commandQueue.add(new Command(cmd, futureResponse));
    if (currentCommand == null) {
      sendNextCommand();
    }
    return futureResponse;
  }

  private synchronized void sendNextCommand() {
    currentCommand = commandQueue.poll();
    if (currentCommand != null) {
      currentCommand.timeout = System.currentTimeMillis() + modemTimeout;
      packetSender.accept(packetWith(currentCommand.cmd));
    }
  }

  public void resetCommandQueue() {
    this.commandQueue.clear();
    this.currentCommand = null;
  }

  private Packet packetWith(String cmd) {
    byte[] data = (cmd + EOL).getBytes(StandardCharsets.US_ASCII);
    Packet packet = new Packet(data.length, false);
    packet.put(data).flip();
    return packet;
  }

  private synchronized void handleLine(String line) {
    if (line.isEmpty()) {
      return;
    }
    logger.log(STOGI_RECEIVED_AT_MESSAGE, line);
    if (currentCommand == null) {
      // log this
      return;
    }

    if (line.equalsIgnoreCase(OK) || line.startsWith(ERROR) || oneShotResponse) {
      oneShotResponse = false;
      String response = responseBuffer.toString().trim();
      if (!response.isEmpty()) {
        response += EOL;
      }
      response += line;
      currentCommand.future.complete(response);
      currentCommand = null;
      responseBuffer.setLength(0);
      sendNextCommand();
    } else {
      responseBuffer.append(line).append(EOL);
    }
  }

  public void waitForModemActivity() {
    boolean ready = false;
    int countDown = 5;
    while (!ready && countDown > 0) {
      try {
        CompletableFuture<String> res = sendATCommand("AT");
        String response = res.get(2000, TimeUnit.MILLISECONDS);
        if (response != null) {
          ready = true;
        }
      } catch (TimeoutException | ExecutionException te) {
        resetCommandQueue();
        countDown--;
      } catch (InterruptedException e) {
        ready = true;
        Thread.currentThread().interrupt();
      }
    }
  }

  public String getType() {
    return modemProtocol.getType();
  }
}
