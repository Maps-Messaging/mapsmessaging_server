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

public final class XmodemReceiver {

  // Control bytes
  private static final int SOH = 0x01;   // 128-byte block
  private static final int STX = 0x02;   // 1024-byte block
  private static final int EOT = 0x04;
  private static final int ACK = 0x06;
  private static final int NAK = 0x15;
  private static final int CAN = 0x18;
  private static final int CHAR_C = 0x43; // 'C'

  private static int readByteWithTimeout(InputStream in, int timeoutMs) throws IOException {
    long end = System.nanoTime() + timeoutMs * 1_000_000L;
    while (true) {
      if (in.available() > 0) {
        int b = in.read();
        if (b < 0) throw new EOFException("Link closed");
        return b & 0xFF;
      }
      if (System.nanoTime() >= end) return -1;
      try {
        Thread.sleep(5);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new InterruptedIOException();
      }
    }
  }

  private static int readU8(InputStream in) throws IOException {
    int b = in.read();
    if (b < 0) throw new EOFException("Link closed");
    return b & 0xFF;
  }

  private static byte[] readFully(InputStream in, int len) throws IOException {
    byte[] buf = new byte[len];
    int off = 0;
    while (off < len) {
      int r = in.read(buf, off, len - off);
      if (r < 0) throw new EOFException("Link closed");
      off += r;
    }
    return buf;
  }

  // CRC-16/XMODEM: poly 0x1021, init 0x0000, no refin/refout, no xorout
  private static int crc16Xmodem(byte[] data, int off, int len) {
    int crc = 0x0000;
    for (int i = 0; i < len; i++) {
      crc ^= (data[off + i] & 0xFF) << 8;
      for (int b = 0; b < 8; b++) {
        if ((crc & 0x8000) != 0) crc = (crc << 1) ^ 0x1021;
        else crc <<= 1;
        crc &= 0xFFFF;
      }
    }
    return crc & 0xFFFF;
  }

  // CRC-32/MPEG-2 (poly 0x04C11DB7, init 0xFFFFFFFF, refin=false, refout=false, xorout=0)
  static long crc32Mpeg2(byte[] data, int len) {
    int crc = 0xFFFFFFFF;
    for (int i = 0; i < len; i++) {
      crc ^= (data[i] & 0xFF) << 24;
      for (int b = 0; b < 8; b++) {
        crc = ((crc & 0x80000000) != 0) ? (crc << 1) ^ 0x04C11DB7 : (crc << 1);
      }
      crc &= 0xFFFFFFFF;
    }
    return crc; // already no xorout
  }

  /**
   * Receive one XMODEM transfer in CRC mode.
   * - Sends 'C' to request CRC.
   * - Writes exactly announcedLength bytes to dst (truncates any 0x1A padding).
   * - If expectedCrc32 >= 0, verifies CRC32 over the announcedLength bytes.
   */
  public Result receive(InputStream linkIn,
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

  private static final class RxState {
    int expectedBlock = 1;
    int totalBlocks = 0;
    int totalRetries = 0;
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
        // Early EOT: no data; finalize happens in caller using zero counts
        return s;
      } else if (b == CAN) {
        throw new IOException("Sender cancelled (CAN)");
      }
      // else: ignore stray/timeout and continue
    }

    if (!started) throw new IOException("XMODEM handshake failed (no SOH/STX/EOT)");
    return s;
  }

  private Result receiveLoop(InputStream linkIn,
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
            if (wrote >= 0) {            // delivered (not a duplicate)
              s.expectedBlock = nextExpected(s.expectedBlock);
              s.totalBlocks++;
            }
            break;                       // delivered or duplicate handled
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
      // else ignore stray bytes and continue
    }
  }

  private int nextExpected(int current) {
    int n = (current + 1) & 0xFF;
    return (n == 0) ? 1 : n;
  }
  /**
   * Reads one block; returns >=0 when delivered (bytes written or 0 if duplicate), throws RetryBlock to request NAK/resend.
   */
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
      // deliver (but cap to announced length to avoid padding spill)
      int remaining = announcedLength - buf.size();
      int toWrite = Math.max(0, Math.min(remaining, data.length));
      if (toWrite > 0) buf.write(data, 0, toWrite);
      out.write(ACK);
      out.flush();
      return toWrite;
    } else if (blk == last) {
      // duplicate of last delivered; ACK again, do not re-deliver
      out.write(ACK);
      out.flush();
      return 0;
    } else {
      // unexpected block number
      throw new RetryBlock("Unexpected block " + blk + " (expected " + expected + ")");
    }
  }

  private Result finalizeAndWrite(OutputStream dst,
                                  ByteArrayOutputStream buf,
                                  int announcedLength,
                                  long expectedCrc32,
                                  long startNano,
                                  int totalBlocks,
                                  int totalRetries) throws IOException {

    byte[] all = buf.toByteArray();
    if (all.length < announcedLength) throw new IOException("Transfer ended early: got " + all.length + " < " + announcedLength);
    // Trim to announced length (drop any 0x1A padding)
    dst.write(all, 0, announcedLength);
    dst.flush();
    if (expectedCrc32 >= 0) {
      long got = crc32Mpeg2(all, announcedLength) & 0xFFFFFFFFL;
      long exp = expectedCrc32 & 0xFFFFFFFFL;
      if (got != exp) throw new IOException(String.format("CRC32 mismatch: got 0x%08X expected 0x%08X", got, exp));
    }
    long durMs = (System.nanoTime() - startNano) / 1_000_000L;
    return new Result(totalBlocks, totalRetries, durMs);
  }

  public static class Result {
    public final int blocks;
    public final int retries;
    public final long durationMs;

    public Result(int blocks, int retries, long durationMs) {
      this.blocks = blocks;
      this.retries = retries;
      this.durationMs = durationMs;
    }
  }

  private static final class RetryBlock extends IOException {
    RetryBlock(String m) {
      super(m);
    }
  }

}
