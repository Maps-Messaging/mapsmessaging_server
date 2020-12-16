/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.frames;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.maps.messaging.api.features.Priority;
import org.maps.network.io.Packet;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.impl.stomp.StompProtocolException;

/**
 * Implements the STOMP Connect frame as per https://stomp.github.io/stomp-specification-1.2.html#SEND
 */
public class Send extends ClientFrame {

  private static final String END_OF_FRAME_MSG = "Expected end of frame";
  private static final String MORE_DATA = "Need more data";

  private final int maxBufferSize;

  private byte[] buffer;
  private int bufferPos;
  private ByteArrayOutputStream byteArrayOutputStream;
  private String transaction;
  private String destination;
  private int priority;
  private long expiry;
  private long delay;

  public Send(int maxBufferSize) {
    this.maxBufferSize = maxBufferSize;
    buffer = null;
    byteArrayOutputStream = null;
    bufferPos = 0;
    priority = 0;
    expiry = 0;
    delay = 0;
  }

  public byte[] getData() {
    return buffer;
  }

  @Override
  public boolean isValid() {
    destination = getHeader().remove("destination");
    transaction = getHeader().remove("transaction");
    getHeader().remove("content-length");
    priority = parseHeaderInt("priority", Priority.NORMAL.getValue());
    expiry = parseHeaderLong("expiry", 0);
    delay = parseHeaderLong("delay", 0);
    return destination != null && destination.length() > 0;
  }

  @Override
  public void parseCompleted() throws IOException {
    super.parseCompleted();
    String lengthString = getHeader("content-length");
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

  @Override
  public String toString() {
    return "STOMP Send[ Transaction:"
        + transaction
        + ", Destination:"
        + destination
        + ", Priority:"
        + priority
        +", Delay:"
        + delay
        + " Header:"
        + getHeaderAsString()
        + "]";
  }
}
