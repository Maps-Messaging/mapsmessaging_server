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
import io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem.XmodemReceiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class XmodemReceieveBypass implements StreamBypass {
  private final int announcedLength;
  private final long expectedCrc32;  // pass -1 to skip CRC32 check
  private final int readTimeoutMs;
  private final CompletableFuture<byte[]> future;
  private final ByteArrayOutputStream payload;

  private volatile boolean complete = false;
  private volatile boolean started = false;

  public XmodemReceieveBypass(int announcedLength, long expectedCrc32, int readTimeoutMs, CompletableFuture<byte[]> future) {
    this.future = future;
    this.payload = new ByteArrayOutputStream();
    this.announcedLength = announcedLength;
    this.expectedCrc32 = expectedCrc32;
    this.readTimeoutMs = readTimeoutMs;
  }

  @Override
  public int parseInput(InputStream in, OutputStream linkOut) throws IOException {
    if (complete) return 0;
    if (!started) {
      started = true;
      try {
        XmodemReceiver rx = new XmodemReceiver();
        rx.receive(in, linkOut, payload, announcedLength, expectedCrc32, readTimeoutMs);
        byte[] result = payload.toByteArray();
        complete = true;
        future.complete(result);
      } catch (IOException e) {
        future.completeExceptionally(e);
        complete = true;
        throw e;
      }
    }
    return 0;
  }

  @Override
  public int parseOutput(OutputStream out, Packet packet) throws IOException {
    int avail = packet.available();
    if (avail <= 0) return 0;
    byte[] buf = new byte[Math.min(avail, 8192)];
    int total = avail;
    while (avail > 0) {
      int n = Math.min(avail, buf.length);
      packet.get(buf, 0, n);
      out.write(buf, 0, n);
      avail -= n;
    }
    out.flush();
    return total;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public byte[] result() {
    return payload.toByteArray();
  }

  @Override
  public void reset() {
    payload.reset();
    complete = false;
    started = false;
  }
}