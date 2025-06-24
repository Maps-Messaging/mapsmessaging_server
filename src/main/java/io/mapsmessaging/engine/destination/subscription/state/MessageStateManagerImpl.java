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
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;

import java.util.ArrayList;
import java.util.List;

public class MessageStateManagerImpl extends BaseMessageStateManager {

  private final List<MessageStateManagerListener> listeners;

  public MessageStateManagerImpl(String name, long uniqueSessionId, BitSetFactory bitsetFactory) {
    super(name, uniqueSessionId, bitsetFactory, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    listeners = new ArrayList<>();
  }

  //<editor-fold desc="Message state change APIs">
  @Override
  public synchronized void register(Message message) {
    super.register(message);
    for (MessageStateManagerListener listener : listeners) {
      listener.add(message.getIdentifier(), message.getPriority().getValue());
    }
  }

  @Override
  public synchronized void rollbackInFlightMessages() {
    for (MessageStateManagerListener listener : listeners) {
      listener.addAll(messagesInFlight);
    }
    super.rollbackInFlightMessages();
  }

  @Override
  public synchronized void commit(long messageId) {
    super.commit(messageId);
    for (MessageStateManagerListener listener : listeners) {
      listener.remove(messageId);
    }
  }
  //</editor-fold>


  //<editor-fold desc="Listener management APIs">
  public void add(MessageStateManagerListener listener) {
    listeners.add(listener);
  }

  public void remove(MessageStateManagerListener listener) {
    listeners.remove(listener);
  }
  //</editor-fold>
}
