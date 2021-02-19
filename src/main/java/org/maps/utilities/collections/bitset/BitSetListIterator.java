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

package org.maps.utilities.collections.bitset;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

class BitSetListIterator implements ListIterator<Integer> {

  private final BitSet active;
  private int current;

  public BitSetListIterator(@NonNull @NotNull BitSet bitSet) {
    active = bitSet;
    current = 0;
  }

  @Override
  public boolean hasNext() {
    return (active.nextSetBit(current) != -1);
  }

  @Override
  public Integer next() {
    if (current > active.length()) {
      throw new NoSuchElementException();
    }
    int res = active.nextSetBit(current);
    if (res == -1) {
      throw new NoSuchElementException();
    }
    current = res + 1;
    return res;
  }

  @Override
  public boolean hasPrevious() {
    int previousSet = active.previousSetBit(current);
    return previousSet != -1;
  }

  @Override
  public Integer previous() {
    if (current < 0) {
      throw new NoSuchElementException();
    }
    int res = active.previousSetBit(current);
    if (current == -1) {
      throw new NoSuchElementException();
    }
    current = res - 1;
    return res;
  }

  @Override
  public int nextIndex() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int previousIndex() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void remove() {
    active.clear(current);
  }

  @Override
  public void forEachRemaining(Consumer<? super Integer> action) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(Integer integer) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(Integer integer) {
    active.set(integer);
  }
}
