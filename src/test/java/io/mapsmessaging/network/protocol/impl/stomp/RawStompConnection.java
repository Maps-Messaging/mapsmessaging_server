/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.stomp;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

final class RawStompConnection implements AutoCloseable {

  private final Socket socket;
  private final InputStream input;
  private final OutputStream output;

  RawStompConnection(int port) throws IOException {
    socket = new Socket("127.0.0.1", port);
    socket.setSoTimeout(5000);
    socket.setTcpNoDelay(true);
    input = socket.getInputStream();
    output = socket.getOutputStream();
  }

  void connect(String version, String heartbeat) throws IOException {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("accept-version", version);
    headers.put("host", "localhost");
    headers.put("heart-beat", heartbeat);
    send("STOMP", headers, new byte[0], false, false);
  }

  void send(
      String command,
      Map<String, String> headers,
      byte[] body,
      boolean crlf,
      boolean includeContentLength) throws IOException {
    String eol = crlf ? "\r\n" : "\n";
    ByteArrayOutputStream frame = new ByteArrayOutputStream();
    frame.writeBytes((command + eol).getBytes(StandardCharsets.UTF_8));
    for (Map.Entry<String, String> header : headers.entrySet()) {
      frame.writeBytes(
          (header.getKey() + ":" + header.getValue() + eol)
              .getBytes(StandardCharsets.UTF_8));
    }
    if (includeContentLength) {
      frame.writeBytes(
          ("content-length:" + body.length + eol).getBytes(StandardCharsets.UTF_8));
    }
    frame.writeBytes(eol.getBytes(StandardCharsets.UTF_8));
    frame.writeBytes(body);
    frame.write(0);
    sendBytes(frame.toByteArray());
  }

  void sendBytes(byte[] bytes) throws IOException {
    output.write(bytes);
    output.flush();
  }

  StompFrame readFrame() throws IOException {
    String command = readCommand();
    Map<String, String> headers = new LinkedHashMap<>();
    String line;
    while (!(line = readLine()).isEmpty()) {
      int separator = line.indexOf(':');
      if (separator <= 0) {
        throw new IOException("Invalid STOMP response header " + line);
      }
      headers.putIfAbsent(line.substring(0, separator), line.substring(separator + 1));
    }

    byte[] body;
    String contentLength = headers.get("content-length");
    if (contentLength != null) {
      int length = Integer.parseInt(contentLength);
      body = input.readNBytes(length);
      if (body.length != length) {
        throw new EOFException("Connection closed during STOMP body");
      }
      if (readRequired() != 0) {
        throw new IOException("STOMP frame is missing its NUL terminator");
      }
    } else {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      int value;
      while ((value = readRequired()) != 0) {
        output.write(value);
      }
      body = output.toByteArray();
    }
    return new StompFrame(command, headers, body);
  }

  int read() throws IOException {
    return input.read();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  private String readCommand() throws IOException {
    String command;
    do {
      command = readLine();
    } while (command.isEmpty());
    return command;
  }

  private String readLine() throws IOException {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    while (true) {
      int value = readRequired();
      if (value == '\n') {
        byte[] bytes = line.toByteArray();
        int length = bytes.length;
        if (length > 0 && bytes[length - 1] == '\r') {
          length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
      }
      line.write(value);
    }
  }

  private int readRequired() throws IOException {
    int value = input.read();
    if (value < 0) {
      throw new EOFException("Connection closed while reading STOMP frame");
    }
    return value;
  }

  record StompFrame(String command, Map<String, String> headers, byte[] body) {
    byte[] decodedBody() {
      if ("base64".equalsIgnoreCase(headers.get("encoding"))) {
        return Base64.getDecoder().decode(body);
      }
      return body;
    }

    String bodyText() {
      return new String(decodedBody(), StandardCharsets.UTF_8);
    }
  }
}
