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

package io.mapsmessaging.network.protocol.impl.mavlink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.io.Packet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MavlinkStreamHandlerConcurrencyTest {

  @Test
  void inputAndOutputUseIndependentScratchBuffers() throws Exception {
    byte[] frame = {
      (byte) 0xFE,
      4,
      1,
      2,
      3,
      4,
      10,
      11,
      12,
      13,
      20,
      21
    };

    CountDownLatch headerCopied = new CountDownLatch(1);
    CountDownLatch outputCompleted = new CountDownLatch(1);
    InputStream input =
        new CoordinatedInputStream(frame, headerCopied, outputCompleted);
    MavlinkStreamHandler handler = new MavlinkStreamHandler(2_000);
    Packet inbound = new Packet(64, false);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<Integer> read = executor.submit(() -> handler.parseInput(input, inbound));
      assertTrue(headerCopied.await(1, TimeUnit.SECONDS));

      Packet outbound =
          new Packet(ByteBuffer.wrap(new byte[] {85, 85, 85, 85, 85, 85, 85, 85}));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        assertEquals(8, handler.parseOutput(output, outbound));
      } finally {
        outputCompleted.countDown();
      }

      assertEquals(frame.length, read.get(1, TimeUnit.SECONDS));
      inbound.flip();
      byte[] actual = new byte[inbound.available()];
      inbound.get(actual);

      assertArrayEquals(frame, actual);
      assertArrayEquals(new byte[] {85, 85, 85, 85, 85, 85, 85, 85}, output.toByteArray());
    } finally {
      outputCompleted.countDown();
      executor.shutdownNow();
    }
  }

  private static final class CoordinatedInputStream extends InputStream {

    private final byte[] data;
    private final CountDownLatch headerCopied;
    private final CountDownLatch outputCompleted;
    private int position;
    private boolean firstBulkRead = true;

    private CoordinatedInputStream(
        byte[] data,
        CountDownLatch headerCopied,
        CountDownLatch outputCompleted) {
      this.data = data;
      this.headerCopied = headerCopied;
      this.outputCompleted = outputCompleted;
    }

    @Override
    public int read() {
      if (position >= data.length) {
        return -1;
      }
      return data[position++] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      if (position >= data.length) {
        return -1;
      }

      int count = Math.min(length, data.length - position);
      System.arraycopy(data, position, buffer, offset, count);
      position += count;

      if (firstBulkRead) {
        firstBulkRead = false;
        headerCopied.countDown();
        try {
          if (!outputCompleted.await(1, TimeUnit.SECONDS)) {
            throw new IOException("Timed out waiting for concurrent output");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted waiting for concurrent output", e);
        }
      }
      return count;
    }
  }
}
