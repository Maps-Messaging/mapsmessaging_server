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

public class XmodemReceiver extends Xmodem {

  /**
   * Receive one XMODEM transfer in CRC mode.
   * - Sends 'C' to request CRC.
   * - Writes exactly announcedLength bytes to dst (truncates any 0x1A padding).
   * - If expectedCrc32 >= 0, verifies CRC32 over the announcedLength bytes.
   */
  public ReceiverResult receive(InputStream linkIn,
                        OutputStream linkOut,
                        OutputStream dst,
                        int announcedLength,
                        long expectedCrc32,
                        int readTimeoutMs) throws IOException {

    final long start = System.nanoTime();
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(announcedLength, 128));

    RxState state = handshake(linkIn, linkOut, buffer, announcedLength, readTimeoutMs);
    return receiveLoop(linkIn, linkOut, dst, buffer, announcedLength, expectedCrc32, readTimeoutMs, start, state);
  }

  private RxState handshake(InputStream linkIn,
                            OutputStream linkOut,
                            ByteArrayOutputStream buffer,
                            int announcedLength,
                            int readTimeoutMs) throws IOException {
    final int maxHandshakeTries = 15;
    RxState s = new RxState();

    boolean started = false;
    for (int tries = 0; !started && tries < maxHandshakeTries; tries++) {
      linkOut.write(CHAR_C);
      linkOut.flush();

      int b = readByteWithTimeout(linkIn, readTimeoutMs);
      if (b == SOH || b == STX) {
        started = true;
        processBlock(linkIn, linkOut, buffer, announcedLength, b, s.expectedBlock);
        s.expectedBlock = nextExpected(s.expectedBlock);
        s.totalBlocks++;
      } else if (b == EOT) {
        linkOut.write(ACK);
        linkOut.flush();
        return s; // early EOT
      } else if (b == CAN) {
        throw new IOException("Sender cancelled (CAN)");
      }
    }

    if (!started) throw new IOException("XMODEM handshake failed (no SOH/STX/EOT)");
    return s;
  }

  private ReceiverResult receiveLoop(InputStream linkIn,
                             OutputStream linkOut,
                             OutputStream dst,
                             ByteArrayOutputStream buffer,
                             int announcedLength,
                             long expectedCrc32,
                             int readTimeoutMs,
                             long startNs,
                             RxState s) throws IOException {

    final int maxPerBlockRetries = 10;

    while (true) {
      int header = readByteWithTimeout(linkIn, readTimeoutMs);

      if (header == EOT) {
        linkOut.write(ACK);
        linkOut.flush();
        return finalizeAndWrite(dst, buffer, announcedLength, expectedCrc32, startNs, s.totalBlocks, s.totalRetries);
      }

      if (header == SOH || header == STX) {
        int perBlockRetries = 0;
        while (true) {
          try {
            int wrote = processBlock(linkIn, linkOut, buffer, announcedLength, header, s.expectedBlock);
            if (wrote >= 0) {
              s.expectedBlock = nextExpected(s.expectedBlock);
              s.totalBlocks++;
            }
            break;
          } catch (RetryBlock e) {
            if (++perBlockRetries > maxPerBlockRetries)
              throw new IOException("Too many NAKs/timeouts on block " + s.expectedBlock);

            s.totalRetries++;
            linkOut.write(NAK);
            linkOut.flush();

            header = readByteWithTimeout(linkIn, readTimeoutMs);
            if (header != SOH && header != STX)
              throw new IOException("Expected SOH/STX on retry, got: " + header);
          }
        }
        continue;
      }

      if (header == CAN) throw new IOException("Sender cancelled (CAN)");
      if (header < 0) throw new EOFException("Link closed");
    }
  }

  /** Reads one block; returns >=0 when delivered (bytes written or 0 if duplicate), throws RetryBlock to request NAK/resend. */
  private int processBlock(InputStream in,
                           OutputStream out,
                           ByteArrayOutputStream buf,
                           int announcedLength,
                           int header,
                           int expectedBlock) throws IOException {

    final int blockSize = (header == STX) ? 1024 : 128;

    int blk = readU8(in);
    int cblk = readU8(in);
    if (((blk + cblk) & 0xFF) != 0xFF) throw new RetryBlock("Block number complement invalid");

    byte[] data = readFully(in, blockSize);
    int crcHi = readU8(in);
    int crcLo = readU8(in);
    int recvCrc = ((crcHi & 0xFF) << 8) | (crcLo & 0xFF);
    int calcCrc = crc16Xmodem(data, 0, data.length);
    if (recvCrc != calcCrc) throw new RetryBlock("CRC mismatch");

    int expected = expectedBlock & 0xFF;
    int last = ((expected - 2) & 0xFF) + 1; // previous delivered block in 1..255 space
    if (blk == expected) {
      int remaining = announcedLength - buf.size();
      int toWrite = Math.max(0, Math.min(remaining, data.length));
      if (toWrite > 0) buf.write(data, 0, toWrite);
      out.write(ACK);
      out.flush();
      return toWrite;
    } else if (blk == last) {
      out.write(ACK);
      out.flush();
      return 0;
    } else {
      throw new RetryBlock("Unexpected block " + blk + " (expected " + expected + ")");
    }
  }

  private ReceiverResult finalizeAndWrite(OutputStream dst,
                                  ByteArrayOutputStream buf,
                                  int announcedLength,
                                  long expectedCrc32,
                                  long startNano,
                                  int totalBlocks,
                                  int totalRetries) throws IOException {

    byte[] all = buf.toByteArray();
    if (all.length < announcedLength) throw new IOException("Transfer ended early: got " + all.length + " < " + announcedLength);
    dst.write(all, 0, announcedLength);
    dst.flush();
    if (expectedCrc32 >= 0) {
      long got = crc32Mpeg2(all, announcedLength) & 0xFFFFFFFFL;
      long exp = expectedCrc32 & 0xFFFFFFFFL;
      if (got != exp) throw new IOException(String.format("CRC32 mismatch: got 0x%08X expected 0x%08X", got, exp));
    }
    long durMs = (System.nanoTime() - startNano) / 1_000_000L;
    return new ReceiverResult(totalBlocks, totalRetries, durMs);
  }


  private static final class RetryBlock extends IOException {
    RetryBlock(String m) { super(m); }
  }
}
