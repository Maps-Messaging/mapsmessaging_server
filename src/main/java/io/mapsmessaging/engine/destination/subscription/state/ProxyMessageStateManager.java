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
import java.util.LinkedList;
import java.util.Queue;

public class ProxyMessageStateManager implements MessageStateManager {

  private final MessageStateManagerImpl actualStateManager;
  private final BoundedMessageStateManager boundedStateManager;

  public ProxyMessageStateManager(MessageStateManagerImpl actual, BoundedMessageStateManager bounded) {
    actualStateManager = actual;
    boundedStateManager = bounded;
  }

  @Override
  public void close() throws IOException {
    boundedStateManager.remove(actualStateManager);
    actualStateManager.close();
  }

  @Override
  public Queue<Long> getAllAtRest() {
    return actualStateManager.getAllAtRest();
  }

  @Override
  public Queue<Long> getAll() {
    return actualStateManager.getAll();
  }

  @Override
  public Queue<Long> getAllAtRest(Queue<Long> copy) {
    return new LinkedList<>();
  }

  @Override
  public void delete() throws IOException {
    boundedStateManager.remove(actualStateManager);
    actualStateManager.delete();
  }

  @Override
  public void register(Message message) {
    actualStateManager.register(message);
  }

  @Override
  public void register(long messageId) {
    actualStateManager.register(messageId);
  }

  @Override
  public void allocate(Message message) {
    boundedStateManager.allocate(message);
  }

  @Override
  public void commit(long messageId) {
    boundedStateManager.commit(messageId);
  }

  @Override
  public boolean rollback(long messageId) {
    return boundedStateManager.rollback(messageId);
  }

  @Override
  public long nextMessageId() {
    return actualStateManager.nextMessageId();
  }

  @Override
  public void rollbackInFlightMessages() {
    actualStateManager.rollbackInFlightMessages();
  }

  @Override
  public int size() {
    return actualStateManager.size();
  }

  @Override
  public int pending() {
    return actualStateManager.pending();
  }

  @Override
  public boolean hasMessagesInFlight() {
    return actualStateManager.hasMessagesInFlight();
  }

  @Override
  public boolean hasAtRestMessages() {
    return actualStateManager.hasAtRestMessages();
  }

  @Override
  public boolean hasMessage(long messageId) {
    return actualStateManager.hasMessage(messageId);
  }

  @Override
  public void expired(long messageIdentifier) {
    boundedStateManager.expired(messageIdentifier);
  }
}
