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

package org.maps.utilities.streams;

import java.io.IOException;

public abstract class ObjectReader {

  public short readShort() throws IOException {
    return (short) read(2);
  }

  public int readInt() throws IOException {
    return (int) read(4);
  }

  public long readLong() throws IOException {
    return read(8);
  }

  public float readFloat() throws IOException {
    return Float.intBitsToFloat((int) read(4));
  }

  public double readDouble() throws IOException {
    return Double.longBitsToDouble(read(8));
  }

  public String readString() throws IOException {
    String result = null;
    int length = readInt();
    if (length > -1) {
      result = new String(readFromStream(length));
    }
    return result;
  }

  public byte[] readByteArray() throws IOException {
    int length = readInt();
    return readFromStream(length);
  }

  // <editor-fold desc="Native array type support">
  public char[] readCharArray() throws IOException {
    char[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new char[length];
      for (int x = 0; x < length; x++) {
        result[x] = readChar();
      }
    }
    return result;
  }

  public short[] readShortArray() throws IOException {
    short[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new short[length];
      for (int x = 0; x < length; x++) {
        result[x] = readShort();
      }
    }
    return result;
  }

  public int[] readIntArray() throws IOException {
    int[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new int[length];
      for (int x = 0; x < length; x++) {
        result[x] = readShort();
      }
    }
    return result;
  }

  public long[] readLongArray() throws IOException {
    long[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new long[length];
      for (int x = 0; x < length; x++) {
        result[x] = readShort();
      }
    }
    return result;
  }

  public float[] readFloatArray() throws IOException {
    float[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new float[length];
      for (int x = 0; x < length; x++) {
        result[x] = readFloat();
      }
    }
    return result;
  }

  public double[] readDoubleArray() throws IOException {
    double[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new double[length];
      for (int x = 0; x < length; x++) {
        result[x] = readDouble();
      }
    }
    return result;
  }

  public String[] readStringArray() throws IOException {
    String[] result = null;
    int length = readInt();
    if (length > -1) {
      result = new String[length];
      for (int x = 0; x < length; x++) {
        result[x] = readString();
      }
    }
    return result;
  }

  // </editor-fold>
  protected long fromByteArray(byte[] tmp) {
    long val = 0;
    for (byte b : tmp) {
      val = (val << 8) | (0xff & b);
    }
    return val;
  }

  protected abstract byte[] readFromStream(int length) throws IOException;

  protected abstract long read(int size) throws IOException;

  public abstract byte readByte() throws IOException;

  public abstract char readChar() throws IOException;

}
