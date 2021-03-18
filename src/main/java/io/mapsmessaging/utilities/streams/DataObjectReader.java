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

import java.io.DataInput;
import java.io.IOException;

public class DataObjectReader extends ObjectReader {

  private final DataInput dataInput;

  public DataObjectReader(DataInput dataInput) {
    this.dataInput = dataInput;
  }

  @Override
  public byte readByte() throws IOException {
    return (byte) (0xff & dataInput.readByte());
  }

  @Override
  public char readChar() throws IOException {
    return dataInput.readChar();
  }

  protected long read(int size) throws IOException {
    return fromByteArray(readFromStream(size));
  }

  protected byte[] readFromStream(int length) throws IOException {
    byte[] result = null;
    if (length > -1) {
      result = new byte[length];
      dataInput.readFully(result);
    }
    return result;
  }
}
