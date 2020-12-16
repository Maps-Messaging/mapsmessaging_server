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

package org.maps.utilities.collections.bitset;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class BitSetIterator implements Iterator<Integer> {

  private final BitSet bitSet;
  private int current;

  public BitSetIterator(@NotNull BitSet bitSet) {
    this.bitSet = bitSet;
  }

  @Override
  public boolean hasNext() {
    if (current >= bitSet.length()) {
      return false;
    }
    return bitSet.nextSetBit(current) != -1;
  }

  @Override
  public Integer next() {
    if (current > bitSet.length()) {
      throw new NoSuchElementException();
    }
    int res = bitSet.nextSetBit(current);
    current = res + 1;
    return res;
  }

  @Override
  public void remove() {
    bitSet.clear(current);
  }

  @Override
  public void forEachRemaining(Consumer<? super Integer> action) {
    throw new UnsupportedOperationException();
  }
}
