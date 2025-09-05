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

package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.UniqueIdHelper;
import io.mapsmessaging.utilities.collections.ConcurrentNaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.PriorityCollection;
import io.mapsmessaging.utilities.collections.PriorityQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;

import java.io.IOException;
import java.util.Queue;

public abstract class BaseMessageStateManager implements MessageStateManager {

  protected final Logger logger;
  protected final PriorityQueue<Long> messagesAtRest;
  protected final PriorityCollection<Long> messagesInFlight;
  protected final String name;

  protected BaseMessageStateManager(String name, long uniqueSessionId, BitSetFactory priorityBitSetFactory, BitSetFactory inflightBitSetFactory) {
    this.name = name;
    logger = LoggerFactory.getLogger(MessageStateManagerImpl.class);
    NaturalOrderedLongQueue[] priorityLists = new NaturalOrderedLongQueue[Priority.HIGHEST.getValue()];
    for (int x = 0; x < priorityLists.length; x++) {
      long priorityId = UniqueIdHelper.compute(uniqueSessionId, x);
      priorityLists[x] = new ConcurrentNaturalOrderedLongQueue(priorityId, priorityBitSetFactory);
    }
    messagesAtRest = new PriorityQueue<>(priorityLists, null);

    priorityLists = new NaturalOrderedLongQueue[Priority.HIGHEST.getValue()];
    for (int x = 0; x < priorityLists.length; x++) {
      priorityLists[x] = new ConcurrentNaturalOrderedLongQueue(x, inflightBitSetFactory);
    }
    messagesInFlight = new PriorityQueue<>(priorityLists, null);
  }

  public void close() throws IOException {
    messagesAtRest.clear();
    messagesInFlight.clear();
  }

  @Override
  public Queue<Long> getAllAtRest() {
    Queue<Long> queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    return messagesAtRest.flatten(queue);
  }

  @Override
  public Queue<Long> getAll() {
    Queue<Long> queue = new NaturalOrderedLongQueue(0, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    messagesAtRest.flatten(queue);
    queue.addAll(messagesInFlight);
    return queue;
  }

  @Override
  public Queue<Long> getAllAtRest(Queue<Long> copy) {
    copy.addAll(messagesAtRest);
    return copy;
  }

  public synchronized String toString() {
    return name + " At Rest:" + messagesAtRest + " InFlight:" + messagesInFlight;
  }

  @Override
  public void delete() throws IOException {
    close();
    messagesInFlight.clear();
  }

  @Override
  public synchronized void register(Message message) {
    messagesAtRest.add(message.getIdentifier(), message.getPriority().getValue());
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_REGISTER, name, message.getIdentifier());
  }

  @Override
  public synchronized void register(long messageId) {
    messagesAtRest.add(messageId, Priority.ONE_BELOW_HIGHEST.getValue());
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_REGISTER, name, messageId);
  }

  @Override
  public synchronized void allocate(Message message) {
    messagesInFlight.add(message.getIdentifier(), message.getPriority().getValue());
    if (!messagesAtRest.remove(message.getIdentifier())) {
      messagesInFlight.remove(message.getIdentifier());
    }
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_ALLOCATE, name, message.getIdentifier());
  }

  @Override
  public synchronized void commit(long messageId) {
    messagesInFlight.remove(messageId);
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_COMMIT, name, messageId);
  }


  public synchronized boolean rollback(long messageId) {
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_ROLLBACK, name, messageId);
    int priority = messagesInFlight.removeAndGetPriority(messageId);
    if (priority != -1) {
      priority = io.mapsmessaging.api.features.Constants.getInstance().getRollbackPriority().incrementPriority(priority);
      messagesAtRest.add(messageId, priority);
      return true;
    }
    return false;
  }


  @SuppressWarnings("java:S2201") // size also resets the internals
  @Override
  public synchronized long nextMessageId() {
    Long response = messagesAtRest.peek();
    if (response == null) {
      response = -1L;
      messagesAtRest.size(); // reset the actual size since we do not have any messages
    }
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_NEXT, name, response);
    return response;
  }

  @Override
  public synchronized void rollbackInFlightMessages() {
    if (!messagesInFlight.isEmpty()) {
      logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_ROLLBACK_INFLIGHT, name, messagesInFlight);
      messagesAtRest.addAll(messagesInFlight);
      messagesInFlight.clear();
      logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_ROLLED_BACK_INFLIGHT, name);
    }
  }

  @Override
  public synchronized int size() {
    return messagesAtRest.size() + messagesInFlight.size();
  }

  @Override
  public synchronized int pending() {
    return messagesAtRest.size();
  }

  @Override
  public synchronized boolean hasMessagesInFlight() {
    return !messagesInFlight.isEmpty();
  }

  @Override
  public synchronized boolean hasAtRestMessages() {
    return !messagesAtRest.isEmpty();
  }

  @Override
  public synchronized boolean hasMessage(long messageId) {
    return (messagesAtRest.contains(messageId) || messagesInFlight.contains(messageId));
  }

  @Override
  public synchronized void expired(long messageIdentifier) {
    messagesAtRest.remove(messageIdentifier);
    messagesInFlight.remove(messageIdentifier);
  }
}