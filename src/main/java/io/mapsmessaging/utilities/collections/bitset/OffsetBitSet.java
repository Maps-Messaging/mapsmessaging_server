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

import java.util.Iterator;
import java.util.ListIterator;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class OffsetBitSet implements Comparable<OffsetBitSet> {

  protected BitSet rawBitSet;
  private long start;
  private long end;

  public OffsetBitSet(@NonNull @NotNull BitSet bitSet, long offset) {
    rawBitSet = bitSet;
    this.start = offset;
    end = start + rawBitSet.length();
  }

  public void releaseBitSet() {
    rawBitSet = null;
  }

  public void clear(){
    rawBitSet.clear();
  }

  public boolean set(long bit) {
    return rawBitSet.set((int) (bit - start));
  }

  public String toString() {
    if (rawBitSet == null) {
      return "cleared - unusable";
    }
    return "Offset::{Start:" + start + ", End:" + end + "} > " + rawBitSet.toString();
  }

  public boolean clear(long bit) {
    return rawBitSet.clear((int) (bit - start));
  }

  public boolean isSet(long bit) {
    return rawBitSet.isSet((int) (bit - start));
  }

  public void flip(long bit) {
    rawBitSet.flip((int) (bit - start));
  }

  public void flip(long fromIndex, long toIndex) {
    rawBitSet.flip((int) (fromIndex - start), (int) (toIndex - start));
  }

  public int length() {
    return rawBitSet.length();
  }

  public boolean isEmpty() {
    return rawBitSet.isEmpty();
  }

  public int cardinality() {
    return rawBitSet.cardinality();
  }

  public long nextSetBit(long fromIndex) {
    int index = (int) (fromIndex - start);
    long response = rawBitSet.nextSetBit(index);
    if (response >= 0) {
      response += start;
    }
    return response;
  }

  public long nextSetBitAndClear(long fromIndex) {
    long response = rawBitSet.nextSetBitAndClear((int) (fromIndex - start));
    if (response >= 0) {
      response += start;
    }
    return response;
  }

  public long nextClearBit(long fromIndex) {
    return rawBitSet.nextClearBit((int) (fromIndex - start)) + start;
  }

  public long previousSetBit(long fromIndex) {
    return rawBitSet.previousSetBit((int) (fromIndex - start)) + start;
  }

  public long previousClearBit(long fromIndex) {
    return rawBitSet.previousClearBit((int) (fromIndex - start)) + start;
  }

  public void clearAll() {
    rawBitSet.clear();
  }

  public void and(BitSet map) {
    rawBitSet.and(map);
  }

  public void xor(BitSet map) {
    rawBitSet.xor(map);
  }

  public void or(BitSet map) {
    rawBitSet.or(map);
  }

  public void andNot(BitSet map) {
    rawBitSet.andNot(map);
  }

  public @NonNull @NotNull BitSet getBitSet() {
    return rawBitSet;
  }

  public long getStart() {
    return start;
  }

  public void reset(long start, long uniqueId) {
    this.start = start;
    end = start + rawBitSet.length();
    rawBitSet.clear();
    rawBitSet.setUniqueId(uniqueId);
  }

  public long getEnd() {
    return end;
  }

  public Iterator<Long> iterator() {
    return new OffsetBitSetIterator();
  }

  public ListIterator<Long> listIterator() {
    return new OffsetBitSetListIterator();
  }

  @Override
  public int compareTo(OffsetBitSet o) {
    long v = (start - o.start);
    if (v < 0) {
      return -1;
    }
    if (v > 0) {
      return 1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof OffsetBitSet) {
      return compareTo((OffsetBitSet) obj) == 0;
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  class OffsetBitSetListIterator implements ListIterator<Long> {

    private final ListIterator<Integer> implIterator;

    OffsetBitSetListIterator() {
      implIterator = rawBitSet.listIterator();
    }

    public boolean hasNext() {
      return implIterator.hasNext();
    }

    @Override
    public Long next() {
      return start + implIterator.next();
    }

    @Override
    public boolean hasPrevious() {
      return implIterator.hasPrevious();
    }

    @Override
    public Long previous() {
      return start + implIterator.previous();
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
      implIterator.remove();
    }

    @Override
    public void set(Long aLong) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(Long aLong) {
      implIterator.add((int) (aLong - start));
    }
  }

  class OffsetBitSetIterator implements Iterator<Long> {

    private final Iterator<Integer> implIterator;

    OffsetBitSetIterator() {
      implIterator = rawBitSet.iterator();
    }

    @Override
    public boolean hasNext() {
      return implIterator.hasNext();
    }

    @Override
    public Long next() {
      return start + implIterator.next();
    }

    @Override
    public void remove() {
      implIterator.remove();
    }

  }
}
