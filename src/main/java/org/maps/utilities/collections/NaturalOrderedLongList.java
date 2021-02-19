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

package org.maps.utilities.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.maps.utilities.collections.bitset.BitSetFactory;
import org.maps.utilities.collections.bitset.OffsetBitSet;

public class NaturalOrderedLongList extends NaturalOrderedCollection implements List<Long> {

  public NaturalOrderedLongList(int id, @NonNull @NotNull BitSetFactory factory) {
    super(id, factory);
  }

  @Override
  public boolean addAll(int index, @NonNull @NotNull  Collection<? extends Long> c) {
    for (Long item : c) {
      add(item);
    }
    return true;
  }

  @Override
  public Long get(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long set(int index, Long element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, Long element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull @NotNull ListIterator<Long> listIterator() {
    return new LongListIterator();
  }

  @Override
  public @NonNull @NotNull ListIterator<Long> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull @NotNull List<Long> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(IntFunction<T[]> generator) {
    throw new UnsupportedOperationException();
  }

  // <editor-fold desc="Standard Iterator">
  class LongListIterator implements ListIterator<Long> {

    ArrayList<ListIterator<Long>> iterators;
    int iteratorIndex;
    ListIterator<Long> active;

    LongListIterator() {
      iterators = new ArrayList<>();
      for (OffsetBitSet bitMap : tree.values()) {
        iterators.add(bitMap.listIterator());
      }
      active = iterators.get(0);
      iteratorIndex = 0;
    }

    @Override
    public boolean hasNext() {
      if (active.hasNext()) {
        return true;
      } else {
        iteratorIndex++;
        if (iteratorIndex >= iterators.size()) {
          active = null;
          return false;
        }
        active = iterators.get(iteratorIndex);
        return hasNext();
      }
    }

    @Override
    public Long next() {
      if (active != null) {
        return active.next();
      }
      return null;
    }

    @Override
    public boolean hasPrevious() {
      if (active.hasPrevious()) {
        return true;
      } else {
        iteratorIndex--;
        if (iteratorIndex < 0) {
          active = null;
          return false;
        }
        active = iterators.get(iteratorIndex);
        return hasPrevious();
      }
    }

    @Override
    public Long previous() {
      if (active != null) {
        return active.previous();
      }
      return null;
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
      if (active != null) {
        active.remove();
      }
    }

    @Override
    public void set(Long aLong) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(Long aLong) {
      NaturalOrderedLongList.this.add(aLong);
    }

    @Override
    public void forEachRemaining(Consumer<? super Long> action) {
      throw new UnsupportedOperationException();
    }
  }
}
