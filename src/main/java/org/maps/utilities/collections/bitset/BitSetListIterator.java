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

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ListIterator;
import java.util.NoSuchElementException;

class BitSetListIterator implements ListIterator<Integer> {

  private final BitSet active;
  private int current;
  private int selected;

  public BitSetListIterator(@NonNull @NotNull BitSet bitSet) {
    active = bitSet;
    current = 0;
    selected = -1;
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
    selected = active.nextSetBit(current);
    if (selected == -1) {
      throw new NoSuchElementException();
    }
    current = selected + 1;
    return selected;
  }

  @Override
  public boolean hasPrevious() {
    if (current >= 0) {
      if(current >= active.length()){
        current--;
      }
      int previousSet = active.previousSetBit(current);
      return previousSet != -1;
    }
    return false;
  }

  @Override
  public Integer previous() {
    if (current < 0) {
      throw new NoSuchElementException();
    }
    selected = active.previousSetBit(current);
    if (current == -1) {
      throw new NoSuchElementException();
    }
    current = selected - 1;
    return selected;
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
    if(selected != -1) {
      active.clear(selected);
    }
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
