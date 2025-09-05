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

import io.mapsmessaging.api.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BoundedMessageStateManager implements MessageStateManager {

  private final List<MessageStateManagerImpl> sharedStateList;


  public BoundedMessageStateManager() {
    sharedStateList = new ArrayList<>();
  }

  public boolean add(MessageStateManagerImpl messageStateManager) {
    return sharedStateList.add(messageStateManager);
  }

  public boolean remove(MessageStateManagerImpl messageStateManager) {
    return sharedStateList.remove(messageStateManager);
  }

  public boolean hasMessage(long messageIdentifier) {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      if (messageStateManager.hasMessage(messageIdentifier)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void expired(long messageIdentifier) {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.expired(messageIdentifier);
    }
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return new LinkedList<>();
  }

  @Override
  public Queue<Long> getAll() {
    return new LinkedList<>();
  }

  @Override
  public Queue<Long> getAllAtRest(Queue<Long> copy) {
    return new LinkedList<>();
  }

  @Override
  public void delete() throws IOException {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.delete();
    }
  }

  @Override
  public void register(Message message) {
    // All registering of messages MUST be done on the actual MessageStateManager
  }

  @Override
  public void register(long messageId) {
    // All registering of messages MUST be done on the actual MessageStateManager
  }

  @Override
  public void allocate(Message messageIdentifier) {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.allocate(messageIdentifier);
    }
  }

  public void commit(long messageIdentifier) {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.commit(messageIdentifier);
    }
  }

  @Override
  public boolean rollback(long messageIdentifier) {
    boolean rolledBack = false;
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      if (messageStateManager.rollback(messageIdentifier)) {
        rolledBack = true;
      }
    }
    return rolledBack;
  }

  @Override
  public long nextMessageId() {
    return 0;
  }

  @Override
  public void rollbackInFlightMessages() {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.rollbackInFlightMessages();
    }
  }

  @Override
  public int size() {
    int size = 0;
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      size += messageStateManager.size();
    }

    return size;
  }

  @Override
  public int pending() {
    int pending = 0;
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      pending += messageStateManager.pending();
    }

    return pending;
  }

  @Override
  public boolean hasMessagesInFlight() {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      if (messageStateManager.hasMessagesInFlight()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean hasAtRestMessages() {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      if (messageStateManager.hasAtRestMessages()) {
        return true;
      }
    }
    return false;
  }

  public void close() throws IOException {
    for (MessageStateManagerImpl messageStateManager : sharedStateList) {
      messageStateManager.close();
    }
    sharedStateList.clear();
  }
}
