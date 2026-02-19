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

package io.mapsmessaging.network.protocol.impl.tak.framing;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class TakFrameReader {

  private final TakStreamFramer framer;
  private final byte[] readBuffer;

  public TakFrameReader(TakStreamFramer framer, int bufferSize) {
    this.framer = framer;
    this.readBuffer = new byte[Math.max(bufferSize, 256)];
  }

  public List<byte[]> read(InputStream inputStream) throws IOException {
    int read = inputStream.read(readBuffer);
    if (read < 0) {
      throw new EOFException("TAK connection closed");
    }
    return framer.onBytes(readBuffer, read);
  }

  public List<byte[]> read(ByteBuffer byteBuffer) throws IOException {
    if (byteBuffer == null || !byteBuffer.hasRemaining()) {
      return List.of();
    }
    int len = byteBuffer.remaining();
    byte[] data = new byte[len];
    byteBuffer.get(data);
    return framer.onBytes(data, len);
  }
}
