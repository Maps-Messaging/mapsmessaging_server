/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.utilities.streams;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferObjectWriter extends ObjectWriter {

  private final ByteBuffer buffer;

  public BufferObjectWriter(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void write(byte val) throws IOException {
    buffer.put(val);
  }

  @Override
  public void write(char val) throws IOException {
    buffer.putChar(val);
  }

  @Override
  public void write(byte[] val) throws IOException {
    if (val == null) {
      write(-1);
    }
    else {
      write(val.length);
      if (val.length > 0) {
        buffer.put(val);
      }
    }
  }

  protected void write(long val, int size) {
    buffer.put(toByteArray(val, size));
  }
}
