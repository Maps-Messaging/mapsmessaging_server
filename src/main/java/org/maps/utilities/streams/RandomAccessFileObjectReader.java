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
import java.io.RandomAccessFile;

public class RandomAccessFileObjectReader extends ObjectReader {

  private final RandomAccessFile randomAccessFile;

  public RandomAccessFileObjectReader(RandomAccessFile randomAccessFile) {
    this.randomAccessFile = randomAccessFile;
  }

  @Override
  public byte readByte() throws IOException {
    return (byte) (0xff & randomAccessFile.read());
  }

  @Override
  public short readShort() throws IOException {
    return randomAccessFile.readShort();
  }

  @Override
  public int readInt() throws IOException {
    return randomAccessFile.readInt();
  }

  @Override
  public long readLong() throws IOException {
    return randomAccessFile.readLong();
  }

  @Override
  public char readChar() throws IOException {
    return randomAccessFile.readChar();
  }

  @Override
  public float readFloat() throws IOException {
    return randomAccessFile.readFloat();
  }

  @Override
  public double readDouble() throws IOException {
    return randomAccessFile.readDouble();
  }

  @Override
  public String readString() throws IOException {
    String result = null;
    int length = readInt();
    if (length > -1) {
      byte[] buffer = new byte[length];
      randomAccessFile.read(buffer);
      result = new String(buffer);
    }
    return result;
  }

  @Override
  protected byte[] readFromStream(int length) throws IOException {
    byte[] tmp = new byte[length];
    randomAccessFile.write(tmp);
    return tmp;
  }

  @Override
  protected long read(int size) throws IOException {
    byte[] tmp = new byte[size];
    randomAccessFile.read(tmp);
    return fromByteArray(tmp);
  }

  @Override
  public byte[] readByteArray() throws IOException {
    byte[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new byte[length];
      randomAccessFile.read(result);
    }
    return result;
  }
}
