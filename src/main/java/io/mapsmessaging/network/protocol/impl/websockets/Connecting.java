/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.websockets;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.websockets.frames.AcceptFrame;
import io.mapsmessaging.network.protocol.impl.websockets.frames.Frame;
import io.mapsmessaging.network.protocol.impl.websockets.frames.GetFrame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class Connecting {

  private static final String STOMP = "stomp";
  private static final String MQTT = "mqtt";
  private static final String AMQP = "amqp";

  private static final int PROTOCOL_NAME = 0;
  private static final int IANA_NAME = 1;
  private static final String[][] SUB_PROTOCOL_MAP = {
      {STOMP, "v10.stomp"},
      {STOMP, "v11.stomp"},
      {STOMP, "v12.stomp"},
      {MQTT, "mqtt"},
      {AMQP, "amqp"}
  };

  public static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private final GetFrame getFrame;

  public Connecting() {
    getFrame = new GetFrame();
  }

  public Frame handle(Packet packet, EndPoint endPoint) throws IOException {
    try {
      getFrame.parse(packet);
    } catch (EndOfBufferException incomplete) {
      return null;
    }
    if (!getFrame.isComplete()) {
      return null;
    }

    validateHandshake();
    AcceptFrame acceptFrame = new AcceptFrame();
    Map<String, String> headers = acceptFrame.getHeaders();
    headers.put("Upgrade", "websocket");
    headers.put("Connection", "Upgrade");
    headers.put("Sec-WebSocket-Accept", generateAcceptKey(getFrame));

    String requestedProtocols = getFrame.getHeaders().get("sec-websocket-protocol");
    if (requestedProtocols != null) {
      String selectedProtocol = selectProtocol(requestedProtocols, endPoint.getServer().getConfig().getProtocols());
      if (selectedProtocol == null) {
        throw new IOException("No requested WebSocket subprotocol is enabled");
      }
      headers.put("Sec-WebSocket-Protocol", selectedProtocol);
    }
    return acceptFrame;
  }

  private void validateHandshake() throws IOException {
    String request = getFrame.getRequest();
    if (request == null || !request.matches("GET\\s+\\S+\\s+HTTP/1\\.1")) {
      throw new IOException("Invalid WebSocket HTTP request line");
    }
    requireHeaderToken("upgrade", "websocket");
    requireHeaderToken("connection", "upgrade");

    String version = getFrame.getHeaders().get(GetFrame.SEC_WEBSOCKET_VERSION_HEADER);
    if (!"13".equals(version)) {
      throw new IOException("Unsupported WebSocket version " + version);
    }

    String key = getFrame.getHeaders().get(GetFrame.SEC_WEBSOCKET_KEY_HEADER);
    if (key == null) {
      throw new IOException("Missing Sec-WebSocket-Key header");
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(key.trim());
      if (decoded.length != 16) {
        throw new IOException("Sec-WebSocket-Key must decode to 16 bytes");
      }
    } catch (IllegalArgumentException invalidBase64) {
      throw new IOException("Sec-WebSocket-Key is not valid Base64", invalidBase64);
    }
  }

  private void requireHeaderToken(String headerName, String requiredToken) throws IOException {
    String value = getFrame.getHeaders().get(headerName);
    if (value == null) {
      throw new IOException("Missing WebSocket " + headerName + " header");
    }
    StringTokenizer tokenizer = new StringTokenizer(value, ",");
    while (tokenizer.hasMoreTokens()) {
      if (requiredToken.equalsIgnoreCase(tokenizer.nextToken().trim())) {
        return;
      }
    }
    throw new IOException("WebSocket " + headerName + " header does not contain " + requiredToken);
  }

  private String selectProtocol(String clientProtocols, String enabledProtocols) {
    StringTokenizer tokenizer = new StringTokenizer(clientProtocols, ",");
    while (tokenizer.hasMoreTokens()) {
      String selected = isSupported(tokenizer.nextToken().trim(), enabledProtocols);
      if (selected != null) {
        return selected;
      }
    }
    return null;
  }

  private String isSupported(String ianaName, String enabledProtocols) {
    String configured = enabledProtocols == null ? "" : enabledProtocols.toLowerCase(Locale.ROOT);
    for (String[] protocol : SUB_PROTOCOL_MAP) {
      if (protocol[IANA_NAME].equalsIgnoreCase(ianaName)
          && configured.contains(protocol[PROTOCOL_NAME])) {
        return protocol[IANA_NAME];
      }
    }
    return null;
  }

  @java.lang.SuppressWarnings("squid:S4790")
  String generateAcceptKey(GetFrame frame) throws IOException {
    String webSocketKey = frame.getHeaders().get(GetFrame.SEC_WEBSOCKET_KEY_HEADER);
    if (webSocketKey == null) {
      throw new IOException("Missing Sec-WebSocket-Key header");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] result = digest.digest((webSocketKey.trim() + MAGIC_STRING).getBytes(StandardCharsets.US_ASCII));
      return Base64.getEncoder().encodeToString(result);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("Unable to create WebSocket accept key", e);
    }
  }
}
