/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.maps.network.io.Packet;
import org.maps.network.io.StreamHandler;

public class SimpleStreamHandler implements StreamHandler {

  private final byte[] outBuffer;
  private final byte[] inBuffer;


  public SimpleStreamHandler(int bufferSize) {
    outBuffer = new byte[bufferSize];
    inBuffer = new byte[bufferSize];
  }

  @Override
  public void close() {
    // Nothing to close here
  }

  @Override
  public int parseInput(InputStream input, Packet packet) throws IOException {
    int totalRead = 0;
    int available = Math.min(input.available(), inBuffer.length);
    packet.getRawBuffer().clear();
    while (available != 0) {
      int read = input.read(inBuffer, 0, available);
      if (read > 0) {
        packet.put(inBuffer, 0, read);
        totalRead += read;
        available = Math.min(input.available(), inBuffer.length);
      } else if (read < 0) {
        throw new IOException("Stream has been closed");
      }
    }
    return totalRead;
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
    return total;
  }
}
