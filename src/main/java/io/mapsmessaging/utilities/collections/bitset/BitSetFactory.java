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

package io.mapsmessaging.utilities.collections.bitset;

import java.io.IOException;
import java.util.List;

public abstract class BitSetFactory implements AutoCloseable {

  protected final int windowSize;

  protected BitSetFactory(int size) {
    windowSize = size;
  }

  public void close() throws IOException {
    // Nothing required to clean up any resources
  }

  public void delete() throws IOException {
    // Nothing required to clean up any resources
  }

  public int getSize() {
    return windowSize;
  }

  public long getStartIndex(long id) {
    return (id / windowSize) * windowSize;
  }

  public abstract void close(OffsetBitSet bitset);

  public abstract OffsetBitSet open(long uniqueId, long id) throws IOException;

  public abstract List<OffsetBitSet> get(long uniqueId);

  public abstract List<Long> getUniqueIds();
}
