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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public abstract class Frame implements ServerPacket {

  static final byte END_OF_FRAME = 0x00;
  static final byte END_OF_LINE = 0x0A;
  static final byte CARRIAGE_RETURN = 0x0D;
  static final byte DELIMITER = ':';

  private static final int MAX_HEADER_COUNT = 1024;
  private static final int MAX_HEADER_LINE_LENGTH = 8192;

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
  private int parsedHeaderCount;
  private boolean headerEscaping;

  protected Frame() {
    header = new LinkedHashMap<>();
    caseHeader = new LinkedHashMap<>();
    completionHandler = null;
    endOfHeader = false;
    hasEndOfFrame = false;
    parsedHeaderCount = 0;
    headerEscaping = true;
  }

  protected String getHeader(String key) {
    String val = header.get(key);
    if (val == null) {
      String keyLookup = caseHeader.get(normaliseKey(key));
      if (keyLookup != null) {
        val = header.get(keyLookup);
      }
    }
    return val;
  }

  protected void putHeader(String key, String val) {
    String normalised = normaliseKey(key);
    String existing = caseHeader.put(normalised, key);
    if (existing != null && !existing.equals(key)) {
      header.remove(existing);
    }
    header.put(key, val);
  }

  private void putParsedHeader(String key, String val) {
    String normalised = normaliseKey(key);
    if (!caseHeader.containsKey(normalised)) {
      caseHeader.put(normalised, key);
      header.put(key, val);
    }
  }

  protected String removeHeader(String key) {
    String caseKey = caseHeader.remove(normaliseKey(key));
    return caseKey == null ? null : header.remove(caseKey);
  }

  protected boolean headerContainsKey(String key) {
    return caseHeader.containsKey(normaliseKey(key));
  }

  abstract byte[] getCommand();

  public void setHeaderEscaping(boolean headerEscaping) {
    this.headerEscaping = headerEscaping;
  }

  protected boolean escapeHeaders() {
    return headerEscaping;
  }

  protected int packHeader(Packet packet) {
    int start = packet.position();
    packet.put(getCommand());
    packet.put(END_OF_LINE);

    if (receipt != null) {
      putEncodedHeader(packet, "receipt-id", receipt);
    }
    for (Map.Entry<String, String> headerEntry : getHeader().entrySet()) {
      putEncodedHeader(packet, headerEntry.getKey(), headerEntry.getValue());
    }
    packet.put(END_OF_LINE);
    return start;
  }

  private void putEncodedHeader(Packet packet, String key, String value) {
    String encodedKey = escapeHeaders() ? encodeHeader(key) : key;
    String encodedValue = escapeHeaders() ? encodeHeader(value) : value;
    packet.put(encodedKey.getBytes(StandardCharsets.UTF_8));
    packet.put(DELIMITER);
    packet.put(encodedValue.getBytes(StandardCharsets.UTF_8));
    packet.put(END_OF_LINE);
  }

  public int packFrame(Packet packet) {
    int start = packHeader(packet);
    packBody(packet);
    packet.put(END_OF_FRAME);
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
      return;
    }

    int lastValidPos = packet.position();
    if (!endOfHeader) {
      lastValidPos = loadHeader(packet);
    }

    if (endOfHeader && packet.hasRemaining() && scanForEnd && packet.get() == END_OF_FRAME) {
      parseCompleted();
      hasEndOfFrame = true;
      return;
    }
    packet.position(lastValidPos);
    throw new EndOfBufferException("Expecting End Of Frame 0x0");
  }

  private int loadHeader(Packet packet) throws IOException {
    int lastValidPos = packet.position();
    while (packet.hasRemaining() && !endOfHeader) {
      int lineStart = packet.position();
      String line = readLine(packet);
      if (line == null) {
        packet.position(lineStart);
        break;
      }
      lastValidPos = packet.position();
      if (line.isEmpty()) {
        endOfHeader = true;
        break;
      }

      int delimiter = line.indexOf(':');
      if (delimiter <= 0) {
        throw new StompProtocolException("Invalid STOMP header line");
      }
      parsedHeaderCount++;
      if (parsedHeaderCount > MAX_HEADER_COUNT) {
        throw new StompProtocolException("STOMP frame contains too many headers");
      }

      String key = line.substring(0, delimiter);
      String value = line.substring(delimiter + 1);
      if (escapeHeaders()) {
        key = decodeHeader(key);
        value = decodeHeader(value);
      }
      putParsedHeader(key, value);
    }
    return lastValidPos;
  }

  private String readLine(Packet packet) throws StompProtocolException {
    int start = packet.position();
    int length = 0;
    while (packet.hasRemaining()) {
      byte value = packet.get();
      if (value == END_OF_LINE) {
        int end = packet.position() - 1;
        if (end > start && packet.get(end - 1) == CARRIAGE_RETURN) {
          end--;
        }
        byte[] line = new byte[end - start];
        int current = packet.position();
        packet.position(start);
        packet.get(line);
        packet.position(current);
        return new String(line, StandardCharsets.UTF_8);
      }
      length++;
      if (length > MAX_HEADER_LINE_LENGTH) {
        throw new StompProtocolException("STOMP header line exceeds " + MAX_HEADER_LINE_LENGTH + " bytes");
      }
    }
    packet.position(start);
    return null;
  }

  private String decodeHeader(String value) throws StompProtocolException {
    StringBuilder decoded = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      if (current != '\\') {
        decoded.append(current);
        continue;
      }
      if (++index >= value.length()) {
        throw new StompProtocolException("Incomplete STOMP header escape");
      }
      char escaped = value.charAt(index);
      switch (escaped) {
        case 'n' -> decoded.append('\n');
        case 'r' -> decoded.append('\r');
        case 'c' -> decoded.append(':');
        case '\\' -> decoded.append('\\');
        default -> throw new StompProtocolException("Undefined STOMP header escape \\" + escaped);
      }
    }
    return decoded.toString();
  }

  private String encodeHeader(String value) {
    StringBuilder encoded = new StringBuilder(value.length());
    for (int index = 0; index < value.length(); index++) {
      char current = value.charAt(index);
      switch (current) {
        case '\n' -> encoded.append("\\n");
        case '\r' -> encoded.append("\\r");
        case ':' -> encoded.append("\\c");
        case '\\' -> encoded.append("\\\\");
        default -> encoded.append(current);
      }
    }
    return encoded.toString();
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

  private String normaliseKey(String key) {
    return key.toLowerCase(Locale.ROOT);
  }
}
