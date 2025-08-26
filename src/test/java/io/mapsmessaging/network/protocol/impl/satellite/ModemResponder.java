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

package io.mapsmessaging.network.protocol.impl.satellite;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModemResponder implements Runnable {


  private final String deviceName;
  private final Map<String, Function<ParsedAt, String>> handlers = new ConcurrentHashMap<>();
  private volatile boolean running;
  private Thread worker;
  private SerialPort port;

  public ModemResponder(String deviceName) {
    this.deviceName = Objects.requireNonNull(deviceName, "deviceName");
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
    System.err.println("Received: " + line);
    if (!line.regionMatches(true, 0, "AT", 0, 2)) return;
    ParsedAt at = ParsedAt.parse(line);
    String key = at.getName().toUpperCase();  // "", "I", "+CSQ", "+FOO"
    Function<ParsedAt, String> handler =
        handlers.getOrDefault(key, handlers.get("__DEFAULT__"));

    String payload = (handler != null) ? handler.apply(at) : "OK";
    System.err.println(payload);
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

  public static void main(String[] args) throws IOException {
    Queue<byte[]> queue = new ConcurrentLinkedQueue<>();
    ModemResponder modemResponder = new ModemResponder("com7");
    modemResponder.registerHandler("E0;&W;I5", at -> "\r\n10\r\nOK");
    modemResponder.registerHandler("I0;+GMM;+GMR;+GMR;+GMI", at ->
        "ORBCOMM Inc\r\n" +
            "\r\n" +
            "+GMM: IsatDataPro Modem Simulator\r\n" +
            "\r\n" +
            "+GMR: 5.003,2.0,10\r\n" +
            "\r\n" +
            "+GMR: 5.003,2.0,10\r\n" +
            "\r\n" +
            "+GMI: ORBCOMM Inc\r\n" +
            "\r\n" +
            "OK\r\n"
    );
    modemResponder.registerHandler("%GPS", at ->
            "%GPS: $GPGGA,224444.000,2142.0675,S,15914.7646,E,1,05,3.0,0.00,M,,,,0000*2E\r\n" +
            "\r\n" +
            "$GPRMC,224444.000,A,2142.0675,S,15914.7646,E,0.00,000.00,250825,,,A*71\r\n" +
            "\r\n" +
            "$GPGSV,1,1,03,1,45,060,50,2,45,120,50,3,45,180,50*72\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    // AT%TRK=10,1  → OK
    modemResponder.registerHandler("%TRK=10,1", at -> "OK\r\n");

    // AT%NETINFO  → %NETINFO: 2,6,0,0,0  + blank line + OK
    modemResponder.registerHandler("%NETINFO", at ->
        "%NETINFO: 2,6,0,0,0 \r\n" +
            "\r\n" +
            "OK\r\n"
    );

    // AT%MTQS  → %MTQS:  (empty) + blank line + OK
    modemResponder.registerHandler("%MTQS", at ->
        "%MTQS: \r\n" +
            "\r\n" +
            "OK\r\n"
    );

    modemResponder.registerHandler("S57", at ->
        "\r\n" +
        "005\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    modemResponder.registerHandler("S56", at ->
        "001\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    modemResponder.registerHandler("S85", at ->
        "00250\r\n" +
            "\r\n" +
            "OK\r\n"
    );

    registerOgxMomt(modemResponder, queue);
    modemResponder.start();
  }

  public static void registerOgxMomt(ModemResponder responder, Queue<byte[]> onMomt) {
    Objects.requireNonNull(responder, "responder");
    Objects.requireNonNull(onMomt, "onMomt");

    responder.registerHandler("%MOMT", at -> {
      String params = at.getParams();                  // "0,2,10,352,3,gQE..."
      if (params != null && !params.isEmpty()) {
        String[] parts = params.split(",", 6);         // keep last field intact
        if (parts.length == 6) {
          String b64 = parts[5].trim();
          try {
            byte[] payload = Base64.getDecoder().decode(b64);
            onMomt.add(payload);
          } catch (IllegalArgumentException ignore) {
            // bad base64; ignore or log in your test harness if needed
          }
        }
      }
      return "OK\r\n";
    });
  }
}
