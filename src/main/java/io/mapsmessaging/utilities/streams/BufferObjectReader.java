/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.streams;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferObjectReader extends ObjectReader {

  private final ByteBuffer buffer;

  public BufferObjectReader(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public byte readByte() {
    return buffer.get();
  }

  @Override
  public char readChar() {
    return buffer.getChar();
  }

  @Override
  public String readString() throws IOException {
    String result = null;
    int length = readInt();
    if (length > -1) {
      byte[] tmp = new byte[length];
      buffer.get(tmp);
      result = new String(tmp);
    }
    return result;
  }

  @Override
  public byte[] readByteArray() throws IOException {
    int length = readInt();
    return readFromStream(length);
  }

  protected long read(int size) throws IOException {
    byte[] tmp = new byte[size];
    if (buffer.limit() - buffer.position() < size) {
      throw new IOException("End Of buffer reached");
    }
    buffer.get(tmp);
    return fromByteArray(tmp);
  }

  @Override
  public byte[] readFromStream(int length){
    byte[] result = null;
    if (length > -1) {
      result = new byte[length];
      buffer.get(result);
    }
    return result;
  }

}
