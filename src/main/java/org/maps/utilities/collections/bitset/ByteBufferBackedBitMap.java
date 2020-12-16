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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.ListIterator;
import org.jetbrains.annotations.NotNull;
import org.maps.utilities.collections.bitset.BitWiseOperator.And;
import org.maps.utilities.collections.bitset.BitWiseOperator.AndNot;
import org.maps.utilities.collections.bitset.BitWiseOperator.Or;
import org.maps.utilities.collections.bitset.BitWiseOperator.Xor;

public class ByteBufferBackedBitMap implements BitSet {

  private static final long LONG_MASK = 0xffffffffffffffffL;
  private static final int LONG_BIT_SHIFT = 6;
  private static final int BYTE_BIT_SHIFT = 3;
  private static final int LONG_SIZE = 8;
  private static final int LONG_BITS = 64;

  private final int longStartIdx;
  private final int longs;
  private final int capacity;
  private final int offset;

  private ByteBuffer backing;
  private long uniqueId;

  // <editor-fold desc="BitSet constructors">
  public ByteBufferBackedBitMap(@NotNull ByteBuffer buffer, int offset) {
    this(buffer, offset, 0);
  }

  public ByteBufferBackedBitMap(@NotNull ByteBuffer buffer, int offset, long uniqueId) {
    this.uniqueId = uniqueId;
    backing = buffer;
    int bits = backing.capacity() - offset;
    bits = bits * LONG_SIZE;
    longs = (bits >> LONG_BIT_SHIFT);
    capacity = longs << 6L;
    this.offset = offset;
    longStartIdx = getLongPosition(0);
  }
  // </editor-fold>

  // <editor-fold desc="Byte Buffer functions">
  public @NotNull ByteBuffer getBacking() {
    return backing;
  }

  protected @NotNull ByteBuffer clearBacking() {
    ByteBuffer buffer = backing;
    backing = null;
    return buffer;
  }

  public @NotNull ByteBuffer getBacking(ByteBuffer copy) {
    copy.clear();
    copy.put(backing);
    return copy;
  }

  public long getOffset() {
    return offset;
  }

  public long getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(long uniqueId) {
    this.uniqueId = uniqueId;
  }
  // </editor-fold>

  // <editor-fold desc="Single bit operations">

  @Override
  public boolean set(int bit) {
    int internalBit = checkBoundary(bit);
    int position = getLongPosition(internalBit);
    int bitPos = internalBit % LONG_BITS;

    long bitMask = 1L << (bitPos);

    //
    // Get the int and now set the bit within the long
    //
    long map = backing.getLong(position);
    long original = map;
    map |= bitMask;
    backing.putLong(position, map);
    return map != original;
  }

  @Override
  public boolean clear(int bit) {
    int internalBit = checkBoundary(bit);
    int position = getLongPosition(internalBit);
    int bitPos = internalBit % LONG_BITS;

    long bitMask = 1L << (bitPos);

    //
    // Get the long and now set the bit within the long
    //
    long map = backing.getLong(position);
    long original = map;
    map &= ~bitMask;
    backing.putLong(position, map);
    return map != original;
  }

  @Override
  public boolean isSet(int bit) {
    int internalBit = checkBoundary(bit);
    int position = getLongPosition(internalBit);
    int bitPos = internalBit % LONG_BITS;

    long bitMask = 1L << (bitPos);

    //
    // Get the long and now set the bit within the long
    //
    long map = backing.getLong(position);
    return (map & bitMask) != 0;
  }

  @Override
  public boolean isSetAndClear(int bit) {
    int internalBit = checkBoundary(bit);
    int position = getLongPosition(internalBit);
    int bitPos = internalBit % LONG_BITS;

    long bitMask = 1L << (bitPos);

    //
    // Get the long and now set the bit within the long
    //
    long map = backing.getLong(position);
    boolean val = (map & bitMask) != 0;
    if (val) {
      map &= ~bitMask;
      backing.putLong(position, map);
    }
    return val;
  }

  @Override
  public void flip(int bit) {
    int internalBit = checkBoundary(bit);
    int position = getLongPosition(internalBit);
    int bitPos = internalBit % LONG_BITS;

    long bitMask = 1L << (bitPos);
    long map = backing.getLong(position);
    map ^= bitMask;
    backing.putLong(position, map);
  }

  @Override
  public void flip(int fromIndex, int toIndex) {
    if (fromIndex == toIndex) {
      return;
    }
    int fromOffset = checkBoundary(fromIndex);
    int toOffset = checkBoundary(toIndex);

    int startWordIndex = getLongPosition(fromOffset);
    int endWordIndex = getLongPosition(toOffset - 1);

    long firstWordMask = LONG_MASK << fromOffset;
    long lastWordMask = LONG_MASK >>> -toOffset;
    if (startWordIndex == endWordIndex) {
      // Case 1: One word
      long map = backing.getLong(startWordIndex);
      map ^= (firstWordMask & lastWordMask);
      backing.putLong(startWordIndex, map);
    } else {
      // Case 2: Multiple words
      // Handle first word
      long map = backing.getLong(startWordIndex);
      map ^= (firstWordMask & lastWordMask);
      backing.putLong(startWordIndex, map);

      // Handle intermediate words, if any
      for (int i = startWordIndex + 1; i < endWordIndex; i++) {
        map = backing.getLong(i);
        map ^= LONG_MASK;
        backing.putLong(i, map);
      }
      map = backing.getLong(endWordIndex);
      map ^= (lastWordMask);
      backing.putLong(startWordIndex, map);
    }
  }
  // </editor-fold>

  // <editor-fold desc="Statistic functions">

  /**
   * Returns the total number of bits in the bit map that can be used
   *
   * @return the number of bits in the map
   */
  @Override
  public int length() {
    return capacity;
  }

  /**
   * Returns the number of set bits in the bit map
   *
   * @return the number of currently set bits
   */
  @Override
  public int cardinality() {
    int sum = 0;
    for (int x = 0; x < longs; x++) {
      int pos = (x << BYTE_BIT_SHIFT) + longStartIdx;
      sum += Long.bitCount(backing.getLong(pos));
    }
    return sum;
  }

  @Override
  public boolean isEmpty() {
    for (int x = 0; x < longs; x++) {
      int pos = (x << BYTE_BIT_SHIFT) + longStartIdx;
      if (backing.getLong(pos) != 0) {
        return false;
      }
    }
    return true;
  }

  // </editor-fold>

  // <editor-fold desc="Search Functions">
  @Override
  public int nextSetBit(int fromIndex) {
    int internalBit = checkBoundary(fromIndex);
    int position = getLongPosition(internalBit);
    long word = backing.getLong(position) & (LONG_MASK << internalBit);

    while (true) {
      if (word != 0) {
        int base = (position - longStartIdx) << 3;
        base += Long.numberOfTrailingZeros(word);
        return base;
      }
      position += LONG_SIZE;
      if (position == longs << 3) {
        return -1;
      }
      word = backing.getLong(position);
    }
  }

  @Override
  public int nextSetBitAndClear(int fromIndex) {
    int internalBit = checkBoundary(fromIndex);
    int position = getLongPosition(internalBit);
    long word = backing.getLong(position) & (LONG_MASK << internalBit);

    while (true) {
      if (word != 0) {
        int base = (position - longStartIdx) << 3;
        base += Long.numberOfTrailingZeros(word);

        //
        // Get the long and now set the bit within the long
        //
        long bitMask = 1L << (base);
        word &= ~bitMask;
        backing.putLong(position, word);
        return base;
      }
      position += LONG_SIZE;
      if (position == longs << 3) {
        return -1;
      }
      word = backing.getLong(position);
    }
  }

  @Override
  public int nextClearBit(int fromIndex) {
    int internalBit = checkBoundary(fromIndex);
    int position = getLongPosition(internalBit);

    long word = ~backing.getLong(position);
    while (true) {
      if (word != 0) {
        int base = (position - longStartIdx) << 3;
        base += Long.numberOfTrailingZeros(word);
        return base;
      }
      position += LONG_SIZE;
      if (position == longs << 3) {
        return -1;
      }
      word = ~backing.getLong(position);
    }
  }

  @Override
  public int previousSetBit(int fromIndex) {
    int internalBit = checkBoundary(fromIndex);
    int position = getLongPosition(internalBit);
    long word = backing.getLong(position) & (LONG_MASK >>> -(fromIndex + 1));

    while (true) {
      if (word != 0) {
        int base = ((position - longStartIdx) << 3) + 64;
        base = base - 1 - Long.numberOfLeadingZeros(word);
        return base;
      }
      position -= LONG_SIZE;
      if (position == -1) {
        return -1;
      }
      word = backing.getLong(position);
    }
  }

  @Override
  public int previousClearBit(int fromIndex) {
    int internalBit = checkBoundary(fromIndex);
    int position = getLongPosition(internalBit);
    long word = ~backing.getLong(position) & (LONG_MASK >>> -(fromIndex + 1));

    while (true) {
      if (word != 0) {
        int base = ((position - longStartIdx) << 3) + 64;
        base = base - 1 - Long.numberOfLeadingZeros(word);
        return base;
      }
      position -= LONG_SIZE;
      if (position == -1) {
        return -1;
      }
      word = ~backing.getLong(position);
    }
  }
  // </editor-fold>

  // <editor-fold desc="Complete bit map operations">
  @Override
  public void clear() {
    for (int x = 0; x < longs; x++) {
      int pos = (x << BYTE_BIT_SHIFT) + longStartIdx;
      backing.putLong(pos, 0);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ID:").append(uniqueId);
    sb.append(" Size:").append(cardinality()).append(" Offset:").append(offset).append(" {");
    int counter = 0;
    int idx = 0;
    while (counter < 1024 && idx < capacity) {
      idx = nextSetBit(idx);
      if (idx < 0) {
        break;
      }
      sb.append(idx).append(", ");
      idx++;
      counter++;
    }
    sb.append("}");
    return sb.toString();
  }
  // </editor-fold>

  // <editor-fold desc="Bitwise operations between 2 BitMaps">
  @Override
  public void and(BitSet map) {
    bitwiseCompute(map, new And());
  }

  @Override
  public void xor(BitSet map) {
    bitwiseCompute(map, new Xor());
  }

  @Override
  public void or(BitSet map) {
    bitwiseCompute(map, new Or());
  }

  @Override
  public void andNot(BitSet map) {
    bitwiseCompute(map, new AndNot());
  }

  private void bitwiseCompute(BitSet test, BitWiseOperator operator) {
    if (test instanceof ByteBufferBackedBitMap) {
      ByteBufferBackedBitMap map = (ByteBufferBackedBitMap) test;
      int len = Math.min(map.longs, longs);
      for (int x = 0; x < len; x++) {
        int position = (x << BYTE_BIT_SHIFT) + longStartIdx;
        long lhs = backing.getLong(position);
        long rhs = map.backing.getLong(position);
        long result = operator.operation(lhs, rhs);
        backing.putLong(position, result);
      }
    } else if (test instanceof BitSetImpl) {
      BitSetImpl map = (BitSetImpl) test;
      long[] values = map.getWords();
      int rhsIndex = 0;
      int len = Math.min(values.length, longs);
      for (int x = 0; x < len; x++) {
        int position = (x << BYTE_BIT_SHIFT) + longStartIdx;
        long lhs = backing.getLong(position);
        long rhs = values[rhsIndex];
        rhsIndex++;
        long result = operator.operation(lhs, rhs);
        backing.putLong(position, result);
      }
    } else {
      throw new UnsupportedOperationException(
          "Unable to perform bitwise operation from " + test.getClass().toString());
    }
  }

  public int getLongCount() {
    return longs;
  }
  // </editor-fold>

  // <editor-fold desc="positional functions">
  private int checkBoundary(int bit) {
    //
    // Must be within range
    //
    if (bit < 0 || bit > capacity) {
      throw new IndexOutOfBoundsException(
          "Expecting range from 0 to " + (capacity) + " received " + bit);
    }
    return bit;
  }

  private int getLongPosition(int bit) {
    //
    // Must be within bit set
    //
    int position = (bit >> LONG_BIT_SHIFT);

    if (position > longs) {
      throw new IndexOutOfBoundsException();
    }
    return (position << BYTE_BIT_SHIFT) + offset; // Return position in bytes
  }
  // </editor-fold>

  // <editor-fold desc="Iterator functions">
  @Override
  public Iterator<Integer> iterator() {
    return new BitSetIterator(this);
  }

  @Override
  public ListIterator<Integer> listIterator() {
    return new BitSetListIterator(this);
  }

  long getLong(int index) {
    int position = (index << BYTE_BIT_SHIFT) + offset;
    return backing.getLong(position);
  }
  // </editor-fold>
}
