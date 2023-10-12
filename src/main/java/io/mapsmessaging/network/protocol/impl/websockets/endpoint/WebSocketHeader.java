/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.websockets.endpoint;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;

import java.io.IOException;

public class WebSocketHeader {

  protected static final int CONTINUATION = 0x0;
  protected static final int TEXT = 0x1;
  protected static final int BINARY = 0x2;
  protected static final int CLOSE = 0x8;
  protected static final int PING = 0x9;
  protected static final int PONG = 0xA;


  private static final int SHORT_INDICATOR = 126;
  private static final int LONG_INDICATOR = 127;

  private boolean isClosed;
  private boolean finish;
  private boolean mask;
  private byte opCode;
  private long length;
  private byte[] maskKey;
  private boolean completed;

  public WebSocketHeader() {
    reset();
  }

  public void packHeader(Packet header) {
    int flags = 0;
    if (finish) {
      flags = 0b10000000;
    }
    flags = flags | (opCode & 0b1111);
    header.put((byte) flags);
    flags = 0;
    if (length < 126) {
      flags = flags | (byte) (length & 0x7F);
      header.put((byte) flags);
    } else if (length < 0xFFFF) {
      header.put((byte) SHORT_INDICATOR);
      write(header, length, 2);
    } else {
      header.put((byte) LONG_INDICATOR);
      write(header, length, 8);
    }
  }

  //
  // We need to read in the web socket header as a stream since we don't
  // know exactly where the header finishes
  //
  public boolean parse(Packet header) throws IOException {
    // Read the OpCode and initial length
    if (header.available() >= 2) {
      int flags = header.get();
      finish = ((flags & 0b10000000) != 0);
      opCode = (byte) (flags & 0b1111);
      int maskSize = header.get();
      mask = (maskSize & 0b10000000) != 0;
      int size = maskSize & 0x7F;

      switch (opCode) {
        case CONTINUATION:
        case TEXT:
        case BINARY:
          loadCompleteHeader(header, size);
          break;

        case PING:
        case PONG:
          break;

        case CLOSE:
          isClosed = true;
          break;

        default:
          throw new IOException("Unknown WebSocket OpCode: " + opCode);
      }
    } else {
      throw new EndOfBufferException();
    }
    completed = true;
    return true;
  }

  public void reset() {
    isClosed = false;
    finish = false;
    mask = false;
    opCode = -1;
    length = 0;
    maskKey = null;
    completed = false;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public boolean isClose() {
    return isClosed;
  }

  public void setClose(boolean close) {
    this.isClosed = close;
  }

  public boolean isMask() {
    return mask;
  }

  public byte getOpCode() {
    return opCode;
  }

  public void setOpCode(byte opCode) {
    this.opCode = opCode;
  }

  public long getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public byte[] getMaskKey() {
    return maskKey;
  }

  public void setFinish(boolean finish) {
    this.finish = finish;
  }

  private boolean loadCompleteHeader(Packet header, int size) throws EndOfBufferException {
    if (size < SHORT_INDICATOR) {
      length = size;
    }
    if (size == SHORT_INDICATOR) {
      if (header.available() < 2) {
        throw new EndOfBufferException("Excepted more data for the length");
      }
      length = read(header, 2);
    } else if (size == LONG_INDICATOR) {
      if (header.available() < 8) {
        throw new EndOfBufferException("Excepted more data for the length");
      }
      length = read(header, 8);
    }

    if (mask) {
      if (header.available() < 4) {
        throw new EndOfBufferException("Excepted more data for the maskKey");
      }
      maskKey = new byte[4];
      header.get(maskKey);
    }
    return true;
  }

  private long read(Packet packet, int size) {
    long tmp = 0;
    for (int x = 0; x < size; x++) {
      tmp = (tmp << 8) + (packet.get() & 0xff);
    }
    return tmp;
  }

  void write(Packet packet, long value, int size) {
    for (int x = 0; x < size; x++) {
      packet.put((byte) ((value >> (8 * (size - (x + 1)))) & 0xff));
    }
  }
}
