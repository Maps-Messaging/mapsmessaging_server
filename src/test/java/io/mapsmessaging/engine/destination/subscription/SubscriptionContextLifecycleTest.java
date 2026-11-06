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

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.builders.BrowserSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubscriptionContextLifecycleTest {
  @Test
  void browser_rejects_parent_removed_before_construction() throws Exception {
    SubscriptionContext parentContext = new SubscriptionContext("/sensor");
    DestinationSubscription parent = subscription(parentContext);
    SubscriptionContext context = new SubscriptionContext("/sensor");
    context.setSelector("speed > 10");
    BrowserSubscriptionBuilder builder = new BrowserSubscriptionBuilder(mock(DestinationImpl.class), context, parent);
    parent.removeContext(parentContext);
    IOException error = assertThrows(IOException.class, () -> builder.construct(mock(SessionImpl.class), "id", "unique", 1));
    assertTrue(error.getMessage().contains("subscription"));
  }

  @Test
  void browser_rejects_parent_already_removed_before_builder_creation() {
    SubscriptionContext context = new SubscriptionContext("/sensor");
    DestinationSubscription parent = subscription(context);
    parent.removeContext(context);
    assertThrows(IOException.class, () -> new BrowserSubscriptionBuilder(mock(DestinationImpl.class), context, parent));
  }

  @Test
  void concurrent_context_removal_and_reading_returns_a_snapshot_or_null() throws Exception {
    SubscriptionContext context = new SubscriptionContext("/sensor");
    DestinationSubscription subscription = subscription(context);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var writer = executor.submit(() -> {
        start.await();
        for (int iteration = 0; iteration < 200_000; iteration++) {
          subscription.removeContext(context);
          subscription.addContext(context);
        }
        return null;
      });
      var reader = executor.submit(() -> {
        start.await();
        for (int iteration = 0; iteration < 200_000; iteration++) {
          SubscriptionContext captured = subscription.getContext();
          assertTrue(captured == null || captured == context);
          subscription.getContexts();
        }
        return null;
      });
      start.countDown();
      writer.get(10, TimeUnit.SECONDS);
      reader.get(10, TimeUnit.SECONDS);
    }
  }

  private DestinationSubscription subscription(SubscriptionContext context) {
    MessageStateManagerImpl state = new MessageStateManagerImpl("test", 1, new BitSetFactoryImpl(1024));
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("/sensor");
    return new DestinationSubscription(destination, context, null, "id", null, state, false);
  }
}
