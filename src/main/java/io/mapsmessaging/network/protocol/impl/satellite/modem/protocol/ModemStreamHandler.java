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
import io.mapsmessaging.network.io.StreamHandler;
import io.mapsmessaging.network.protocol.impl.nmea.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ModemStreamHandler implements StreamHandler {

  private static final int BUFFER_SIZE = 10240;

  private final byte[] inputBuffer;
  private final byte[] outBuffer;

  ModemStreamHandler() {
    inputBuffer = new byte[BUFFER_SIZE];
    outBuffer = new byte[BUFFER_SIZE];
  }

  @Override
  public void close() {
    // There is nothing to do here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    int idx = 0;
    int b;

// skip empty lines (CR/LF only)
    do {
      b = input.read();
      if (b == -1) return 0; // EOF, no data
    } while (b == Constants.CR || b == Constants.LF);

// read until first CR/LF after any data
    inputBuffer[idx++] = (byte) b;
    while (idx < inputBuffer.length) {
      b = input.read();
      if (b == -1) break;                 // EOF mid-line
      if (b == Constants.CR || b == Constants.LF) break; // end of line (we'll add CR/LF later)
      inputBuffer[idx++] = (byte) b;
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
    int available = packet.available();
    int total = available;
    while (available > outBuffer.length) {
      packet.get(outBuffer, 0, available);
      output.write(outBuffer, 0, available);
      available = packet.available();
    }
    if (available > 0) {
      packet.get(outBuffer, 0, available);
      output.write(outBuffer, 0, available);
    }
    output.flush();
    return total;
  }
}
