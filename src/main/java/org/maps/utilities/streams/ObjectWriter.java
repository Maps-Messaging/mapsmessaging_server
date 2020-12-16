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

public abstract class ObjectWriter {

  public void write(short val) throws IOException {
    write(val, 2);
  }

  public void write(int val) throws IOException {
    write(val, 4);
  }

  public void write(long val) throws IOException {
    write(val, 8);
  }

  public void write(float val) throws IOException {
    write(Float.floatToIntBits(val), 4);
  }

  public void write(double val) throws IOException {
    write(Double.doubleToRawLongBits(val), 8);
  }

  public void write(String val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.getBytes());
  }

    // <editor-fold desc="Native array type support">
  public void write(char[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (char entry : val) {
        write(entry);
      }
    }
  }

  public void write(short[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (short entry : val) {
        write(entry);
      }
    }
  }

  public void write(int[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (int entry : val) {
        write(entry);
      }
    }
  }

  public void write(long[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (long entry : val) {
        write(entry);
      }
    }
  }

  public void write(float[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (float entry : val) {
        write(entry);
      }
    }
  }

  public void write(double[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (double entry : val) {
        write(entry);
      }
    }
  }

  public void write(String[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      for (String entry : val) {
        write(entry);
      }
    }
  }

  // </editor-fold>

  protected byte[] toByteArray(long val, int size) {
    byte[] tmp = new byte[size];
    for (int x = 0; x < size; x++) {
      tmp[(size - x) - 1] = (byte) (0xff & val);
      val = val >> 8;
    }
    return tmp;
  }

  public abstract void write(byte val) throws IOException;

  public abstract void write(char val) throws IOException;

  public abstract void write(byte[] val) throws IOException;

  protected abstract void write(long val, int size) throws IOException;

}
