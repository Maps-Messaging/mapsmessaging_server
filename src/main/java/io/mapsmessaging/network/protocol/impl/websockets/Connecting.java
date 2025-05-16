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

package io.mapsmessaging.network.protocol.impl.websockets;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.websockets.frames.AcceptFrame;
import io.mapsmessaging.network.protocol.impl.websockets.frames.Frame;
import io.mapsmessaging.network.protocol.impl.websockets.frames.GetFrame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.StringTokenizer;

public class Connecting {

  private static final String STOMP = "stomp";
  private static final String MQTT = "mqtt";
  private static final String AMQP = "amqp";

  private static final int PROTOCOL_NAME = 0;
  private static final int IANA_NAME = 1;
  private static final String[][] SUB_PROTOCOL_MAP = {{STOMP, "v10.stomp"}, {STOMP, "v11.stomp"}, {STOMP, "v12.stomp"}, {MQTT, "mqtt"}, {AMQP, "amqp"}};

  /*
    https://en.wikipedia.org/wiki/WebSocket

    Web Sockets secret UUID to use on the server
   */
  public static final String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

  private final GetFrame getFrame;

  public Connecting() {
    getFrame = new GetFrame();
  }

  public Frame handle(Packet packet, EndPoint endPoint) throws IOException {
    getFrame.parse(packet);
    if (getFrame.isComplete()) {
      AcceptFrame acceptFrame = new AcceptFrame();
      Map<String, String> headers = acceptFrame.getHeaders();
      headers.put("Upgrade", "websocket");
      headers.put("Connection", "Upgrade");
      String acceptKey = generateAcceptKey(getFrame);
      if (acceptKey != null) {
        headers.put("Sec-WebSocket-Accept", acceptKey);
      }
      if (getFrame.getHeaders().containsKey("sec-websocket-protocol")) {
        processProtocols(endPoint, headers);
      }
      headers.put("Accept-Encoding", "gzip, deflate");
      return acceptFrame;
    }
    return null;
  }

  private void processProtocols(EndPoint endPoint, Map<String, String> headers) {
    String protocols = endPoint.getServer().getConfig().getProtocols();
    String clientProtocols = getFrame.getHeaders().get("sec-websocket-protocol");
    String subProtocol = "";
    int isList = clientProtocols.indexOf(',');
    if (isList > 0) {
      StringTokenizer st = new StringTokenizer(clientProtocols, ",");
      boolean found = false;
      while (!found && st.hasMoreElements()) {
        String result = isSupported((String) st.nextElement(), protocols);
        if (result != null) {
          found = true;
          subProtocol = result;
        }
      }
    } else {
      clientProtocols = clientProtocols.trim();
      subProtocol = isSupported(clientProtocols, protocols);
    }
    headers.put("Sec-WebSocket-Protocol", subProtocol);
  }

  private String isSupported(String ianaName, String protocols) {
    for (String[] protocol : SUB_PROTOCOL_MAP) {
      if (protocol[IANA_NAME].equalsIgnoreCase(ianaName) &&
          protocols.toLowerCase().contains(protocol[PROTOCOL_NAME])) {
        return protocol[IANA_NAME];
      }
    }
    return null;
  }

  // This is part of the WebSocket handshake standard so we can safely assume the use of a digest is done correctly
  @java.lang.SuppressWarnings("squid:S4790")
  String generateAcceptKey(GetFrame getFrame) throws IOException {
    String webSocketKey = getFrame.getHeaders().get(GetFrame.SEC_WEBSOCKET_KEY_HEADER);
    if (webSocketKey != null) {
      webSocketKey = webSocketKey.trim();
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] result = digest.digest((webSocketKey + MAGIC_STRING).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
      } catch (NoSuchAlgorithmException e) {
        throw new IOException("Unable to create Accept Key", e);
      }
    }
    return null;
  }
}

