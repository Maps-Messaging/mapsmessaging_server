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

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public abstract class Event extends Frame {

  private static final String CONTENT_LENGTH = "content-length";
  private static final String END_OF_FRAME_MSG = "Expected end of frame";
  private static final String MORE_DATA = "Need more data";
  private static final String ENCODED = "encoding";

  protected final int maxBufferSize;

  protected byte[] buffer;
  protected int bufferPos;
  private ByteArrayOutputStream byteArrayOutputStream;
  @Getter
  protected String transaction;
  @Getter
  protected String destination;
  @Getter
  protected int priority;
  @Getter
  protected long expiry;
  @Getter
  protected long delay;

  @Getter
  protected boolean base64Encode;

  private String encodedString;

  protected Event(int maxBufferSize, boolean base64Encode) {
    this.maxBufferSize = Math.max(0, maxBufferSize);
    this.base64Encode = base64Encode;
    buffer = null;
    byteArrayOutputStream = null;
    bufferPos = 0;
    priority = 0;
    expiry = 0;
    delay = 0;
  }

  public void packMessage(String destination, Message internalMessage) {
    Map<String, TypedData> dataMap = internalMessage.getDataMap();
    for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
      putHeader(entry.getKey(), entry.getValue().getData().toString());
    }

    Map<String, String> metaMap = internalMessage.getMeta();
    if (metaMap != null) {
      for (Map.Entry<String, String> entry : metaMap.entrySet()) {
        putHeader(entry.getKey(), entry.getValue());
      }
    }
    putHeader("destination", destination);

    buffer = internalMessage.getOpaqueData();
    if (buffer == null) {
      buffer = new byte[0];
    }
    if (base64Encode) {
      putHeader(ENCODED, "base64");
      buffer = Base64.getEncoder().encode(buffer);
    }
    if (buffer.length > 0) {
      putHeader(CONTENT_LENGTH, Integer.toString(buffer.length));
    }
  }

  public byte[] getData() {
    if (encodedString != null && encodedString.equalsIgnoreCase("base64")) {
      return Base64.getDecoder().decode(buffer);
    }
    return buffer;
  }

  @Override
  public void packBody(Packet packet) {
    if (buffer != null && buffer.length > 0) {
      packet.put(buffer);
    }
  }

  @Override
  public boolean isValid() {
    destination = removeHeader("destination");
    transaction = removeHeader("transaction");
    removeHeader(CONTENT_LENGTH);
    priority = parseHeaderInt("priority", Priority.NORMAL.getValue());
    expiry = parseHeaderLong("expiry", 0);
    delay = parseHeaderLong("delay", 0);
    return destination != null && !destination.isEmpty();
  }

  @Override
  public void parseCompleted() throws IOException {
    super.parseCompleted();
    String lengthString = getHeader(CONTENT_LENGTH);
    encodedString = getHeader(ENCODED);
    if (lengthString == null) {
      byteArrayOutputStream = new ByteArrayOutputStream(Math.min(maxBufferSize, 1024));
      return;
    }

    int length;
    try {
      length = Integer.parseInt(lengthString.trim());
    } catch (NumberFormatException e) {
      throw new StompProtocolException("Invalid STOMP content-length " + lengthString);
    }
    if (length < 0) {
      throw new StompProtocolException("STOMP content-length must not be negative");
    }
    if (length > maxBufferSize) {
      throw new StompProtocolException(
          "STOMP frame body exceeds configured maximum, received "
              + length
              + ", configured for "
              + maxBufferSize);
    }
    buffer = new byte[length];
    bufferPos = 0;
    byteArrayOutputStream = null;
  }

  @Override
  public void scanFrame(Packet packet) throws IOException {
    if (!endOfHeader) {
      try {
        super.scanFrame(packet, false);
      } catch (EndOfBufferException e) {
        if (!packet.hasRemaining()) {
          throw e;
        }
      }
    }

    if (!endOfHeader) {
      throw new EndOfBufferException(MORE_DATA);
    }

    initialiseBodyReader(packet);
    resume(packet);
    if (!hasEndOfFrame) {
      throw new EndOfBufferException(MORE_DATA);
    }
  }

  private void initialiseBodyReader(Packet packet) throws IOException {
    if (buffer == null && byteArrayOutputStream == null) {
      parseCompleted();
      if (!packet.hasRemaining()) {
        throw new EndOfBufferException();
      }
    }
  }

  @Override
  public void resume(Packet packet) throws EndOfBufferException, StompProtocolException {
    if (byteArrayOutputStream != null) {
      loadNullTerminatedBuffer(packet);
    } else {
      loadLengthBasedBuffer(packet);
    }
  }

  private void loadLengthBasedBuffer(Packet packet)
      throws EndOfBufferException, StompProtocolException {
    int len = Math.min(packet.available(), buffer.length - bufferPos);
    if (len > 0) {
      packet.get(buffer, bufferPos, len);
      bufferPos += len;
    }
    if (bufferPos != buffer.length) {
      throw new EndOfBufferException(MORE_DATA);
    }
    if (!packet.hasRemaining()) {
      throw new EndOfBufferException(MORE_DATA);
    }
    if (packet.get() != END_OF_FRAME) {
      throw new StompProtocolException(END_OF_FRAME_MSG);
    }
    hasEndOfFrame = true;
  }

  private void loadNullTerminatedBuffer(Packet packet)
      throws EndOfBufferException, StompProtocolException {
    while (packet.hasRemaining()) {
      byte value = packet.get();
      if (value == END_OF_FRAME) {
        buffer = byteArrayOutputStream.toByteArray();
        hasEndOfFrame = true;
        return;
      }
      if (byteArrayOutputStream.size() >= maxBufferSize) {
        throw new StompProtocolException(
            "STOMP frame body exceeds configured maximum of " + maxBufferSize + " bytes");
      }
      byteArrayOutputStream.write(value);
    }
    throw new EndOfBufferException(MORE_DATA);
  }

  @Override
  public Frame instance() {
    return new Send(maxBufferSize, base64Encode);
  }
}
