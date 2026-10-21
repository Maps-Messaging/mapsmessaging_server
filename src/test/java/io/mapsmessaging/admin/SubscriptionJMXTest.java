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

package io.mapsmessaging.admin;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.utilities.admin.JMXManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

class SubscriptionJMXTest {

  @Test
  void operations_andMetrics_delegateToSubscription() throws Exception {
    DestinationSubscription subscription = Mockito.mock(DestinationSubscription.class);
    Mockito.when(subscription.getAcknowledgementType()).thenReturn("AUTO");
    Mockito.when(subscription.getSessionId()).thenReturn("session-1");
    Mockito.when(subscription.isHibernating()).thenReturn(true);
    Mockito.when(subscription.getDepth()).thenReturn(12);
    Mockito.when(subscription.getInFlight()).thenReturn(3);
    Mockito.when(subscription.isPaused()).thenReturn(true);
    Mockito.when(subscription.getMessagesSent()).thenReturn(20L);
    Mockito.when(subscription.getMessagesAcked()).thenReturn(17L);
    Mockito.when(subscription.getMessagesRolledBack()).thenReturn(2L);

    withJmxManager(subscriptionJMX -> {
      subscriptionJMX.pause();
      subscriptionJMX.resume();

      Assertions.assertTrue(subscriptionJMX.isHibernating());
      Assertions.assertEquals(12, subscriptionJMX.getAtRest());
      Assertions.assertEquals(3, subscriptionJMX.getInFlight());
      Assertions.assertTrue(subscriptionJMX.isPaused());
      Assertions.assertEquals(20L, subscriptionJMX.getMessagesSent());
      Assertions.assertEquals(17L, subscriptionJMX.getMessagesAcked());
      Assertions.assertEquals(2L, subscriptionJMX.getMessagesRolledback());
      Mockito.verify(subscription).pause();
      Mockito.verify(subscription).resume();
    }, subscription);
  }

  @Test
  void delete_removedSubscription_closesIt() throws Exception {
    DestinationSubscription subscription = Mockito.mock(DestinationSubscription.class);
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    Subscribable removed = Mockito.mock(Subscribable.class);
    Mockito.when(subscription.getAcknowledgementType()).thenReturn("AUTO");
    Mockito.when(subscription.getSessionId()).thenReturn("session-1");
    Mockito.when(subscription.getDestinationImpl()).thenReturn(destination);
    Mockito.when(destination.removeSubscription("session-1")).thenReturn(removed);

    withJmxManager(subscriptionJMX -> subscriptionJMX.delete(), subscription);

    Mockito.verify(removed).close();
  }

  @Test
  void delete_missingSubscription_doesNotAttemptClose() throws Exception {
    DestinationSubscription subscription = Mockito.mock(DestinationSubscription.class);
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    Mockito.when(subscription.getAcknowledgementType()).thenReturn("AUTO");
    Mockito.when(subscription.getSessionId()).thenReturn("session-1");
    Mockito.when(subscription.getDestinationImpl()).thenReturn(destination);

    withJmxManager(subscriptionJMX -> subscriptionJMX.delete(), subscription);

    Mockito.verify(destination).removeSubscription("session-1");
  }

  @Test
  void delete_closeFailure_propagatesIOException() throws Exception {
    DestinationSubscription subscription = Mockito.mock(DestinationSubscription.class);
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    Subscribable removed = Mockito.mock(Subscribable.class);
    IOException failure = new IOException("close failed");
    Mockito.when(subscription.getAcknowledgementType()).thenReturn("AUTO");
    Mockito.when(subscription.getSessionId()).thenReturn("session-1");
    Mockito.when(subscription.getDestinationImpl()).thenReturn(destination);
    Mockito.when(destination.removeSubscription("session-1")).thenReturn(removed);
    Mockito.doThrow(failure).when(removed).close();

    IOException thrown = Assertions.assertThrows(
        IOException.class,
        () -> withJmxManager(subscriptionJMX -> subscriptionJMX.delete(), subscription));

    Assertions.assertSame(failure, thrown);
  }

  private static void withJmxManager(ThrowingConsumer<SubscriptionJMX> assertion, DestinationSubscription subscription) throws Exception {
    JMXManager manager = Mockito.mock(JMXManager.class);
    try (MockedStatic<JMXManager> jmxManager = Mockito.mockStatic(JMXManager.class)) {
      jmxManager.when(JMXManager::getInstance).thenReturn(manager);
      assertion.accept(new SubscriptionJMX(List.of("type=Broker"), subscription));
    }
  }

  @FunctionalInterface
  private interface ThrowingConsumer<T> {
    void accept(T value) throws Exception;
  }
}
