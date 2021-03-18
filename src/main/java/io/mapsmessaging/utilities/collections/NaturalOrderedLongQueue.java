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

package io.mapsmessaging.utilities.collections;

import java.util.NoSuchElementException;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.OffsetBitSet;

public class NaturalOrderedLongQueue extends NaturalOrderedCollection implements Queue<Long> {

  public NaturalOrderedLongQueue(int id, @NonNull @NotNull BitSetFactory factory) {
    super(id, factory);
  }

  @Override
  public boolean offer(Long aLong) {
    add(aLong);
    return true;
  }

  @Override
  public Long remove() {
    Long response = poll();
    if (response == null || response == -1) {
      throw new NoSuchElementException();
    }
    return response;
  }

  @Override
  public Long poll() {
    if (tree.isEmpty()) {
      return -1L;
    }
    OffsetBitSet current = tree.firstEntry().getValue();

    long val = current.nextSetBitAndClear(current.getStart());
    if (val != -1) {
      //
      // Check and see if this was the last entry in the bit set, if so then remove the entry
      //
      if (current.isEmpty() && tree.size() > 1) {
        tree.remove(current.getStart());
        factory.close(current);
      }
      return val;
    }
    tree.remove(current.getStart());
    factory.close(current);
    return poll();
  }

  @Override
  public Long element() {
    return peek();
  }

  @Override
  public Long peek() {
    for (OffsetBitSet bitMap : tree.values()) {
      if (bitMap.cardinality() != 0) {
        return bitMap.nextSetBit(bitMap.getStart());
      }
    }
    return null;
  }
}
