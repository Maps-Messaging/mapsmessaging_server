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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class TransactionHandlerTest {
  private final TransactionHandler handler = new TransactionHandler();
  private SessionState session;
  private PayloadFrame frame;
  private SubscribedEventManager subscription;

  @BeforeEach
  void setUp() {
    session = mock(SessionState.class);
    frame = mock(PayloadFrame.class);
    subscription = mock(SubscribedEventManager.class);
    NamedConsumer consumer = mock(NamedConsumer.class);
    StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
    when(consumer.getStreams()).thenReturn(List.of(info));
    when(info.getSubscribedEventManager()).thenReturn(subscription);
    when(session.getNamedConsumers()).thenReturn(Map.of("consumer", consumer));
    when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.1.0.42.123.0");
  }

  @Test
  void emptyPayloadAcknowledgesDelivery() throws Exception {
    when(frame.getPayload()).thenReturn(new byte[0]);
    assertNull(handler.handle(frame, null, session));
    verify(subscription).ackReceived(42L);
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void nullPayloadAcknowledgesDelivery() throws Exception {
    assertNull(handler.handle(frame, null, session));
    verify(subscription).ackReceived(42L);
  }

  @Test
  void negativeAcknowledgementRollsBackDelivery() throws Exception {
    when(frame.getPayload()).thenReturn("-NAK".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription).rollbackReceived(42L);
    verify(subscription, never()).ackReceived(anyLong());
  }

  @Test
  void delayedNegativeAcknowledgementStillRollsBack() throws Exception {
    when(frame.getPayload()).thenReturn("-NAK {\"delay\":1000000000}".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription).rollbackReceived(42L);
  }

  @Test
  void malformedSixPartSubjectDoesNotIndexBeyondEnd() throws Exception {
    when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.1.0");
    assertNull(handler.handle(frame, null, session));
    verifyNoInteractions(subscription);
  }

  @Test
  void malformedNumericSubjectDoesNotThrow() throws Exception {
    when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.bad.0.42.123.0");
    assertNull(handler.handle(frame, null, session));
    verifyNoInteractions(subscription);
  }

  @Test
  void negativeDestinationIndexDoesNotAccessList() throws Exception {
    when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.1.-1.42.123.0");
    assertNull(handler.handle(frame, null, session));
    verifyNoInteractions(subscription);
  }
}
