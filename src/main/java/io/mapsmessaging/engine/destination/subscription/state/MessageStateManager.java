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
import java.util.Queue;

public interface MessageStateManager {

  void close() throws IOException;

  Queue<Long> getAllAtRest();

  Queue<Long> getAll();

  Queue<Long> getAllAtRest(Queue<Long> copy);

  void delete() throws IOException;

  void register(Message message);

  void register(long messageId);

  void allocate(Message message);

  void commit(long messageId);

  boolean rollback(long messageId);

  long nextMessageId();

  void rollbackInFlightMessages();

  int size();

  int pending();

  boolean hasMessagesInFlight();

  boolean hasAtRestMessages();

  boolean hasMessage(long messageId);

  void expired(long messageIdentifier);
}
