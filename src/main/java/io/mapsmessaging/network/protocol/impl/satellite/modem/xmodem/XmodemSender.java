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

import java.io.*;

public class XmodemSender extends Xmodem {

  private static final int MAX_HANDSHAKE_TRIES = 15;
  private static final int MAX_PER_BLOCK_RETRIES = 10;
  private static final int MAX_EOT_RETRIES = 10;


  /**
   * Send one XMODEM-CRC transfer.
   *
   * If {@code precomputedCrc32 >= 0}, it is returned in the result without recomputing.
   * Otherwise, this method computes CRC-32/MPEG-2 over exactly {@code announcedLength} bytes from {@code src}.
   * For on-the-fly computation, {@code src} must support mark/reset; otherwise precompute and pass the CRC.
   */
  public SenderResult send(InputStream src,
                     OutputStream linkOut,
                     InputStream linkIn,
                     int announcedLength,
                     boolean use1kBlocks,
                     int readTimeoutMs,
                     long precomputedCrc32) throws IOException {

    final long startNs = System.nanoTime();
    final int blockSize = use1kBlocks ? 1024 : 128;

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
    byte[] frameBuf = new byte[3 + blockSize + 2]; // hdr + blk + ~blk + data + crc16

    while (bytesRemaining > 0) {
      int toRead = Math.min(blockSize, bytesRemaining);
      int got = readExactly(src, dataBuf, 0, toRead);
      if (got < toRead) throw new EOFException("Source ended early: got " + got + " < " + toRead);

      // pad with 0x1A to full block
      if (got < blockSize) {
        for (int i = got; i < blockSize; i++) dataBuf[i] = 0x1A;
      }

      int idx = 0;
      int header = use1kBlocks ? STX : SOH;
      frameBuf[idx++] = (byte) header;
      frameBuf[idx++] = (byte) (expectedBlock & 0xFF);
      frameBuf[idx++] = (byte) ((~expectedBlock) & 0xFF);
      System.arraycopy(dataBuf, 0, frameBuf, idx, blockSize);
      idx += blockSize;

      int crc16 = crc16Xmodem(dataBuf, 0, blockSize);
      frameBuf[idx++] = (byte) ((crc16 >>> 8) & 0xFF);
      frameBuf[idx++] = (byte) (crc16 & 0xFF);

      int perBlockRetries = 0;
      while (true) {
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
          // retransmit
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
    while (true) {
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
      // retry EOT
    }

    long durMs = (System.nanoTime() - startNs) / 1_000_000L;
    return new SenderResult(totalBlocks, totalRetries, durMs, bytesOnWire, crc32);
  }

  /** Convenience overload: computes CRC32 using mark/reset. */
  public SenderResult send(InputStream src,
                     OutputStream linkOut,
                     InputStream linkIn,
                     int announcedLength,
                     boolean use1kBlocks,
                     int readTimeoutMs) throws IOException {
    return send(src, linkOut, linkIn, announcedLength, use1kBlocks, readTimeoutMs, -1);
  }

  // ---- helpers ----

  private void waitForC(InputStream in, int timeoutMs) throws IOException {
    for (int tries = 0; tries < MAX_HANDSHAKE_TRIES; tries++) {
      int b = readByteWithTimeout(in, timeoutMs);
      if (b == CHAR_C) return;
      if (b == CAN) throw new IOException("Receiver cancelled (CAN) during handshake");
      // ignore others/timeouts
    }
    throw new IOException("Handshake failed: no 'C' from receiver");
  }

}
