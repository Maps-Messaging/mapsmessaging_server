/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

class IteratorStateManagerImplTest {

  @Test
  void constructor_deepCopyTrue_copiesParentsAtRest() throws IOException {
    BitSetFactoryImpl factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    MessageStateManagerImpl realParent =
        new MessageStateManagerImpl("parent", 100L, factory);
    MessageStateManagerImpl parent = Mockito.spy(realParent);

    parent.register(10L);
    parent.register(20L);

    IteratorStateManagerImpl iterator =
        new IteratorStateManagerImpl("iter", 200L, parent, true);

    Assertions.assertTrue(iterator.hasAtRestMessages());
    Assertions.assertTrue(iterator.hasMessage(10L));
    Assertions.assertTrue(iterator.hasMessage(20L));

    iterator.close();
    parent.close();
  }

  @Test
  void constructor_deepCopyFalse_startsEmpty() throws IOException {
    BitSetFactoryImpl factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    MessageStateManagerImpl parent =
        Mockito.spy(new MessageStateManagerImpl("parent", 101L, factory));

    parent.register(10L);
    parent.register(20L);

    IteratorStateManagerImpl iterator =
        new IteratorStateManagerImpl("iter", 201L, parent, false);

    Assertions.assertFalse(iterator.hasAtRestMessages());
    Assertions.assertFalse(iterator.hasMessage(10L));
    Assertions.assertFalse(iterator.hasMessage(20L));

    iterator.close();
    parent.close();
  }

  @Test
  void close_unregistersListenerFromParent() throws IOException {
    BitSetFactoryImpl factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    MessageStateManagerImpl parent =
        Mockito.spy(new MessageStateManagerImpl("parent", 102L, factory));

    IteratorStateManagerImpl iterator =
        new IteratorStateManagerImpl("iter", 202L, parent, false);

    Mockito.verify(parent).add(Mockito.any(MessageStateManagerListener.class));

    iterator.close();

    Mockito.verify(parent).remove(Mockito.any(MessageStateManagerListener.class));
    parent.close();
  }

  @Test
  void parentCommit_triggersIteratorRemove() throws IOException {
    BitSetFactoryImpl factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    MessageStateManagerImpl parent =
        new MessageStateManagerImpl("parent", 103L, factory);

    parent.register(55L);

    IteratorStateManagerImpl iterator =
        new IteratorStateManagerImpl("iter", 203L, parent, true);

    Assertions.assertTrue(iterator.hasMessage(55L));

    // parent commit notifies listeners with remove(messageId)
    parent.commit(55L);

    Assertions.assertFalse(iterator.hasMessage(55L));
    Assertions.assertFalse(iterator.hasAtRestMessages());

    iterator.close();
    parent.close();
  }

  @Test
  void parentRegisterAfterIteratorCreation_doesNotAppearInIterator() throws IOException {
    BitSetFactoryImpl factory = new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE);

    MessageStateManagerImpl parent =
        new MessageStateManagerImpl("parent", 104L, factory);

    parent.register(1L);

    IteratorStateManagerImpl iterator =
        new IteratorStateManagerImpl("iter", 204L, parent, true);

    Assertions.assertTrue(iterator.hasMessage(1L));
    Assertions.assertFalse(iterator.hasMessage(2L));

    // IteratorStateManagerImpl.add(...) is intentionally a no-op,
    // so newly registered messages should NOT show up.
    parent.register(2L);

    Assertions.assertFalse(iterator.hasMessage(2L));

    iterator.close();
    parent.close();
  }
}
