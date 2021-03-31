/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
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
    int checksum = 0;
    val = input.read(); // Start on the next byte not the START frame
    while (val != Constants.CHECKSUM &&
        val != Constants.CR &&
        val != Constants.LF &&
        idx < inputBuffer.length) {
      inputBuffer[idx] = (byte) val;
      idx++;
      checksum ^= val;
      val = input.read();
    }
    if (idx >= inputBuffer.length) {
      throw new IOException("Exceeded buffer size of known NMEA sentences");
    }
    //
    // Now read the checksum
    //
    if (val == Constants.CHECKSUM) {
      val = input.read();
      int count = 0;
      StringBuilder sb = new StringBuilder();
      while (val != Constants.CR &&
          val != Constants.LF &&
          count < 4) {
        sb.append((char) val);
        val = input.read();
        count++;
      }
      // Skip the eok char
      input.read(); // Need to skip this one char
      int sentCheckSum = Integer.parseInt(sb.toString(), 16);
      if (sentCheckSum == checksum) {
        packet.put(inputBuffer, 0, idx);
      } else {
        throw new IOException("Invalid Checksum calculated");
      }
    }
    return idx;
  }

  @Override
  public int parseOutput(OutputStream output, Packet packet)  {
    return 0;
  }
}
