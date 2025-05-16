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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.listener.FrameListener;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Frame implements ServerPacket {

  static final byte END_OF_FRAME = 0x00;
  static final byte END_OF_LINE = 0x0A;
  static final byte DELIMITER = ':';

  @Getter
  private final Map<String, String> header;
  private final Map<String, String> caseHeader;
  @Getter
  private FrameListener frameListener;

  protected boolean endOfHeader;
  protected boolean hasEndOfFrame;

  @Setter
  @Getter
  String receipt;
  private CompletionHandler completionHandler;

  protected Frame() {
    header = new LinkedHashMap<>();
    caseHeader = new LinkedHashMap<>();
    completionHandler = null;
    endOfHeader = false;
    hasEndOfFrame = false;
  }

  protected String getHeader(String key) {
    String val = header.get(key);
    if (val == null) {
      String keyLookup = caseHeader.get(key.toLowerCase());
      if (keyLookup != null) {
        val = header.get(keyLookup);
      }
    }
    return val;
  }

  protected void putHeader(String key, String val) {
    header.put(key, val);
    caseHeader.put(key.toLowerCase(), key);
  }

  protected String removeHeader(String key) {
    String caseKey = caseHeader.remove(key.toLowerCase());
    return header.remove(caseKey);
  }

  protected boolean headerContainsKey(String key) {
    return header.containsKey(key.toLowerCase());
  }

  abstract byte[] getCommand();

  protected int packHeader(Packet packet){
    int start = packet.position();
    //
    // Pack the command
    //
    packet.put(getCommand());
    packet.put(END_OF_LINE);

    //
    // Pack the header
    //
    if (receipt != null) {
      packet.put("receipt-id".getBytes());
      packet.put(DELIMITER);
      packet.put(receipt.getBytes());
      packet.put(END_OF_LINE);
    }
    for (Map.Entry<String, String> headerEntry : getHeader().entrySet()) {
      packet.put(headerEntry.getKey().getBytes());
      packet.put(DELIMITER);
      packet.put(headerEntry.getValue().getBytes());
      packet.put(END_OF_LINE);
    }
    packet.put(END_OF_LINE);
    return start;
  }

  public int packFrame(Packet packet) {
    int start = packHeader(packet);
    packBody(packet);
    packet.put((byte) 0x0);
    return packet.position() - start;
  }

  void packBody(Packet packet) {
    // requires the extending class to provide this mechanism, if one is required
  }

  public abstract Frame instance();

  public CompletionHandler getCallback() {
    return completionHandler;
  }

  public void setCallback(CompletionHandler completion) {
    completionHandler = completion;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().toString());
    if (receipt != null) {
      sb.append("::").append(receipt);
    }
    return sb.toString();
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

  public void setListener(FrameListener frameListener) {
    this.frameListener = frameListener;
  }

  public String getHeaderAsString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : header.entrySet()) {
      sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
    }
    if (receipt != null) {
      sb.append("Receipt:").append(receipt);
    }
    return sb.toString();
  }

  public void scanFrame(Packet packet) throws IOException {
    scanFrame(packet, true);
  }

  public void scanFrame(Packet packet, boolean scanForEnd) throws IOException {
    if (hasEndOfFrame) {
      resume(packet);
    } else {
      int lastValidPos = 0;
      int pos;
      if (!endOfHeader) {
        pos = packet.position();
        lastValidPos = loadHeader(packet, pos);
      }

      if (endOfHeader && packet.hasRemaining() && scanForEnd && packet.get() == END_OF_FRAME) {
        parseCompleted();
        hasEndOfFrame = true;
        return;
      }
      packet.position(lastValidPos);
      throw new EndOfBufferException("Expecting End Of Frame 0x0");
    }
  }

  private int loadHeader(Packet packet, int pos) {
    StringBuilder keyBuilder = new StringBuilder();
    StringBuilder valBuilder = new StringBuilder();
    boolean isKey = true;
    int lastValidPos = pos;
    while (packet.limit() != packet.position() && !endOfHeader) {
      pos++;
      byte t = packet.get();
      if (t == Frame.END_OF_LINE) {
        if (isKey && keyBuilder.length() == 0) {
          endOfHeader = true;
          lastValidPos = pos;
          break;
        } else {
          putHeader(keyBuilder.toString(), valBuilder.toString());
          keyBuilder = new StringBuilder();
          valBuilder = new StringBuilder();
          isKey = true;
          lastValidPos = pos;
        }
      } else if (t == DELIMITER) {
        isKey = false;
      } else {
        if (isKey) {
          keyBuilder.append((char) t);
        } else {
          valBuilder.append((char) t);
        }
      }
    }
    return lastValidPos;
  }


  protected int parseHeaderInt(String key, int def) {
    String value = getHeader(key);
    if (value != null) {
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        // We ignore this as its not a number and return the default
      }
    }
    return def;
  }


  public int getReceiveMaximum() {
    String val = getHeader("receivemaximum");
    if (val != null) {
      try {
        return Integer.parseInt(val.trim());
      } catch (NumberFormatException e) {
        // Invalid number supplied so just return 0 and use the default
      }
    }
    return 0;
  }

  protected long parseHeaderLong(String key, long def) {
    String value = getHeader(key);
    if (value != null) {
      try {
        return Long.parseLong(value.trim());
      } catch (NumberFormatException e) {
        // We ignore this as its not a number and return the default
      }
    }
    return def;
  }

  // This class doesn't throw it, however, extending classes do
  @java.lang.SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
  public void parseCompleted() throws IOException {
    receipt = removeHeader("receipt");
  }

  public abstract boolean isValid();

  public void resume(Packet packet) throws EndOfBufferException, StompProtocolException {
  }

  @Override
  public SocketAddress getFromAddress() {
    return null;
  }
}
