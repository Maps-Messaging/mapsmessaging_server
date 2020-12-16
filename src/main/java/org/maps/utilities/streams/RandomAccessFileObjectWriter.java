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

public class RandomAccessFileObjectWriter extends ObjectWriter {

  private final RandomAccessFile randomAccessFile;

  public RandomAccessFileObjectWriter(RandomAccessFile randomAccessFile) {
    this.randomAccessFile = randomAccessFile;
  }

  @Override
  public void write(byte val) throws IOException {
    randomAccessFile.write(val);
  }

  @Override
  public void write(short val) throws IOException {
    randomAccessFile.writeShort(val);
  }

  @Override
  public void write(int val) throws IOException {
    randomAccessFile.writeInt(val);
  }

  @Override
  public void write(long val) throws IOException {
    randomAccessFile.writeLong(val);
  }

  @Override
  public void write(char val) throws IOException {
    randomAccessFile.writeChar(val);
  }

  @Override
  public void write(float val) throws IOException {
    randomAccessFile.writeFloat(val);
  }

  @Override
  public void write(double val) throws IOException {
    randomAccessFile.writeDouble(val);
  }

  @Override
  public void write(String val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.getBytes());
  }

  @Override
  public void write(byte[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      randomAccessFile.write(val);
    }
  }

  @Override
  protected void write(long val, int size) throws IOException {
    randomAccessFile.write(toByteArray(val, size));
  }
}
