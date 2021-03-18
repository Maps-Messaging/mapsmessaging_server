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
import org.jetbrains.annotations.Nullable;

/**
 * Implements a priority queue mechanism that breaks the queue into discreet priority ranges. This allows entries with a higher priority to be returned before entries with a lower
 * priority.
 *
 * <p>For example, if you create a priority queue with 10 priorities and push in 2 entries, one with
 * priority 1 and then one with priority 5. The first entry returned would be the last one entered at priority 5.
 *
 * <p>Please note: The order of priority is the larger the priority value the high the priority and
 * the closer to the top of the queue it will appear.
 */
public class PriorityQueue<T> extends PriorityCollection<T> implements Queue<T>, AutoCloseable {

  public PriorityQueue(int priorityBound, @NonNull @NotNull PriorityFactory<T> factory) {
    super(priorityBound, factory);
  }

  public PriorityQueue( @NonNull @NotNull Queue<T>[] priorityQueues, @Nullable PriorityFactory<T> factory) {
    super(priorityQueues, factory);
  }

  @Override
  public boolean offer(T t) {
    return push(t);
  }

  @Override
  public T remove() {
    T response = poll();
    if (response == null) {
      throw new NoSuchElementException();
    }
    return response;
  }

  @Override
  public T poll() {
    for (int x = prioritySize - 1; x >= 0; x--) {
      if (!priorityStructure.get(x).isEmpty()) {
        entryCount.decrementAndGet();
        return priorityStructure.get(x).remove();
      }
    }
    return null;
  }

  @Override
  public T element() {
    T response = peek();
    if (response == null) {
      throw new NoSuchElementException();
    }
    return response;
  }

  @Override
  public T peek() {
    for (int x = prioritySize - 1; x >= 0; x--) {
      if (!priorityStructure.get(x).isEmpty()) {
        return priorityStructure.get(x).peek();
      }
    }
    return null;
  }
}
