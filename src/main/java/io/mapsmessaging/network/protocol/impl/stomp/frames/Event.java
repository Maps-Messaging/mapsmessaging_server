/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public abstract class Event extends Frame {

  private static final String CONTENT_LENGTH = "content-length";
  private static final String END_OF_FRAME_MSG = "Expected end of frame";
  private static final String MORE_DATA = "Need more data";

  protected final int maxBufferSize;

  protected byte[] buffer;
  protected int bufferPos;
  private ByteArrayOutputStream byteArrayOutputStream;
  protected String transaction;
  protected String destination;
  protected int priority;
  protected long expiry;
  protected long delay;

  protected Event(int maxBufferSize) {
    this.maxBufferSize = maxBufferSize;
    buffer = null;
    byteArrayOutputStream = null;
    bufferPos = 0;
    priority = 0;
    expiry = 0;
    delay = 0;
  }

  public void packMessage(String destination, Message internalMessage) {
    //
    // Map the data map to the header
    //
    Map<String, TypedData> dataMap = internalMessage.getDataMap();
    for (Map.Entry<String, TypedData> entry : dataMap.entrySet()) {
      putHeader(entry.getKey(), entry.getValue().getData().toString());
    }

    //
    // Map the meta data to the header
    //
    Map<String, String> metaMap = internalMessage.getMeta();
    if (metaMap != null) {
      for (Map.Entry<String, String> entry : metaMap.entrySet()) {
        putHeader(entry.getKey(), entry.getValue());
      }
    }

    //
    // Ensure the defined header messages are correct and not driven by the entries in the map
    //
    putHeader("destination", destination);
    if (internalMessage.getOpaqueData() != null && internalMessage.getOpaqueData().length > 0) {
      putHeader(CONTENT_LENGTH, "" + internalMessage.getOpaqueData().length);
    }
    buffer = internalMessage.getOpaqueData();
  }

  public byte[] getData() {
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
    destination = getHeader().remove("destination");
    transaction = getHeader().remove("transaction");
    getHeader().remove(CONTENT_LENGTH);
    priority = parseHeaderInt("priority", Priority.NORMAL.getValue());
    expiry = parseHeaderLong("expiry", 0);
    delay = parseHeaderLong("delay", 0);
    return destination != null && destination.length() > 0;
  }

  @Override
  public void parseCompleted() throws IOException {
    super.parseCompleted();
    String lengthString = getHeader(CONTENT_LENGTH);
    if (lengthString != null) {
      lengthString = lengthString.trim();
      int length = Integer.parseInt(lengthString);
      if (length > maxBufferSize) {
        throw new IOException("Send frame body larger than configured size, sending " + length + ", configured for " + maxBufferSize);
      }
      if (buffer == null) {
        buffer = new byte[length];
        bufferPos = 0;
        byteArrayOutputStream = null;
      }
    } else {
      byteArrayOutputStream = new ByteArrayOutputStream();
    }
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

    if (endOfHeader) {
      readBuffer(packet);
      resume(packet);

      if (!hasEndOfFrame && packet.hasRemaining()) {
        if (packet.get() != END_OF_FRAME) {
          throw new EndOfBufferException(END_OF_FRAME_MSG);
        }
        hasEndOfFrame = true;
      }
      return;
    }
    throw new EndOfBufferException(MORE_DATA);
  }

  private void readBuffer(Packet packet) throws IOException {
    if (buffer == null && byteArrayOutputStream == null) {
      parseCompleted();
      if (packet.position() == packet.limit()) {
        throw new EndOfBufferException();
      }
    }
  }

  @Override
  public void resume(Packet packet) throws EndOfBufferException, StompProtocolException {
    if (byteArrayOutputStream != null) {
      loadBuffer(packet);
    } else {
      loadLengthBasedBuffer(packet);
    }
  }

  private void loadLengthBasedBuffer(Packet packet) throws EndOfBufferException, StompProtocolException {
    int len = Math.min(packet.limit() - packet.position(), buffer.length - bufferPos);
    if (len != 0) {
      packet.get(buffer, bufferPos, len);
      bufferPos += len;

      //
      // Check to see if we need more data
      //
      if (bufferPos != buffer.length) {
        throw new EndOfBufferException();
      }
    }

    if (packet.position() != packet.limit()) {
      int eof = packet.get();
      if (eof != END_OF_FRAME) {
        throw new StompProtocolException(END_OF_FRAME_MSG);
      }
      hasEndOfFrame = true;
    } else {
      throw new EndOfBufferException(MORE_DATA);
    }
  }

  private void loadBuffer(Packet packet) throws EndOfBufferException {
    byte t = packet.get();
    int pos = packet.position();
    while (t != 0) {
      byteArrayOutputStream.write(t);
      if (pos >= packet.limit()) {
        packet.position(pos);
        throw new EndOfBufferException(MORE_DATA);
      }
      t = packet.get(pos);
      pos++;
    }
    packet.position(pos - 1);
    buffer = byteArrayOutputStream.toByteArray();
  }

  @Override
  public Frame instance() {
    return new Send(maxBufferSize);
  }

  public String getDestination() {
    return destination;
  }

  public String getTransaction() {
    return transaction;
  }

  public int getPriority() {
    return priority;
  }

  public long getExpiry() {
    return expiry;
  }

  public long getDelay() {
    return delay;
  }
}