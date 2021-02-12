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

import java.io.DataOutput;
import java.io.IOException;

public class DataObjectWriter extends ObjectWriter {

  private final DataOutput dataOutput;

  public DataObjectWriter(DataOutput dataOutput) {
    this.dataOutput = dataOutput;
  }

  // <editor-fold desc="Native Integer based value support">
  @Override
  public void write(byte val) throws IOException {
    dataOutput.write(val);
  }

  @Override
  public void write(char val) throws IOException {
    dataOutput.writeChar(val);
  }
  // </editor-fold>

  @Override
  public void write(byte[] val) throws IOException {
    if (val == null) {
      write(-1);
      return;
    }
    write(val.length);
    if (val.length > 0) {
      dataOutput.write(val);
    }
  }

  protected void write(long val, int size) throws IOException {
    dataOutput.write(toByteArray(val, size));
  }
}
