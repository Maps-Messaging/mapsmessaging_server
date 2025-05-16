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

package io.mapsmessaging.network.protocol.impl.nats.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocolException;
import io.mapsmessaging.network.protocol.impl.nats.listener.FrameListener;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Setter
@Getter
public abstract class NatsFrame implements ServerPacket {

  private String receipt;
  private CompletionHandler completionHandler;
  private FrameListener listener;

  public NatsFrame() {
    completionHandler = null;
  }

  public abstract byte[] getCommand();

  protected void parseLine(String line) throws NatsProtocolException {
  }

  public abstract boolean isValid();

  public abstract NatsFrame instance();


  public void parseFrame(Packet packet) throws IOException {
    parseLine(extractLine(packet));
  }

  protected String extractLine(Packet packet) throws IOException {
    int start = packet.position();
    while (packet.hasRemaining()) {
      byte b = packet.get();
      if (b == '\r') {
        if (!packet.hasRemaining()) {
          throw new EndOfBufferException("Need more data after CR");
        }
        byte next = packet.get();
        if (next == '\n') {
          // We found CRLF
          int end = packet.position();
          int length = end - start - 2; // exclude CRLF
          byte[] jsonBytes = new byte[length];
          packet.position(start);
          packet.get(jsonBytes);
          String line = new String(jsonBytes, StandardCharsets.US_ASCII);
          packet.position(end);
          return line;
        } else {
          throw new IOException("Invalid NATS frame: CR not followed by LF");
        }
      }
    }
    throw new EndOfBufferException("Incomplete NATS frame");
  }

  public CompletionHandler getCallback() {
    return completionHandler;
  }

  public void setCallback(CompletionHandler completion) {
    completionHandler = completion;
  }


  public void complete() {
    CompletionHandler tmp;
    synchronized (this) {
      tmp = completionHandler;
      completionHandler = null;
    }
    if (tmp != null) {
      tmp.run();
    }
  }


  @Override
  public int packFrame(Packet packet) {
    byte[] command = getCommand();
    packet.put(command);
    packet.put("\r\n".getBytes());
    return command.length + 2; // bytes written
  }


  protected boolean extractBoolean(String json, String key) {
    int idx = json.indexOf(key);
    if (idx >= 0) {
      int valStart = idx + key.length();
      int valEnd = json.indexOf(',', valStart);
      if (valEnd == -1) valEnd = json.indexOf('}', valStart);
      if (valEnd > valStart) {
        String val = json.substring(valStart, valEnd).trim();
        return "true".equalsIgnoreCase(val);
      }
    }
    return false;
  }

  protected String extractString(String json, String key) {
    int idx = json.indexOf(key);
    if (idx >= 0) {
      int valStart = idx + key.length();
      while (valStart < json.length() && (json.charAt(valStart) == '"' || json.charAt(valStart) == ' ')) valStart++;
      int valEnd = json.indexOf('"', valStart);
      if (valEnd > valStart) {
        return json.substring(valStart, valEnd);
      }
    }
    return null;
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}


