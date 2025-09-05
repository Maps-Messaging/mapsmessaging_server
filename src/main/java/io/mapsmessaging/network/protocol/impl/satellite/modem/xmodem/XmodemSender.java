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

package io.mapsmessaging.network.protocol.impl.satellite.modem.xmodem;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XmodemSender extends Xmodem {

  private static final int MAX_HANDSHAKE_TRIES = 15;
  private static final int MAX_PER_BLOCK_RETRIES = 10;
  private static final int MAX_EOT_RETRIES = 10;

  /** Send one XMODEM-CRC transfer using 128-byte blocks only. */
  public SenderResult send(InputStream src,
                           OutputStream linkOut,
                           InputStream linkIn,
                           int announcedLength,
                           int readTimeoutMs,
                           long precomputedCrc32) throws IOException {

    final long startNs = System.nanoTime();
    final int blockSize = 128;

    long crc32;
    if (precomputedCrc32 >= 0) {
      crc32 = precomputedCrc32 & 0xFFFFFFFFL;
    } else {
      if (!src.markSupported())
        throw new IOException("Source InputStream must support mark/reset or supply precomputed CRC32");
      src.mark(announcedLength);
      crc32 = crc32Mpeg2(src, announcedLength);
      src.reset();
    }

    waitForC(linkIn, readTimeoutMs);

    int expectedBlock = 1;
    int bytesRemaining = announcedLength;
    int totalBlocks = 0;
    int totalRetries = 0;
    int bytesOnWire = 0;

    byte[] dataBuf = new byte[blockSize];
    byte[] frameBuf = new byte[3 + blockSize + 2]; // SOH + blk + ~blk + data + crc16

    while (bytesRemaining > 0) {
      int toRead = Math.min(blockSize, bytesRemaining);
      int got = readExactly(src, dataBuf, 0, toRead);
      if (got < toRead) throw new EOFException("Source ended early: got " + got + " < " + toRead);

      if (got < blockSize) {
        for (int i = got; i < blockSize; i++) dataBuf[i] = 0x1A; // pad
      }

      int idx = 0;
      frameBuf[idx++] = (byte) SOH; // 128-byte block header
      frameBuf[idx++] = (byte) (expectedBlock & 0xFF);
      frameBuf[idx++] = (byte) ((~expectedBlock) & 0xFF);
      System.arraycopy(dataBuf, 0, frameBuf, idx, blockSize);
      idx += blockSize;

      int crc16 = crc16Xmodem(dataBuf, 0, blockSize);
      frameBuf[idx++] = (byte) ((crc16 >>> 8) & 0xFF);
      frameBuf[idx++] = (byte) (crc16 & 0xFF);

      int perBlockRetries = 0;
      for (;;) {
        linkOut.write(frameBuf, 0, idx);
        linkOut.flush();
        bytesOnWire += idx;

        int resp = readByteWithTimeout(linkIn, readTimeoutMs);
        if (resp == ACK) {
          totalBlocks++;
          expectedBlock = nextExpected(expectedBlock);
          bytesRemaining -= toRead;
          break;
        } else if (resp == NAK || resp == -1) {
          if (++perBlockRetries > MAX_PER_BLOCK_RETRIES) {
            cancel(linkOut);
            throw new IOException("Too many NAKs/timeouts on block " + expectedBlock);
          }
          totalRetries++;
        } else if (resp == CAN) {
          throw new IOException("Receiver cancelled (CAN)");
        } else {
          if (++perBlockRetries > MAX_PER_BLOCK_RETRIES) {
            cancel(linkOut);
            throw new IOException("Unexpected response " + resp + " on block " + expectedBlock);
          }
          totalRetries++;
        }
      }
    }

    int eotRetries = 0;
    for (;;) {
      linkOut.write(EOT);
      linkOut.flush();
      bytesOnWire += 1;

      int r = readByteWithTimeout(linkIn, readTimeoutMs);
      if (r == ACK) break;
      if (r == CAN) throw new IOException("Receiver cancelled during EOT");
      if (++eotRetries > MAX_EOT_RETRIES) {
        cancel(linkOut);
        throw new IOException("No ACK to EOT");
      }
    }

    long durMs = (System.nanoTime() - startNs) / 1_000_000L;
    return new SenderResult(totalBlocks, totalRetries, durMs, bytesOnWire, crc32);
  }

  /** Convenience overload: computes CRC32 using mark/reset. */
  public SenderResult send(InputStream src,
                           OutputStream linkOut,
                           InputStream linkIn,
                           int announcedLength,
                           int readTimeoutMs) throws IOException {
    return send(src, linkOut, linkIn, announcedLength, readTimeoutMs, -1);
  }

  private void waitForC(InputStream in, int timeoutMs) throws IOException {
    for (int tries = 0; tries < MAX_HANDSHAKE_TRIES; tries++) {
      int b = readByteWithTimeout(in, timeoutMs);
      if (b == CHAR_C) return;
      if (b == CAN) throw new IOException("Receiver cancelled (CAN) during handshake");
    }
    throw new IOException("Handshake failed: no 'C' from receiver");
  }
}
