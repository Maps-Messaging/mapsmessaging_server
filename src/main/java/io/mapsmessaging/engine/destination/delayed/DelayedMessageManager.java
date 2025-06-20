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

package io.mapsmessaging.engine.destination.delayed;

import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;

/**
 * This class maintains a list of messages that have a delayed delivery time requested, this means the client has sent us a message with a time in the future that the event should
 * be processed. This means we need to store the event ( not this classes problem ) and then maintain a list of events based on the time to process and be able to recover from a
 * server restart. This is what this class does.
 *
 * It stores each event in the structure. The structure is made up of buckets that store events for the same time for processing, this avoids collision issues on the time.
 *
 * This class is NOT thread safe, and it's assumed that the calling functions will maintain the thread safety
 */
public class DelayedMessageManager extends MessageManager {

  /**
   * Constructor that takes a bitset factory so it can allocate and deallocate bitsets as required
   *
   * @param factory implementation of a BitSetFactory, if its persistent or in memory
   */
  public DelayedMessageManager(BitSetFactory factory) {
    super(factory);
  }

  /**
   * Once the message is processed the message identifier must be removed else it will be returned again and again until it is. This enables the server to postpone or delay
   * processing due to some external reason, like shutdown or pause etc.
   *
   * @param messageIdentifier that has been processed and is no longer required
   * @return True if the message identifier has been removed, else false if it could not be found
   */
  public synchronized boolean remove(long messageIdentifier) {
    if (!bucketList.isEmpty()) {
      int index = 0;
      while (index < bucketList.size()) {
        long next = bucketList.get(index);
        index++;
        DelayedBucket delayedBucket = treeList.get(next);
        if (delayedBucket.delayedMessageState.remove(messageIdentifier)) {
          if (delayedBucket.delayedMessageState.isEmpty()) {
            treeList.remove(next);
            bucketList.remove(0);
          }
          counter.decrementAndGet();
          return true;
        }
      }
    }
    return false;
  }

}
