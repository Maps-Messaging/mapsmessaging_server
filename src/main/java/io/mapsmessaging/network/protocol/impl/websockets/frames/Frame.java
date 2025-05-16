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

package io.mapsmessaging.network.protocol.impl.websockets.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.EndOfBufferException;

import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class Frame implements ServerPacket {

  private static final byte LF = 0xA;
  private static final byte[] END_OF_LINE = {0xD, 0xA};

  protected final Map<String, String> headers;
  protected String request;
  protected boolean isComplete;

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

  public void parse(Packet packet) throws EndOfBufferException {
    request = getHeaderLine(packet);
    boolean endOfHeader = false;
    while (!endOfHeader) {
      String header = getHeaderLine(packet);
      if (header != null) {
        header = header.trim();
        if (!header.isEmpty()) {
          int keyLocale = header.indexOf(':');
          if (keyLocale > 0) {

            String key = header.substring(0, keyLocale).toLowerCase().trim();
            String val = header.substring(keyLocale + 1).trim();
            headers.put(key, val);
          }
        } else {
          isComplete = true;
          endOfHeader = true;
        }
      } else {
        throw new EndOfBufferException();
      }
    }
  }

  String getHeaderLine(Packet packet) throws EndOfBufferException {
    int pos = packet.position();
    while (packet.hasRemaining()) {
      if (packet.get() == LF) {
        byte[] tmp = new byte[packet.position() - pos];
        packet.position(pos);
        packet.get(tmp);
        return new String(tmp);
      }
    }
    packet.position(pos);
    throw new EndOfBufferException("End of Buffer reach");
  }

  public boolean isComplete() {
    return isComplete;
  }

  @Override
  public int packFrame(Packet packet) {
    packet.put(request.getBytes());
    packet.put(END_OF_LINE);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      packet.put(entry.getKey().getBytes());
      packet.put(": ".getBytes());
      packet.put(entry.getValue().getBytes());
      packet.put(END_OF_LINE);
    }
    packet.put(END_OF_LINE);
    int pos = packet.position();
    packet.position(0);
    byte[] test = new byte[pos];
    packet.get(test);
    return 0;
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
