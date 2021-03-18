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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class ByteBufferBitSetFactoryImpl extends BitSetFactory {

  public ByteBufferBitSetFactoryImpl(int size) {
    super(size);
  }

  @Override
  public OffsetBitSet open(long uniqueId, long id) {
    BitSet bs = new ByteBufferBackedBitMap(ByteBuffer.allocateDirect(windowSize / 8), 0);
    return new OffsetBitSet(bs, getStartIndex(id));
  }

  @Override
  public List<Long> getUniqueIds() {
    return new ArrayList<>();
  }

  @Override
  public void close(@NonNull @NotNull OffsetBitSet bitset) {
    bitset.clearAll();
  }

  @Override
  public List<OffsetBitSet> get(long uniqueId) {
    return new ArrayList<>();
  }
}
