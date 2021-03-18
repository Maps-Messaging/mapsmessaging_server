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

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BitSetIterator implements Iterator<Integer> {

  private final BitSet bitSet;
  private int current;
  private int active;

  public BitSetIterator(@NonNull @NotNull BitSet bitSet) {
    this.bitSet = bitSet;
    active = -1;
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
    active = bitSet.nextSetBit(current);
    current = active + 1;
    return active;
  }

  @Override
  public void remove() {
    if(active != -1){
      bitSet.clear(active);
    }
  }
}
