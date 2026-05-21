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

package io.mapsmessaging.engine.destination.subscription;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.session.SubscriptionStateDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class DestinationSubscriptionManagerTest {

  @Test
  void emptyManager_basics() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    Assertions.assertEquals("dest", manager.getName());
    Assertions.assertFalse(manager.hasSubscriptions());

    Assertions.assertFalse(manager.hasInterest(1L));
    Assertions.assertFalse(manager.hasMessage(1L));
    Assertions.assertFalse(manager.expired(1L));

    Assertions.assertTrue(manager.getAll().isEmpty());
    Assertions.assertTrue(manager.getAllAtRest().isEmpty());
    Assertions.assertTrue(manager.getSubscriptionStates().isEmpty());

    Assertions.assertEquals(0, manager.size());
    Assertions.assertNull(manager.getSubscription("missing"));
  }

  @Test
  void put_isIdempotentPerKey_doesNotReplaceExisting() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable first = new FakeSubscribable("sub1", "sess1");
    FakeSubscribable second = new FakeSubscribable("sub2", "sess2");

    manager.put("key", first);
    manager.put("key", second);

    Assertions.assertSame(first, manager.getSubscription("key"));
  }

  @Test
  void remove_returnsInstanceAndRemoves() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub = new FakeSubscribable("sub", "sess");
    manager.put("key", sub);

    Subscribable removed = manager.remove("key");
    Assertions.assertSame(sub, removed);
    Assertions.assertNull(manager.getSubscription("key"));
    Assertions.assertFalse(manager.hasSubscriptions());
  }

  @Test
  void clear_removesAllSubscriptions() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    manager.put("k1", new FakeSubscribable("s1", "a"));
    manager.put("k2", new FakeSubscribable("s2", "b"));
    Assertions.assertTrue(manager.hasSubscriptions());

    manager.clear();

    Assertions.assertFalse(manager.hasSubscriptions());
    Assertions.assertEquals(0, manager.size());
  }

  @Test
  void hasInterest_trueIfAnySubscribableHasMessage() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a").withInterest(Set.of(1L, 2L));
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b").withInterest(Set.of(10L));

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    Assertions.assertTrue(manager.hasInterest(2L));
    Assertions.assertTrue(manager.hasInterest(10L));
    Assertions.assertFalse(manager.hasInterest(3L));
  }

  @Test
  void scanForInterest_removesIdsThatStillHaveInterestAnywhere() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    manager.put("k1", new FakeSubscribable("s1", "a").withAll(Set.of(1L, 2L)));
    manager.put("k2", new FakeSubscribable("s2", "b").withAll(Set.of(4L)));

    Queue<Long> removedQueue = new ArrayDeque<>(List.of(1L, 2L, 3L, 4L, 5L));
    Queue<Long> result = manager.scanForInterest(removedQueue);

    Assertions.assertEquals(List.of(3L, 5L), new ArrayList<>(result));
  }

  @Test
  void register_fansOutAndSumsNonZeroValues() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a").withRegisterResult(1);
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b").withRegisterResult(0);
    FakeSubscribable sub3 = new FakeSubscribable("s3", "c").withRegisterResult(2);

    manager.put("k1", sub1);
    manager.put("k2", sub2);
    manager.put("k3", sub3);

    int sum = manager.register((Message) null);
    Assertions.assertEquals(3, sum);

    Assertions.assertEquals(1, sub1.registerCalls.get());
    Assertions.assertEquals(1, sub2.registerCalls.get());
    Assertions.assertEquals(1, sub3.registerCalls.get());
  }

  @Test
  void expired_returnsTrueIfAnySubscribableReturnsTrue() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a").withExpiredResult(false);
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b").withExpiredResult(true);

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    Assertions.assertTrue(manager.expired(999L));
  }

  @Test
  void pauseAndResume_fanOutToAllSubscribables() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a");
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b");

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    manager.pause();
    Assertions.assertEquals(1, sub1.pauseCalls.get());
    Assertions.assertEquals(1, sub2.pauseCalls.get());

    manager.resume();
    Assertions.assertEquals(1, sub1.resumeCalls.get());
    Assertions.assertEquals(1, sub2.resumeCalls.get());
  }

  @Test
  void close_closesAllSubscribables() throws IOException {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a");
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b");

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    manager.close();

    Assertions.assertEquals(1, sub1.closeCalls.get());
    Assertions.assertEquals(1, sub2.closeCalls.get());
  }

  @Test
  void getAll_returnsUnionOfAllSubscriptionQueues() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    manager.put("k1", new FakeSubscribable("s1", "a").withAll(Set.of(1L, 2L)));
    manager.put("k2", new FakeSubscribable("s2", "b").withAll(Set.of(2L, 3L)));

    List<Long> all = new ArrayList<>(manager.getAll());

    // NaturalOrderedLongQueue should keep unique ordered ids
    Assertions.assertEquals(List.of(1L, 2L, 3L), all);
  }

  @Test
  void getSubscriptionStates_includesOnlyNonNullStates() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    SubscriptionStateDTO state1 = new SubscriptionStateDTO();
    FakeSubscribable sub1 = new FakeSubscribable("s1", "a").withState(state1);
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b").withState(null);

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    List<SubscriptionStateDTO> states = manager.getSubscriptionStates();
    Assertions.assertEquals(1, states.size());
    Assertions.assertSame(state1, states.get(0));
  }

  @Test
  void size_includesMapSizePlusChildSizes() {
    DestinationSubscriptionManager manager = new DestinationSubscriptionManager("dest");

    FakeSubscribable sub1 = new FakeSubscribable("s1", "a").withSize(5);
    FakeSubscribable sub2 = new FakeSubscribable("s2", "b").withSize(7);

    manager.put("k1", sub1);
    manager.put("k2", sub2);

    // subscriptions.size() = 2, plus 5 + 7
    Assertions.assertEquals(14, manager.size());
  }

  // -----------------------------------------------------------------------
  // Fake Subscribable
  // -----------------------------------------------------------------------
  private static final class FakeSubscribable implements Subscribable {

    private final String name;
    private final String sessionId;

    private final Set<Long> interest = new HashSet<>();
    private final Set<Long> all = new HashSet<>();

    private volatile int registerResult;
    private volatile boolean expiredResult;
    private volatile int size;
    private volatile SubscriptionStateDTO state;

    private final AtomicInteger registerCalls = new AtomicInteger();
    private final AtomicInteger pauseCalls = new AtomicInteger();
    private final AtomicInteger resumeCalls = new AtomicInteger();
    private final AtomicInteger closeCalls = new AtomicInteger();

    FakeSubscribable(String name, String sessionId) {
      this.name = name;
      this.sessionId = sessionId;
      this.size = 0;
      this.registerResult = 0;
      this.expiredResult = false;
      this.state = null;
    }

    FakeSubscribable withInterest(Set<Long> values) {
      this.interest.clear();
      this.interest.addAll(values);
      return this;
    }

    FakeSubscribable withAll(Set<Long> values) {
      this.all.clear();
      this.all.addAll(values);
      return this;
    }

    FakeSubscribable withRegisterResult(int value) {
      this.registerResult = value;
      return this;
    }

    FakeSubscribable withExpiredResult(boolean value) {
      this.expiredResult = value;
      return this;
    }

    FakeSubscribable withSize(int value) {
      this.size = value;
      return this;
    }

    FakeSubscribable withState(SubscriptionStateDTO value) {
      this.state = value;
      return this;
    }

    @Override
    public int register(Message messageIdentifier) {
      registerCalls.incrementAndGet();
      return registerResult;
    }

    @Override
    public int register(long messageId) {
      return 0;
    }

    @Override
    public boolean hasMessage(long messageIdentifier) {
      return interest.contains(messageIdentifier);
    }

    @Override
    public boolean expired(long messageIdentifier) {
      return expiredResult;
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Queue<Long> getAll() {
      return new ArrayDeque<>(all);
    }

    @Override
    public Queue<Long> getAllAtRest() {
      return new ArrayDeque<>();
    }

    @Override
    public void pause() {
      pauseCalls.incrementAndGet();
    }

    @Override
    public void resume() {
      resumeCalls.incrementAndGet();
    }

    @Override
    public SubscriptionStateDTO getState() {
      return state;
    }

    @Override
    public String getSessionId() {
      return sessionId;
    }

    @Override
    public void close() {
      closeCalls.incrementAndGet();
    }
  }
}
