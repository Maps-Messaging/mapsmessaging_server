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

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base class that manages messages that have been written to a destination but are not yet active and can not be delivered as part of a subscription but are awaiting a trigger,
 * like a commit or a time period. Once the trigger happens it is up to the calling functions to manage to migration of the message ID to active and then clear out the current
 * context here
 */
public class MessageManager {

  protected final Map<Long, DelayedBucket> treeList;
  protected final List<Long> bucketList;
  private final BitSetFactory factory;

  protected AtomicLong counter;


  public MessageManager(BitSetFactory factory) {
    this.factory = factory;
    treeList = new LinkedHashMap<>();
    bucketList = new ArrayList<>();
    counter = new AtomicLong(0);
    List<Long> ids = factory.getUniqueIds();
    for (Long bucketId : ids) {
      bucketList.add(bucketId);
      DelayedBucket bucket = new DelayedBucket(bucketId);
      treeList.put(bucketId, bucket);
      counter.getAndAdd(bucket.size());
    }
  }

  public void close() throws IOException {
    factory.close();
    treeList.clear();
    bucketList.clear();
    counter.set(0);
  }

  public void delete() throws IOException {
    treeList.clear();
    bucketList.clear();
    counter.set(0);
    factory.delete();
  }

  public synchronized List<Long> getBucketIds() {
    List<Long> response = new ArrayList<>(bucketList);
    Collections.sort(response);
    return response;
  }

  /**
   * Add a message to the delay structure so it can be processed in the future when the delay time passes
   *
   * @param bucketId The bucket ID to register the message with
   * @param message Message to register with the delay structure
   */
  public synchronized void register(long bucketId, @NonNull @NotNull Message message) {
    DelayedBucket bucket = treeList.computeIfAbsent(bucketId, f -> {
      bucketList.add(bucketId);
      if (bucketList.size() > 1) {
        Collections.sort(bucketList);
      }
      return new DelayedBucket(bucketId);
    });
    if (bucket.register(message.getIdentifier())) {
      counter.incrementAndGet();
    }
  }

  /**
   * This removes the message identifier from the bucket, typically this implies that the processing is now complete
   *
   * @param bucketId The bucket id context
   * @param messageIdentifier The message identifier to remove from the bucket
   * @return If the transaction and the specified message identifier has been removed else false if it can not be found
   */
  public synchronized boolean remove(long bucketId, long messageIdentifier) {
    if (!bucketList.isEmpty()) {
      DelayedBucket bucket = treeList.get(bucketId);
      if (bucket != null && bucket.delayedMessageState.remove(messageIdentifier)) {
        counter.decrementAndGet();
        return true;
      }
    }
    return false;
  }

  /**
   * Removes all context for a given bucket Id
   *
   * @param bucketId the transaction id to delete
   * @return true if the transaction id was found else false indicating no known transaction
   */
  public synchronized boolean delete(long bucketId) {
    DelayedBucket delayedBucket = treeList.remove(bucketId);
    if (delayedBucket != null) {
      counter.getAndAdd(-delayedBucket.delayedMessageState.size());
      return true;
    }
    return false;
  }

  /**
   * Get the next Message Id that belongs in this bucket. This is a read only function and does not change the structure you need to call remove to move to the next message id
   *
   * @param bucketId the specific transaction that we are currently processing
   * @return The message ID of the next message to process
   */
  public synchronized long getNext(long bucketId) {
    DelayedBucket delayedBucket = treeList.get(bucketId);

    // The bucket is NOT in the tree so clean up the index and try again
    if (delayedBucket == null) {
      bucketList.remove(bucketId);
      return -1;
    }

    // The bucket is empty so lets remove the index and the bucket and try again
    if (delayedBucket.delayedMessageState.isEmpty()) {
      treeList.remove(bucketId);
      bucketList.remove(0);
      return -1;
    }
    return delayedBucket.delayedMessageState.peek();
  }

  /**
   * This returns the global size of ALL messages currently in a delayed state across all buckets
   *
   * @return number of messages currently stored
   */
  public long size() {
    return counter.get();
  }

  public boolean isEmpty() {
    return counter.get() == 0;
  }

  /**
   * Simple toString override to display the context of the message manager
   *
   * @return String representation
   */
  @Override
  public synchronized String toString() {
    StringBuilder sb = new StringBuilder("Size:");
    sb.append(size()).append("\n");
    treeList.forEach((key, value) -> sb.append("\t").append(key).append(" -> ").append(value).append("\n"));
    return sb.toString();
  }

  public synchronized @NonNull @NotNull Queue<Long> removeBucket(long bucketId) {
    DelayedBucket bucket = treeList.remove(bucketId);
    if (bucket != null) {
      return bucket.delayedMessageState;
    }
    return new ArrayDeque<>();
  }

  /**
   * Structure bucket containing message identifiers.
   *
   * Please note.. The structure containing the message identifiers has a Int (32 bit) value to uniquely identify them, however, time in milliseconds are longs (64 bits) this may
   * cause a single bucket containing delay values that span a roughly 24 day period ( Java uses 32 signed bits, so only 2^31 can be used )
   *
   * What this means is that when we process the bucket we need to confirm each events delay time and only if it has passed do we process it, else we simply pass over it
   */
  protected class DelayedBucket {

    protected final NaturalOrderedLongQueue delayedMessageState;

    public DelayedBucket(long delayTime) {
      delayedMessageState = new NaturalOrderedLongQueue((int) delayTime, factory);
    }

    public boolean register(long identifier) {
      return delayedMessageState.offer(identifier);
    }

    @Override
    public String toString() {
      return delayedMessageState.toString();
    }

    public long size() {
      return delayedMessageState.size();
    }
  }
}