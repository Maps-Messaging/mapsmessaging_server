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

package org.maps.messaging.engine.destination.subscription.state;

import static org.maps.messaging.engine.Constants.BITSET_BLOCK_SIZE;

import java.util.ArrayList;
import java.util.List;
import org.maps.messaging.api.message.Message;
import org.maps.utilities.collections.bitset.BitSetFactory;
import org.maps.utilities.collections.bitset.BitSetFactoryImpl;

public class MessageStateManagerImpl extends BaseMessageStateManager {

  private final List<MessageStateManagerListener> listeners;

  public MessageStateManagerImpl(String name, BitSetFactory bitsetFactory) {
    super(name, bitsetFactory, new BitSetFactoryImpl(BITSET_BLOCK_SIZE));
    listeners = new ArrayList<>();
  }

  //<editor-fold desc="Message state change APIs">
  @Override
  public synchronized void register(Message message){
    super.register(message);
    for(MessageStateManagerListener listener:listeners){
      listener.add(message.getIdentifier(), message.getPriority().getValue());
    }
  }

  @Override
  public synchronized void rollbackInFlightMessages() {
    for(MessageStateManagerListener listener:listeners){
      listener.addAll(messagesInFlight);
    }
    super.rollbackInFlightMessages();
  }

  @Override
  public synchronized void commit(long messageId) {
    super.commit(messageId);
    for(MessageStateManagerListener listener:listeners){
      listener.remove(messageId);
    }
  }
  //</editor-fold>


  //<editor-fold desc="Listener management APIs">
  public void add(MessageStateManagerListener listener){
    listeners.add(listener);
  }

  public void remove(MessageStateManagerListener listener){
    listeners.remove(listener);
  }
  //</editor-fold>
}
