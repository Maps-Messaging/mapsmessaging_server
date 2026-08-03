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

package io.mapsmessaging.network.protocol.impl.websockets.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.EndOfBufferException;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Frame implements ServerPacket {

  private static final byte LF = 0xA;
  private static final int MAX_HEADER_BYTES = 8 * 1024;
  private static final byte[] END_OF_LINE = {0xD, 0xA};

  protected final Map<String, String> headers;
  protected String request;
  protected boolean isComplete;
  private int headerBytes;

  public Frame() {
    headers = new LinkedHashMap<>();
  }

  public Frame(String request) {
    this();
    this.request = request;
    isComplete = true;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getRequest() {
    return request;
  }

  public void parse(Packet packet) throws IOException {
    if (isComplete) {
      return;
    }
    if (request == null) {
      request = getHeaderLine(packet).trim();
    }

    while (!isComplete) {
      String header = getHeaderLine(packet).trim();
      if (header.isEmpty()) {
        isComplete = true;
        return;
      }

      int keyLocale = header.indexOf(':');
      if (keyLocale > 0) {
        String key = header.substring(0, keyLocale).toLowerCase().trim();
        String value = header.substring(keyLocale + 1).trim();
        headers.put(key, value);
      }
    }
  }

  String getHeaderLine(Packet packet) throws IOException {
    int position = packet.position();
    while (packet.hasRemaining()) {
      if (packet.get() == LF) {
        int lineLength = packet.position() - position;
        enforceHeaderLimit(lineLength);
        headerBytes += lineLength;
        byte[] value = new byte[lineLength];
        packet.position(position);
        packet.get(value);
        return new String(value, StandardCharsets.ISO_8859_1);
      }
      enforceHeaderLimit(packet.position() - position);
    }
    packet.position(position);
    throw new EndOfBufferException("Incomplete HTTP header line");
  }

  private void enforceHeaderLimit(int pendingLineBytes) throws IOException {
    if (headerBytes + pendingLineBytes > MAX_HEADER_BYTES) {
      throw new IOException("WebSocket HTTP upgrade headers exceed " + MAX_HEADER_BYTES + " bytes");
    }
  }

  public boolean isComplete() {
    return isComplete;
  }

  @Override
  public int packFrame(Packet packet) {
    int initialPosition = packet.position();
    packet.put(request.getBytes(StandardCharsets.US_ASCII));
    packet.put(END_OF_LINE);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      packet.put(entry.getKey().getBytes(StandardCharsets.US_ASCII));
      packet.put((byte) ':');
      packet.put((byte) ' ');
      packet.put(entry.getValue().getBytes(StandardCharsets.US_ASCII));
      packet.put(END_OF_LINE);
    }
    packet.put(END_OF_LINE);
    return packet.position() - initialPosition;
  }

  @Override
  public void complete() {
    // Nothing to do here
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
