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

package io.mapsmessaging.engine.destination.subscription.impl.shared;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.Constants;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImpl;
import io.mapsmessaging.engine.destination.subscription.tasks.MessageDeliveredListener;
import io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementController;
import io.mapsmessaging.engine.destination.subscription.transaction.FixedCreditManager;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class SharedSubscriptionCleanupInterleavingTest {

  private static final String DESTINATION = "queue/cleanup-interleaving";
  private static final long MESSAGE_ID = 42;

  @Test
  void expiry_during_mapping_cleanup_must_not_strand_an_auto_acknowledged_message() throws Exception {
    DestinationImpl destination = Mockito.mock(DestinationImpl.class);
    Mockito.when(destination.getFullyQualifiedNamespace()).thenReturn(DESTINATION);
    Mockito.when(destination.getTypePath()).thenReturn(List.of("type=QueueCleanupInterleavingTest"));
    SharedSubscriptionManager registry = new SharedSubscriptionManager(DESTINATION);
    Mockito.when(destination.findShareRegister(DESTINATION)).thenReturn(registry);

    SubscriptionContext context = new SubscriptionContextBuilder(DESTINATION, ClientAcknowledgement.AUTO)
        .setQos(QualityOfService.AT_LEAST_ONCE).build();
    MessageStateManagerImpl state = new MessageStateManagerImpl("cleanup-interleaving", 42,
        new BitSetFactoryImpl(Constants.BITSET_BLOCK_SIZE));
    AutoAcknowledgementController sharedAcknowledgements = new AutoAcknowledgementController(new FixedCreditManager(10));
    SharedSubscription shared = new SharedSubscription(destination, context, "cleanup-interleaving", state, sharedAcknowledgements, "group");
    registry.add("group", shared);

    CountDownLatch cleanupEntered = new CountDownLatch(1);
    CountDownLatch resumeCleanup = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      SessionImpl firstSession = session("first");
      SessionImpl expiringSession = session("expiring");
      SessionImpl receivingSession = session("receiving");
      AutoAcknowledgementController receivingAcknowledgements = new AutoAcknowledgementController(new FixedCreditManager(10));

      shared.addSession(firstSession, "first", context, new AutoAcknowledgementController(new FixedCreditManager(10)));
      SessionSharedSubscription expiring = (SessionSharedSubscription) shared.addSession(expiringSession, "expiring", context,
          new AutoAcknowledgementController(new FixedCreditManager(10)));
      shared.addSession(receivingSession, "receiving", context, receivingAcknowledgements);

      // Block at the session's storage boundary after the shared consumer iterator has started.
      Mockito.doAnswer(invocation -> {
        cleanupEntered.countDown();
        if (!resumeCleanup.await(5, TimeUnit.SECONDS)) {
          throw new AssertionError("Expiry did not release delivery-mapping cleanup");
        }
        return null;
      }).when(firstSession).clearPacketIdMapping(Mockito.anyString(), Mockito.eq(MESSAGE_ID));

      // Start at the completion boundary: the real message is in flight and its network write has completed.
      Message message = new MessageBuilder().build();
      message.setIdentifier(MESSAGE_ID);
      state.register(message);
      state.allocate(message);
      receivingAcknowledgements.sent(message);
      Assertions.assertTrue(state.hasMessagesInFlight());
      Assertions.assertEquals(1, receivingAcknowledgements.size());

      Future<?> completion = executor.submit(() -> new MessageDeliveredListener(shared, receivingAcknowledgements).taskCall());
      Assertions.assertTrue(cleanupEntered.await(5, TimeUnit.SECONDS), "Completion did not reach delivery-mapping cleanup");
      Assertions.assertEquals(0, receivingAcknowledgements.size(), "The completion has already consumed its acknowledgement");

      // This is the delete/cancel path invoked by session expiry. Keep a third member so the iterator must advance again.
      expiring.delete();
      resumeCleanup.countDown();

      Assertions.assertDoesNotThrow(() -> {
        completion.get(5, TimeUnit.SECONDS);
      }, () -> "Completion failed after concurrent expiry; outstanding=" + receivingAcknowledgements.size()
          + ", inFlight=" + state.hasMessagesInFlight() + ", atRest=" + state.pending());
      Assertions.assertFalse(state.hasMessagesInFlight(), "The completed message must leave the in-flight set");
      Assertions.assertFalse(state.hasMessage(MESSAGE_ID), "The shared subscription must release the completed message");
      Mockito.verify(destination).complete(MESSAGE_ID);
    } finally {
      resumeCleanup.countDown();
      executor.shutdownNow();
      try {
        Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Completion worker did not terminate");
      } finally {
        shared.close();
      }
    }
  }

  private SessionImpl session(String name) {
    SessionImpl session = Mockito.mock(SessionImpl.class);
    Mockito.when(session.getName()).thenReturn(name);
    return session;
  }
}
