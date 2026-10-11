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

package io.mapsmessaging.network.protocol.impl.nats.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConnectedStateDeliveryTest {

  @Test
  void completesOnlyWhenWrittenFrameCompletes() {
    SessionState session = mock(SessionState.class);
    when(session.send(any(NatsFrame.class))).thenReturn(true);
    SubscriptionContext subscription = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("events.topic");
    frame.setPayload(new byte[]{1});
    when(subscription.getAlias()).thenReturn("sid-1");
    when(session.buildPayloadFrame(message, "events.topic")).thenReturn(frame);
    when(session.getSubscriptions()).thenReturn(Map.of("events.topic", List.of(subscription)));
    when(session.getNamedConsumers()).thenReturn(Map.of());
    AtomicInteger completions = new AtomicInteger();

    assertTrue(new ConnectedState().sendMessage(session, "events.topic", subscription, message, completions::incrementAndGet));
    assertEquals(0, completions.get());
    var written = org.mockito.ArgumentCaptor.forClass(NatsFrame.class);
    verify(session).send(written.capture());
    written.getValue().complete();
    assertEquals(1, completions.get());
    written.getValue().complete();
    assertEquals(1, completions.get());
  }

  @Test
  void multipleSubscriptionsCompleteDeliveryOnceAfterBothWrites() {
    SessionState session = mock(SessionState.class);
    when(session.send(any(NatsFrame.class))).thenReturn(true);
    SubscriptionContext first = mock(SubscriptionContext.class);
    SubscriptionContext second = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("events.topic");
    frame.setPayload(new byte[]{1});
    when(first.getAlias()).thenReturn("sid-1");
    when(second.getAlias()).thenReturn("sid-2");
    when(session.buildPayloadFrame(message, "events.topic")).thenReturn(frame);
    when(session.getSubscriptions()).thenReturn(Map.of("events.topic", List.of(first, second)));
    when(session.getNamedConsumers()).thenReturn(Map.of());
    AtomicInteger completions = new AtomicInteger();

    assertTrue(new ConnectedState().sendMessage(session, "events.topic", first, message, completions::incrementAndGet));
    var written = org.mockito.ArgumentCaptor.forClass(NatsFrame.class);
    verify(session, times(2)).send(written.capture());
    written.getAllValues().get(0).complete();
    assertEquals(0, completions.get());
    written.getAllValues().get(1).complete();
    assertEquals(1, completions.get());
  }

  @Test
  void noMatchingSubscriptionCompletesOnceWithoutWriting() {
    SessionState session = mock(SessionState.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    when(session.buildPayloadFrame(message, "events.topic")).thenReturn(new MsgFrame(1024));
    when(session.getSubscriptions()).thenReturn(Map.of());
    when(session.getNamedConsumers()).thenReturn(Map.of());
    AtomicInteger completions = new AtomicInteger();

    assertTrue(new ConnectedState().sendMessage(session, "events.topic", context, message, completions::incrementAndGet));
    assertEquals(1, completions.get());
    verify(session, never()).send(any(NatsFrame.class));
  }
  @Test
  void overlappingFiltersUseOnlyTheOwningMapsSubscription() {
    SessionState session = mock(SessionState.class);
    SubscriptionContext wildcard = mock(SubscriptionContext.class);
    SubscriptionContext exact = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("events.a");
    frame.setPayload(new byte[]{1});
    when(wildcard.getAlias()).thenReturn("wildcard");
    when(exact.getAlias()).thenReturn("exact");
    when(session.getSubscriptions()).thenReturn(Map.of("events/+", List.of(wildcard), "events/a", List.of(exact)));
    when(session.buildPayloadFrame(message, "events/a")).thenReturn(frame);
    when(session.send(any(NatsFrame.class))).thenReturn(true);
    AtomicInteger completions = new AtomicInteger();

    assertTrue(new ConnectedState().sendMessage(session, "events/a", wildcard, message, completions::incrementAndGet));
    var captured = org.mockito.ArgumentCaptor.forClass(NatsFrame.class);
    verify(session).send(captured.capture());
    assertEquals("wildcard", ((PayloadFrame) captured.getValue()).getSubscriptionId());
    captured.getValue().complete();
    assertEquals(1, completions.get());
  }

  @Test
  void queueGroupSelectsExactlyOneMemberOnEachDelivery() {
    SessionState session = mock(SessionState.class);
    SubscriptionContext first = mock(SubscriptionContext.class);
    SubscriptionContext second = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("jobs.work");
    frame.setPayload(new byte[]{1});
    when(first.getAlias()).thenReturn("member-1");
    when(second.getAlias()).thenReturn("member-2");
    when(first.getSharedName()).thenReturn("workers");
    when(second.getSharedName()).thenReturn("workers");
    when(session.getSubscriptions()).thenReturn(Map.of("jobs.work", List.of(first, second)));
    when(session.buildPayloadFrame(message, "jobs.work")).thenReturn(frame);
    when(session.nextQueueGroupIndex("jobs.work", "workers", 2)).thenReturn(0, 1);
    when(session.send(any(NatsFrame.class))).thenReturn(true);

    ConnectedState state = new ConnectedState();
    AtomicInteger completions = new AtomicInteger();
    assertTrue(state.sendMessage(session, "jobs.work", first, message, completions::incrementAndGet));
    assertTrue(state.sendMessage(session, "jobs.work", first, message, completions::incrementAndGet));
    var captured = org.mockito.ArgumentCaptor.forClass(NatsFrame.class);
    verify(session, times(2)).send(captured.capture());
    assertEquals("member-1", ((PayloadFrame) captured.getAllValues().get(0)).getSubscriptionId());
    assertEquals("member-2", ((PayloadFrame) captured.getAllValues().get(1)).getSubscriptionId());
    captured.getAllValues().forEach(NatsFrame::complete);
    assertEquals(2, completions.get());
  }

  @Test
  void failedQueueSubmissionStillReleasesMapsCompletion() {
    SessionState session = mock(SessionState.class);
    SubscriptionContext subscriber = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("events.topic");
    frame.setPayload(new byte[]{1});
    when(subscriber.getAlias()).thenReturn("sid");
    when(session.getSubscriptions()).thenReturn(Map.of("events.topic", List.of(subscriber)));
    when(session.buildPayloadFrame(message, "events.topic")).thenReturn(frame);
    AtomicInteger completions = new AtomicInteger();

    assertTrue(new ConnectedState().sendMessage(session, "events.topic", subscriber, message, completions::incrementAndGet));
    assertEquals(1, completions.get());
  }

  @Test
  void repeatedSubscriptionAliasDoesNotDuplicateNetworkFrames() {
    SessionState session = mock(SessionState.class);
    SubscriptionContext one = mock(SubscriptionContext.class);
    SubscriptionContext duplicate = mock(SubscriptionContext.class);
    Message message = mock(Message.class);
    PayloadFrame frame = new MsgFrame(1024);
    frame.setSubject("events.topic");
    frame.setPayload(new byte[]{1});
    when(one.getAlias()).thenReturn("sid");
    when(duplicate.getAlias()).thenReturn("sid");
    when(session.getSubscriptions()).thenReturn(Map.of("events.topic", List.of(one, duplicate)));
    when(session.buildPayloadFrame(message, "events.topic")).thenReturn(frame);

    assertTrue(new ConnectedState().sendMessage(session, "events.topic", one, message, null));
    verify(session).send(any(NatsFrame.class));
  }

}
