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
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements the STOMP Error frame as per https://stomp.github.io/stomp-specification-1.2.html#ERROR
 */
public class Error extends ServerFrame {

  private static final byte[] COMMAND = "ERROR".getBytes();
  private static final int MAX_ERROR_BODY_SIZE = 64 * 1024;

  private byte[] payload = new byte[0];
  private int payloadPosition;
  private ByteArrayOutputStream streamedPayload;
  private boolean bodyInitialised;

  @Override
  public Frame instance() {
    return new Error();
  }

  byte[] getCommand() {
    return COMMAND;
  }

  @Override
  public void packBody(Packet packet) {
    packet.put(payload);
  }

  public void setContentType(String contentType) {
    putHeader("content-type", contentType);
  }

  public void setContent(byte[] bytes) {
    payload = bytes == null ? new byte[0] : bytes;
    putHeader("content-length", Integer.toString(payload.length));
  }

  public byte[] getContent() {
    return payload;
  }

  @Override
  public void scanFrame(Packet packet) throws IOException {
    if (!endOfHeader) {
      try {
        super.scanFrame(packet, false);
      } catch (EndOfBufferException incompleteHeader) {
        if (!packet.hasRemaining()) {
          throw incompleteHeader;
        }
      }
    }
    if (!endOfHeader) {
      throw new EndOfBufferException("Need more STOMP ERROR header data");
    }

    initialiseBody();
    resume(packet);
    if (!hasEndOfFrame) {
      throw new EndOfBufferException("Need more STOMP ERROR body data");
    }
  }

  @Override
  public void resume(Packet packet) throws EndOfBufferException, StompProtocolException {
    if (streamedPayload != null) {
      loadNullTerminatedBody(packet);
    } else {
      loadLengthBasedBody(packet);
    }
  }

  private void initialiseBody() throws IOException {
    if (bodyInitialised) {
      return;
    }
    bodyInitialised = true;
    super.parseCompleted();
    String lengthValue = getHeader("content-length");
    if (lengthValue == null) {
      streamedPayload = new ByteArrayOutputStream(256);
      return;
    }

    int length;
    try {
      length = Integer.parseInt(lengthValue.trim());
    } catch (NumberFormatException invalidLength) {
      throw new StompProtocolException("Invalid STOMP ERROR content-length " + lengthValue);
    }
    if (length < 0 || length > MAX_ERROR_BODY_SIZE) {
      throw new StompProtocolException(
          "STOMP ERROR body exceeds supported maximum of " + MAX_ERROR_BODY_SIZE + " bytes");
    }
    payload = new byte[length];
    payloadPosition = 0;
  }

  private void loadLengthBasedBody(Packet packet)
      throws EndOfBufferException, StompProtocolException {
    int count = Math.min(packet.available(), payload.length - payloadPosition);
    if (count > 0) {
      packet.get(payload, payloadPosition, count);
      payloadPosition += count;
    }
    if (payloadPosition != payload.length) {
      throw new EndOfBufferException("Need more STOMP ERROR body data");
    }
    if (!packet.hasRemaining()) {
      throw new EndOfBufferException("Need STOMP ERROR frame terminator");
    }
    if (packet.get() != END_OF_FRAME) {
      throw new StompProtocolException("STOMP ERROR frame is missing its NUL terminator");
    }
    hasEndOfFrame = true;
  }

  private void loadNullTerminatedBody(Packet packet)
      throws EndOfBufferException, StompProtocolException {
    while (packet.hasRemaining()) {
      byte value = packet.get();
      if (value == END_OF_FRAME) {
        payload = streamedPayload.toByteArray();
        hasEndOfFrame = true;
        return;
      }
      if (streamedPayload.size() >= MAX_ERROR_BODY_SIZE) {
        throw new StompProtocolException(
            "STOMP ERROR body exceeds supported maximum of " + MAX_ERROR_BODY_SIZE + " bytes");
      }
      streamedPayload.write(value);
    }
    throw new EndOfBufferException("Need more STOMP ERROR body data");
  }

  @Override
  public String toString() {
    return "STOMP Error[ Header:" + getHeaderAsString() + "]";
  }
}
