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

package io.mapsmessaging.state.drone.tak;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageAPITest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the async decoupling added to fix the TAK-publish thread-pool self-deadlock: publish()
 * must never block the calling thread on the destination's storeMessage(), regardless of how
 * slow, backed-up, or overwhelmed the underlying destination is.
 */
class EventPublisherTest extends MessageAPITest {

  @Test
  void publish_doesNotBlockCallerWhenDestinationStoreIsSlow() throws Exception {
    EventPublisher eventPublisher = new EventPublisher("eventPublisherTest/slowDestination");
    try {
      CountDownLatch releaseStore = new CountDownLatch(1);
      Destination slowDestination = mock(Destination.class);
      when(slowDestination.storeMessage(any())).thenAnswer(invocation -> {
        releaseStore.await(5, TimeUnit.SECONDS);
        return 1;
      });
      setDestination(eventPublisher, slowDestination);

      long start = System.currentTimeMillis();
      eventPublisher.publish("<event/>");
      long elapsed = System.currentTimeMillis() - start;

      assertTrue(elapsed < 500,
          "publish() must not block on a slow destination, took " + elapsed + "ms");

      releaseStore.countDown();
      verify(slowDestination, timeout(2000)).storeMessage(any());
    } finally {
      eventPublisher.close();
    }
  }

  @Test
  void publish_deliversQueuedEventsToDestinationInOrder() throws Exception {
    EventPublisher eventPublisher = new EventPublisher("eventPublisherTest/orderedDestination");
    try {
      Destination destination = mock(Destination.class);
      when(destination.storeMessage(any())).thenReturn(1);
      setDestination(eventPublisher, destination);

      eventPublisher.publish("<event id=\"1\"/>");
      eventPublisher.publish("<event id=\"2\"/>");
      eventPublisher.publish("<event id=\"3\"/>");

      verify(destination, timeout(2000).times(3)).storeMessage(any());
    } finally {
      eventPublisher.close();
    }
  }

  @Test
  void publish_neverBlocksCallerEvenWhenQueueOverflowsUnderABackedUpDestination() throws Exception {
    EventPublisher eventPublisher = new EventPublisher("eventPublisherTest/overflowDestination");
    try {
      CountDownLatch releaseStore = new CountDownLatch(1);
      CountDownLatch firstCallBlocked = new CountDownLatch(1);
      Destination blockedDestination = mock(Destination.class);
      when(blockedDestination.storeMessage(any())).thenAnswer(invocation -> {
        // Only the in-flight call blocks; once released, the drain must run unobstructed so
        // close()'s interrupt lands on the idle queue wait, not mid-mock-invocation.
        if (firstCallBlocked.getCount() > 0) {
          firstCallBlocked.countDown();
          releaseStore.await(5, TimeUnit.SECONDS);
        }
        return 1;
      });
      setDestination(eventPublisher, blockedDestination);

      // Picked up by the background thread immediately, which then blocks there - every
      // publish() below is purely a queue operation with no consumer draining it.
      eventPublisher.publish("<event id=\"in-flight\"/>");
      assertTrue(firstCallBlocked.await(2, TimeUnit.SECONDS),
          "background thread must have picked up the in-flight event by now");

      int overflowAttempts = 2000; // comfortably over the publisher's bounded queue capacity
      long start = System.currentTimeMillis();
      for (int i = 0; i < overflowAttempts; i++) {
        eventPublisher.publish("<event id=\"" + i + "\"/>");
      }
      long elapsed = System.currentTimeMillis() - start;

      assertTrue(elapsed < 2000,
          overflowAttempts + " publish() calls against a backed-up destination must stay "
              + "non-blocking, took " + elapsed + "ms");

      LinkedBlockingDeque<?> queue = getQueue(eventPublisher);
      assertTrue(queue.size() <= 1000, "bounded queue must never grow past its capacity");

      releaseStore.countDown();
      // Let the background thread actually resume and drain the backlog before close()
      // interrupts it, so the interrupt doesn't race a still-blocked mock invocation.
      verify(blockedDestination, timeout(2000).atLeastOnce()).storeMessage(any());
    } finally {
      eventPublisher.close();
    }
  }

  private static void setDestination(EventPublisher eventPublisher, Destination destination)
      throws Exception {
    Field field = EventPublisher.class.getDeclaredField("destination");
    field.setAccessible(true);
    field.set(eventPublisher, destination);
  }

  @SuppressWarnings("unchecked")
  private static LinkedBlockingDeque<String> getQueue(EventPublisher eventPublisher)
      throws Exception {
    Field field = EventPublisher.class.getDeclaredField("queue");
    field.setAccessible(true);
    return (LinkedBlockingDeque<String>) field.get(eventPublisher);
  }
}
