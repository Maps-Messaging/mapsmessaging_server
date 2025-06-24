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

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;

import java.io.IOException;
import java.util.Collection;

/**
 * This class iterates over an existing Message State Manager and allows a browser to walk down the current messages that are still outstanding for this subscriber. As an iterator
 * it can not modify the underlying structure but can only walk down the list of messages. To do this, it takes a deep copy of the messages at rest structure and maintains any
 * addition or deletion from the parent structure. This will keep this structure up to date with the parent and reflect any changes based on message delivery or expiry etc.
 */

public class IteratorStateManagerImpl extends BaseMessageStateManager implements MessageStateManagerListener {

  private final MessageStateManagerImpl parent;

  public IteratorStateManagerImpl(String name, long uniqueSessionId, MessageStateManagerImpl parentState, boolean deepCopy) {
    super(name, uniqueSessionId,  new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE), new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    parent = parentState;
    if (deepCopy) {
      parent.getAllAtRest(messagesAtRest);
    }
    parent.add(this);
  }

  @Override
  public void close() throws IOException {
    super.close();
    parent.remove(this);
  }

  @Override
  public void add(long messageIdentifier, int priority) {
    // We only add based on the incoming event, not the state of the parent
  }

  @Override
  public void addAll(Collection<Long> collection) {
    // We only add based on the incoming event, not the state of the parent
  }

  @Override
  public void remove(long messageIdentifier) {
    messagesAtRest.remove(messageIdentifier);
  }

}
