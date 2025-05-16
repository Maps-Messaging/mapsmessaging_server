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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NMEAStreamHandler implements StreamHandler {

  // Larger than a defined NMEA buffer
  private static final int BUFFER_SIZE = 256;

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
    //
    // Skip any characters before the START character
    //
    int val = input.read();
    while (val != Constants.START) {
      val = input.read();
    }
    //
    // Read to CheckSum char
    //
    int idx = 0;
    inputBuffer[idx++] = (byte) val;
    val = input.read(); // Start on the next byte not the START frame
    while (val != Constants.CR && val != Constants.LF && idx < inputBuffer.length) {
      inputBuffer[idx++] = (byte) val;
      val = input.read();
    }
    if (idx >= inputBuffer.length) {
      throw new IOException("Exceeded buffer size of known NMEA sentences");
    }
    inputBuffer[idx++] = (byte) Constants.CR;
    inputBuffer[idx++] = (byte) Constants.LF;
    packet.put(inputBuffer, 0, idx);
    return idx;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet) {
    return 0;
  }
}
