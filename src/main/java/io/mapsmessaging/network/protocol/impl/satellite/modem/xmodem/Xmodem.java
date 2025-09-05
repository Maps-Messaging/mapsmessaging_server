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

public class Xmodem {

  // Control bytes
  protected static final int SOH = 0x01;   // 128-byte block
  protected static final int STX = 0x02;   // 1024-byte block
  protected static final int EOT = 0x04;
  protected static final int ACK = 0x06;
  protected static final int NAK = 0x15;
  protected static final int CAN = 0x18;
  protected static final int CHAR_C = 0x43; // 'C'

  protected Xmodem() {}

  // ==== CRC helpers ====

  // CRC-16/XMODEM: poly 0x1021, init 0x0000, no refin/refout, no xorout
  protected int crc16Xmodem(byte[] data, int off, int len) {
    int crc = 0x0000;
    for (int i = 0; i < len; i++) {
      crc ^= (data[off + i] & 0xFF) << 8;
      for (int b = 0; b < 8; b++) {
        crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) : (crc << 1);
        crc &= 0xFFFF;
      }
    }
    return crc & 0xFFFF;
  }

  // CRC-32/MPEG-2 (poly 0x04C11DB7, init 0xFFFFFFFF, refin=false, refout=false, xorout=0)
  public static long crc32Mpeg2(byte[] data, int len) {
    int crc = 0xFFFFFFFF;
    for (int i = 0; i < len; i++) {
      crc ^= (data[i] & 0xFF) << 24;
      for (int b = 0; b < 8; b++) {
        crc = ((crc & 0x80000000) != 0) ? ((crc << 1) ^ 0x04C11DB7) : (crc << 1);
      }
      crc &= 0xFFFFFFFF;
    }
    return crc;
  }

  /** Streamed CRC-32/MPEG-2 over exactly {@code length} bytes from {@code in}. */
  protected long crc32Mpeg2(InputStream in, int length) throws IOException {
    int crc = 0xFFFFFFFF;
    byte[] buf = new byte[8192];
    int remaining = length;
    while (remaining > 0) {
      int r = in.read(buf, 0, Math.min(buf.length, remaining));
      if (r < 0) throw new EOFException("Source ended early while computing CRC");
      for (int i = 0; i < r; i++) {
        crc ^= (buf[i] & 0xFF) << 24;
        for (int b = 0; b < 8; b++) {
          crc = ((crc & 0x80000000) != 0) ? ((crc << 1) ^ 0x04C11DB7) : (crc << 1);
        }
        crc &= 0xFFFFFFFF;
      }
      remaining -= r;
    }
    return crc & 0xFFFFFFFFL;
  }

  // ==== IO helpers ====

  protected int readU8(InputStream in) throws IOException {
    int b = in.read();
    if (b < 0) throw new EOFException("Link closed");
    return b & 0xFF;
  }

  protected byte[] readFully(InputStream in, int len) throws IOException {
    byte[] buf = new byte[len];
    int off = 0;
    while (off < len) {
      int r = in.read(buf, off, len - off);
      if (r < 0) throw new EOFException("Link closed");
      off += r;
    }
    return buf;
  }

  /** Polls for a byte up to timeout; returns -1 on timeout. */
  protected int readByteWithTimeout(InputStream in, int timeoutMs) throws IOException {
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

  // ==== Seq helpers ====

  protected int nextExpected(int current) {
    int n = (current + 1) & 0xFF;
    return (n == 0) ? 1 : n;
  }


  protected void cancel(OutputStream out) {
    try {
      out.write(CAN);
      out.write(CAN);
      out.flush();
    } catch (IOException ignored) { }
  }

  protected int readExactly(InputStream in, byte[] buf, int off, int len) throws IOException {
    int got = 0;
    while (got < len) {
      int r = in.read(buf, off + got, len - got);
      if (r < 0) break;
      got += r;
    }
    return got;
  }
}
