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

/**
 * This interface represents a basic Bit Set API, source is from {@link java.util.BitSet}, the main differences are
 *
 * <p>This is not {@link java.lang.Cloneable} since the underlying memory backing may not be able to
 * be cloned
 */
public interface BitSet {

  // <editor-fold desc="Bit operations">
  boolean set(int bit);

  boolean isEmpty();

  boolean clear(int bit);

  boolean isSet(int bit);

  boolean isSetAndClear(int bit);

  void flip(int bit);

  void flip(int fromIndex, int toIndex);
  // </editor-fold>

  // <editor-fold desc="Status functions">
  int length();

  int cardinality();
  // </editor-fold>

  // <editor-fold desc="Search Functions">
  int nextSetBit(int fromIndex);

  int nextSetBitAndClear(int fromIndex);

  int nextClearBit(int fromIndex);

  int previousSetBit(int fromIndex);

  int previousClearBit(int fromIndex);
  // </editor-fold>

  // <editor-fold desc="Complete bit map operations">
  void clear();
  // </editor-fold>

  // <editor-fold desc="Bitwise operations between 2 BitMaps">
  void and(BitSet map);

  void xor(BitSet map);

  void or(BitSet map);

  void andNot(BitSet map);
  // </editor-fold>

  // <editor-fold desc="Iterator functions">
  Iterator<Integer> iterator();

  ListIterator<Integer> listIterator();
  // </editor-fold>

  long getUniqueId();

  void setUniqueId(long uniqueId);
}
