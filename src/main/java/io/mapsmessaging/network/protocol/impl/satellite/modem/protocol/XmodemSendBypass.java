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

package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem.XmodemSender;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Sends a pre-supplied buffer via XMODEM (CRC mode).
 * Usage: create with (data[, off, len]), then let the modem pump parseInput().
 * No staging via parseOutput(); outbound data is already provided.
 */
public class XmodemSendBypass implements StreamBypass {

  private final byte[] data;
  private final int offset;
  private final int length;
  private final int readTimeoutMs;
  private final CompletableFuture<byte[]> future;
  private final long precomputedCrc32; // -1 to compute via mark/reset ByteArrayInputStream

  private volatile boolean started = false;
  private volatile boolean complete = false;

  public XmodemSendBypass(byte[] data,
                          int readTimeoutMs,
                          CompletableFuture<byte[]> future,
                          long precomputedCrc32) {
    offset = 0;
    if (data == null) throw new IllegalArgumentException("data is null");

    length = data.length;
    this.data = data;
    this.readTimeoutMs = readTimeoutMs;
    this.future = future;
    this.precomputedCrc32 = precomputedCrc32;
  }

  @Override
  public int parseInput(InputStream linkIn, OutputStream linkOut) throws IOException {
    if (complete || started) {
      return 0;
    }
    started = true;

    try {
      ByteArrayInputStream src = new ByteArrayInputStream(data, offset, length);
      XmodemSender sender = new XmodemSender();

      if (precomputedCrc32 >= 0) {
        sender.send(src, linkOut, linkIn, length, readTimeoutMs, precomputedCrc32);
      } else {
        sender.send(src, linkOut, linkIn, length, readTimeoutMs);
      }

      complete = true;

      if (future != null) {
        future.complete(slice());
      }
    } catch (IOException e) {
      complete = true;
      if (future != null) {
        future.completeExceptionally(e);
      }
      throw e;
    }
    return 0;
  }

  @Override
  public int parseOutput(OutputStream out, Packet packet) {
    // Not used for sending; data is provided up front.
    return 0;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public byte[] result() {
    return slice();
  }

  @Override
  public void reset() {
    started = false;
    complete = false;
  }

  private byte[] slice() {
    if (offset == 0 && length == data.length) {
      return data;
    }
    byte[] copy = new byte[length];
    System.arraycopy(data, offset, copy, 0, length);
    return copy;
  }
}