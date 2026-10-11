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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NamedConsumerAckLifecycleTest {

  @Test
  void tracksDistinctConsumerSequenceAndRetainsMapsIdsInternally() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    TransactionSubject first = fixture.consumer.recordDelivery("STREAM/orders/new", message(7));
    TransactionSubject second = fixture.consumer.recordDelivery("STREAM/orders/new", message(8));

    assertEquals(7, first.getStreamSequence());
    assertEquals(8, second.getStreamSequence());
    assertEquals(1, first.getConsumerSequence());
    assertEquals(2, second.getConsumerSequence());
    assertEquals(2, fixture.consumer.getOutstandingCount());

    assertTrue(fixture.consumer.acknowledge(first, false));
    assertTrue(fixture.consumer.acknowledge(second, false));
    verify(fixture.subscription).ackReceived(7L);
    verify(fixture.subscription).ackReceived(8L);
    fixture.consumer.close();
  }

  @Test
  void timedOutAcknowledgementReturnsDeliveryToMaps() {
    Fixture fixture = fixture(TimeUnit.MILLISECONDS.toNanos(25));
    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(9));
    assertNotNull(subject);

    verify(fixture.subscription, timeout(2000)).rollbackReceived(9L);
    assertEquals(0, fixture.consumer.getOutstandingCount());
    fixture.consumer.close();
  }

  @Test
  void delayedNegativeAcknowledgementUsesSpecifiedNanosecondDelay() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(12));

    assertTrue(fixture.consumer.negativeAcknowledgement(subject, TimeUnit.MILLISECONDS.toNanos(25)));
    verify(fixture.subscription, timeout(2000)).rollbackReceived(12L);
    assertEquals(0, fixture.consumer.getOutstandingCount());
    fixture.consumer.close();
  }

  @Test
  void progressDoesNotCompleteAndAcknowledgementCancelsDeadline() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(10));

    assertTrue(fixture.consumer.workingProgress(subject));
    assertEquals(1, fixture.consumer.getOutstandingCount());
    assertTrue(fixture.consumer.acknowledge(subject, false));
    assertFalse(fixture.consumer.acknowledge(subject, false));
    verify(fixture.subscription, times(1)).ackReceived(10L);
    verify(fixture.subscription, never()).rollbackReceived(10L);
    fixture.consumer.close();
  }

  @Test
  void closingConsumerRequeuesOutstandingDelivery() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    fixture.consumer.recordDelivery("STREAM/orders/new", message(11));

    fixture.consumer.close();

    verify(fixture.subscription, times(1)).rollbackReceived(11L);
    assertEquals(0, fixture.consumer.getOutstandingCount());
    assertNull(fixture.consumer.recordDelivery("STREAM/orders/new", message(12)));
  }

  @Test
  void staleStreamSequenceCannotAcknowledgeAnotherDelivery() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    TransactionSubject original = fixture.consumer.recordDelivery("STREAM/orders/new", message(13));
    TransactionSubject stale = new TransactionSubject("STREAM", "consumer", original.getDeliverySequence(),
        42, original.getConsumerSequence(), original.getTimestamp(), original.getPending());

    assertFalse(fixture.consumer.acknowledge(stale, false));
    assertTrue(fixture.consumer.acknowledge(original, false));
    verify(fixture.subscription).ackReceived(13L);
    fixture.consumer.close();
  }

  private static Message message(long identifier) {
    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(identifier);
    when(message.getCreation()).thenReturn(1_730_000_000_000L);
    return message;
  }

  private static Fixture fixture(long ackWaitNanos) {
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo stream = mock(StreamSubscriptionInfo.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("STREAM/orders/new");
    when(stream.getDestination()).thenReturn(destination);
    when(stream.getSubscribedEventManager()).thenReturn(subscription);
    ConsumerConfig config = new ConsumerConfig();
    config.setAckWait(ackWaitNanos);
    NamedConsumer consumer = new NamedConsumer("consumer", "STREAM", config, List.of(stream));
    return new Fixture(consumer, subscription);
  }

  private record Fixture(NamedConsumer consumer, SubscribedEventManager subscription) {
  }
}
