/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.utilities.collections.NaturalOrderedLongQueue;
import io.mapsmessaging.utilities.collections.bitset.BitSet;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import io.mapsmessaging.utilities.collections.bitset.OffsetBitSet;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MSG423StateCopySynchronizationTest {

  private static final class CheckingFactory extends BitSetFactoryImpl {
    private volatile Object requiredMonitor;
    private final AtomicInteger reads = new AtomicInteger();

    CheckingFactory() {
      super(Constants.BITSET_BLOCK_SIZE);
    }

    @Override
    public OffsetBitSet open(long uniqueId, long id) {
      OffsetBitSet original = super.open(uniqueId, id);
      return new OffsetBitSet(original.getBitSet(), original.getStart(), original.getLogicalLength()) {
        @Override
        public BitSet getBitSet() {
          Object monitor = requiredMonitor;
          if (monitor != null) {
            Assertions.assertTrue(Thread.holdsLock(monitor), "State copy must hold the parent manager monitor");
            reads.incrementAndGet();
          }
          return super.getBitSet();
        }
      };
    }
  }

  @Test
  void stateCopiesHoldTheSameLockAsRegisterAndCommit() throws IOException {
    CheckingFactory factory = new CheckingFactory();
    MessageStateManagerImpl parent = new MessageStateManagerImpl("parent", 101L, factory);
    try {
      parent.register(1L);
      parent.register(4097L);
      factory.requiredMonitor = parent;

      Assertions.assertEquals(List.of(1L, 4097L), List.copyOf(parent.getAllAtRest()));
      Assertions.assertEquals(List.of(1L, 4097L), List.copyOf(parent.getAll()));
      Assertions.assertTrue(factory.reads.get() >= 4, "Source bitmap copy paths must exercise the lock assertion");

      Queue<Long> supplied = new NaturalOrderedLongQueue(42L, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE)) {
        @Override
        public boolean add(Long value) {
          Assertions.assertTrue(Thread.holdsLock(parent), "Copying into a caller-supplied queue must be synchronized");
          return super.add(value);
        }
      };
      parent.getAllAtRest(supplied);
      Assertions.assertEquals(List.of(1L, 4097L), List.copyOf(supplied));
    } finally {
      parent.close();
    }
  }

  private static final class CheckingParent extends MessageStateManagerImpl {
    private boolean listenerAttached;

    CheckingParent() {
      super("parent", 202L, new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    }

    @Override
    public void add(MessageStateManagerListener listener) {
      Assertions.assertTrue(Thread.holdsLock(this), "Snapshot and listener attachment must share the parent lock");
      listenerAttached = true;
      super.add(listener);
    }
  }

  @Test
  void browserSnapshotAndListenerRegistrationShareParentLock() throws IOException {
    CheckingParent parent = new CheckingParent();
    parent.register(55L);
    IteratorStateManagerImpl browser = new IteratorStateManagerImpl("browser", 303L, parent, true);
    try {
      Assertions.assertTrue(parent.listenerAttached);
      Assertions.assertTrue(browser.hasMessage(55L));
      parent.commit(55L);
      Assertions.assertFalse(browser.hasMessage(55L));
    } finally {
      browser.close();
      parent.close();
    }
  }
}
