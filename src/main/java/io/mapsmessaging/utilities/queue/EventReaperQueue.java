/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.utilities.queue;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;

import java.util.LinkedList;
import java.util.Queue;

public class EventReaperQueue {

  private Queue<Long> localQueue;
  private int queueSize;

  public EventReaperQueue() {
    queueSize = 512;
    localQueue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(queueSize));
  }

  public synchronized void close() {
    localQueue.clear();
  }

  public synchronized void add(long id) {
    localQueue.add(id);
  }

  public synchronized Queue<Long> getAndClear() {
    if(!localQueue.isEmpty()) {
      if(localQueue.size() > queueSize && queueSize < 4096) {
        queueSize = queueSize*2;
      }
      Queue<Long> copy = localQueue;
      localQueue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(queueSize));
      return copy;
    }
    return new LinkedList<>();
  }

}
