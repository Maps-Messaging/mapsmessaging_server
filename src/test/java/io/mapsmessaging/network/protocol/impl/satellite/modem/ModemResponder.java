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

package io.mapsmessaging.network.protocol.impl.satellite.modem;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.network.protocol.impl.satellite.modem.ogx.OgxModemRegistation;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static io.mapsmessaging.network.protocol.impl.satellite.gateway.InmarsatMockServer.getEnvBool;

public class ModemResponder implements Runnable {


  private final String deviceName;
  private final Map<String, Function<ParsedAt, String>> handlers = new ConcurrentHashMap<>();

  @Getter
  private final Queue<SentMessageEntry> oustandingEntries = new ConcurrentLinkedQueue<>();
  private volatile boolean running;
  private Thread worker;
  private SerialPort port;
  @Getter
  private final Queue<byte[]> incomingMessages;
  @Getter
  private final Queue<byte[]> outgoingMessages;
  private final BaseModemRegistration idpModem;
  private final boolean logMessages;

  public ModemResponder( Queue<byte[]> incomingMessages,  Queue<byte[]> outgoingMessages, String deviceName) {
    this.deviceName = Objects.requireNonNull(deviceName, "deviceName");
    this.incomingMessages = incomingMessages;
    this.outgoingMessages = outgoingMessages;
    logMessages = getEnvBool("MODEM_LOG_MESSAGES", false);
    idpModem = new OgxModemRegistation(this);
  }

  /** Register a handler for a command name: "", "I", "+CSQ", "+FOO" etc. Return full payload (without CRLF). */
  public void registerHandler(String commandName, Function<ParsedAt, String> handler) {
    handlers.put(commandName.toUpperCase(), handler);
  }

  /** Optional: set a blanket default handler (overrides built-in OK). */
  public void setDefaultHandler(Function<ParsedAt, String> handler) {
    handlers.put("__DEFAULT__", handler);
  }

  public void start() {
    if (running) return;
    idpModem.registerModem();
    running = true;
    worker = new Thread(this, "AtResponder-" + deviceName);
    worker.start();
  }

  public void stop() {
    running = false;
    if (worker != null) {
      try { worker.join(2000); } catch (InterruptedException ignored) { }
    }
    if (port != null && port.isOpen()) {
      port.closePort();
    }
  }

  @Override
  public void run() {
    port = SerialPort.getCommPort(deviceName);
    port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
    port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
    port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

    if (!port.openPort()) {
      throw new IllegalStateException("Unable to open serial port " + deviceName);
    }

    try (InputStream in = port.getInputStream(); OutputStream out = port.getOutputStream()) {
      StringBuilder buffer = new StringBuilder();
      while (running) {
        int b = in.read();
        if (b == -1) continue;
        char c = (char) b;

        if (c == '\r' || c == '\n') {
          String line = buffer.toString().trim();
          buffer.setLength(0);
          if (!line.isEmpty()){
            handleLine(line, out);
          }
        } else {
          buffer.append(c);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (port.isOpen()) port.closePort();
    }
  }

  private void handleLine(String line, OutputStream out) throws IOException {
    // Must begin with AT (case-insensitive)
    logMessage("IN ", line);
    if (!line.regionMatches(true, 0, "AT", 0, 2)) return;
    ParsedAt at = ParsedAt.parse(line);
    String key = at.getName().toUpperCase();  // "", "I", "+CSQ", "+FOO"
    Function<ParsedAt, String> handler =
        handlers.getOrDefault(key, handlers.get("__DEFAULT__"));

    String payload = (handler != null) ? handler.apply(at) : "OK";
    logMessage("OUT", payload);
    writeResponse(out, payload);
  }

  private void writeResponse(OutputStream out, String payload) throws IOException {
    if (payload != null && !payload.isEmpty()) {
      out.write(payload.getBytes(StandardCharsets.US_ASCII));
      if (!payload.endsWith("\r\n")) out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    } else {
      out.write("OK\r\n".getBytes(StandardCharsets.US_ASCII));
    }
    out.flush();
  }

  private void logMessage(String direction, String msg) {
    System.err.println(direction + " > " + msg);
  }
}
