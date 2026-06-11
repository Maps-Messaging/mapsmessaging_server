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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NMEAStreamHandler implements StreamHandler {

  // Larger than a defined NMEA buffer, and large enough for Sonardyne proprietary telegrams
  private static final int BUFFER_SIZE = 1024;

  private final byte[] inputBuffer;

  NMEAStreamHandler() {
    inputBuffer = new byte[BUFFER_SIZE];
  }

  @Override
  public void close() {
    // There is nothing to do here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    int value = readUntilStart(input);

    int bufferIndex = 0;
    inputBuffer[bufferIndex++] = (byte) value;

    value = input.read();

    while (value != -1 && value != Constants.CR && value != Constants.LF) {
      if (bufferIndex >= inputBuffer.length - 2) {
        throw new IOException("Exceeded buffer size of known NMEA sentences");
      }

      inputBuffer[bufferIndex++] = (byte) value;
      value = input.read();
    }

    if (value == -1) {
      throw new IOException("End of stream while reading NMEA sentence");
    }

    if (value == Constants.CR) {
      consumeOptionalLineFeed(input);
    }

    inputBuffer[bufferIndex++] = (byte) Constants.CR;
    inputBuffer[bufferIndex++] = (byte) Constants.LF;

    packet.put(inputBuffer, 0, bufferIndex);
    return bufferIndex;
  }

  private int readUntilStart(InputStream input) throws IOException {
    int value = input.read();

    while (value != -1 && value != Constants.START) {
      value = input.read();
    }

    if (value == -1) {
      throw new IOException("End of stream before NMEA sentence start");
    }

    return value;
  }

  private void consumeOptionalLineFeed(InputStream input) throws IOException {
    if (!input.markSupported()) {
      return;
    }

    input.mark(1);

    int value = input.read();

    if (value != Constants.LF && value != -1) {
      input.reset();
    }
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) {
    return 0;
  }
}