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
// == ModemStreamHandler with pluggable bypass ==
package io.mapsmessaging.network.protocol.impl.satellite.modem.protocol;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;
import io.mapsmessaging.network.protocol.impl.nmea.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class ModemStreamHandler implements StreamHandler {

  private static final int BUFFER_SIZE = 10240;

  private final byte[] inputBuffer;
  private final byte[] outBuffer;

  private final AtomicReference<StreamBypass> bypass;
  private OutputStream serialOut;   // cached link out for bypass

  public ModemStreamHandler() {
    inputBuffer = new byte[BUFFER_SIZE];
    outBuffer = new byte[BUFFER_SIZE];
    bypass = new AtomicReference<>();
    serialOut = null;
  }

  @Override
  public void close() { /* no-op */ }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    // ---- Bypass path ----
    StreamBypass b = bypass.get();
    if (b != null) {
      if (serialOut == null) throw new IOException("Bypass active before output stream is available");
      int consumed = b.parseInput(input, serialOut);  // may block until complete
      if (b.isComplete()) {
        clearBypass();
        packet.put("\r\n".getBytes());
        return 3;
      }
      return consumed; // not complete yet (rare; e.g., partial reads)
    }

    // ---- Default line mode ----
    int idx = 0;
    int ch;

    // skip empty lines
    do {
      ch = input.read();
      if (ch == -1) return 0;
    } while (ch == Constants.CR || ch == Constants.LF);

    inputBuffer[idx++] = (byte) ch;

    while (idx < inputBuffer.length) {
      ch = input.read();
      if (ch == -1 || ch == Constants.CR || ch == Constants.LF) break;
      inputBuffer[idx++] = (byte) ch;
    }

    if (idx == inputBuffer.length) {
      throw new IOException("Exceeded buffer size of known Modem sentences");
    }
    inputBuffer[idx++] = (byte) Constants.CR;
    inputBuffer[idx++] = (byte) Constants.LF;
    packet.put(inputBuffer, 0, idx);
    return idx;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) throws IOException {
    // cache for bypass use
    if (this.serialOut == null) this.serialOut = output;

    StreamBypass b = bypass.get();
    if (b != null) {
      // Let bypass decide how to handle outbound data; most will pass-through
      return b.parseOutput(output, packet);
    }

    int available = packet.available();
    int total = available;
    while (available > outBuffer.length) {
      packet.get(outBuffer, 0, outBuffer.length);
      output.write(outBuffer, 0, outBuffer.length);
      available = packet.available();
    }
    if (available > 0) {
      packet.get(outBuffer, 0, available);
      output.write(outBuffer, 0, available);
    }
    output.flush();
    return total;
  }

  // ---- Bypass control ----
  public void clearBypass() {
    this.bypass.set(null);
  }

  /**
   * Convenience: start XMODEM receive
   */
  public synchronized void startXModem(int length, long crc32, CompletableFuture<byte[]> future) {
    this.bypass.set(new XmodemBypass(length, crc32, 1000, future));
  }
}
