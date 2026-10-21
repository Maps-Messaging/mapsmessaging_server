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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.mockito.ArgumentCaptor;

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
    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    ScheduledFuture<?> deadline = mock(ScheduledFuture.class);
    doReturn(deadline).when(scheduler).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS));
    Fixture fixture = fixture(TimeUnit.MILLISECONDS.toNanos(25), scheduler);

    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(9));
    assertNotNull(subject);
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(task.capture(), eq(TimeUnit.MILLISECONDS.toNanos(25)), eq(TimeUnit.NANOSECONDS));
    task.getValue().run();

    verify(fixture.subscription).rollbackReceived(9L);
    assertEquals(0, fixture.consumer.getOutstandingCount());
    fixture.consumer.close();
  }

  @Test
  void delayedNegativeAcknowledgementUsesSpecifiedNanosecondDelay() {
    ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    ScheduledFuture<?> original = mock(ScheduledFuture.class);
    ScheduledFuture<?> delayed = mock(ScheduledFuture.class);
    doReturn(original, delayed).when(scheduler)
        .schedule(any(Runnable.class), anyLong(), eq(TimeUnit.NANOSECONDS));
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10), scheduler);
    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(12));

    assertTrue(fixture.consumer.negativeAcknowledgement(subject, TimeUnit.MILLISECONDS.toNanos(25)));
    verify(original).cancel(false);
    ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler, times(2)).schedule(tasks.capture(), anyLong(), eq(TimeUnit.NANOSECONDS));
    tasks.getAllValues().get(1).run();

    verify(fixture.subscription).rollbackReceived(12L);
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

  @Test
  void maxDeliveryLimitStopsConsumerAttemptsWithoutDeletingTheArchive() {
    Fixture fixture = fixture(TimeUnit.SECONDS.toNanos(10));
    fixture.consumer.getConfig().setMaxDeliver(1);
    TransactionSubject subject = fixture.consumer.recordDelivery("STREAM/orders/new", message(27));
    assertNotNull(subject);
    assertFalse(fixture.consumer.terminateIfMaxDelivered("STREAM/orders/new", 28));

    assertTrue(fixture.consumer.negativeAcknowledgement(subject, 0));
    assertTrue(fixture.consumer.terminateIfMaxDelivered("STREAM/orders/new", 27));
    assertFalse(fixture.consumer.terminateIfMaxDelivered("STREAM/orders/new", 27));
    assertEquals(1, fixture.consumer.getMaxDeliveryCount());
    verify(fixture.subscription).rollbackReceived(27L);
    verify(fixture.subscription).ackReceived(27L);
    fixture.consumer.close();
  }

  private static Message message(long identifier) {
    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(identifier);
    when(message.getCreation()).thenReturn(1_730_000_000_000L);
    return message;
  }

  private static Fixture fixture(long ackWaitNanos) {
    return fixture(ackWaitNanos, null);
  }

  private static Fixture fixture(long ackWaitNanos, ScheduledExecutorService scheduler) {
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo stream = mock(StreamSubscriptionInfo.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("STREAM/orders/new");
    when(stream.getDestination()).thenReturn(destination);
    when(stream.getSubscribedEventManager()).thenReturn(subscription);
    ConsumerConfig config = new ConsumerConfig();
    config.setAckWait(ackWaitNanos);
    NamedConsumer consumer = scheduler == null
        ? new NamedConsumer("consumer", "STREAM", config, List.of(stream))
        : new NamedConsumer("consumer", "STREAM", config, List.of(stream), scheduler);
    return new Fixture(consumer, subscription);
  }

  private record Fixture(NamedConsumer consumer, SubscribedEventManager subscription) {
  }
}
