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
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionHandlerTest {
  private final TransactionHandler handler = new TransactionHandler();
  private SessionState session;
  private PayloadFrame frame;
  private SubscribedEventManager subscription;
  private NamedConsumer consumer;
  private TransactionSubject acknowledgement;

  @BeforeEach
  void setUp() {
    session = mock(SessionState.class);
    frame = mock(PayloadFrame.class);
    subscription = mock(SubscribedEventManager.class);
    StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
    DestinationImpl destination = mock(DestinationImpl.class);
    when(info.getSubscribedEventManager()).thenReturn(subscription);
    when(info.getDestination()).thenReturn(destination);
    when(destination.getFullyQualifiedNamespace()).thenReturn("stream/orders");
    ConsumerConfig config = new ConsumerConfig();
    consumer = spy(new NamedConsumer("consumer", "stream", config, List.of(info)));
    when(session.getNamedConsumers()).thenReturn(Map.of("consumer", consumer));
    Message message = mock(Message.class);
    when(message.getIdentifier()).thenReturn(42L);
    when(message.getCreation()).thenReturn(1_730_000_000_000L);
    acknowledgement = consumer.recordDelivery("stream/orders", message);
    when(frame.getSubject()).thenReturn(acknowledgement.toSubject());
  }

  @Test
  void emptyPayloadAcknowledgesMappedMapsIdentifierExactlyOnce() throws Exception {
    when(frame.getPayload()).thenReturn(new byte[0]);
    assertNull(handler.handle(frame, null, session));
    assertNull(handler.handle(frame, null, session));
    verify(subscription, times(1)).ackReceived(42L);
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void explicitAckCannotBeReplayedWithForgedStreamSequence() throws Exception {
    TransactionSubject forged = new TransactionSubject("stream", "consumer", 1, 999, acknowledgement.getConsumerSequence(),
        acknowledgement.getTimestamp(), 0);
    when(frame.getSubject()).thenReturn(forged.toSubject());
    when(frame.getPayload()).thenReturn("+ACK".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription, never()).ackReceived(anyLong());
  }

  @Test
  void immediateNakRollsBackMappedMapsIdentifier() throws Exception {
    when(frame.getPayload()).thenReturn("-NAK".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription).rollbackReceived(42L);
    verify(subscription, never()).ackReceived(anyLong());
  }

  @Test
  void zeroDelayNakRollsBackWithoutWaiting() throws Exception {
    when(frame.getPayload()).thenReturn("-NAK {\"delay\":0}".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription).rollbackReceived(42L);
  }

  @Test
  void malformedDelayDoesNotAcknowledgeOrRollBack() throws Exception {
    when(frame.getPayload()).thenReturn("-NAK {\"delay\":-5}".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription, never()).ackReceived(anyLong());
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void progressExtendsDeadlineWithoutCompletingMessage() throws Exception {
    when(frame.getPayload()).thenReturn("+WPI".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    assertEquals(1, consumer.getOutstandingCount());
    verify(subscription, never()).ackReceived(anyLong());
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void terminalAckTracksRejectionSeparately() throws Exception {
    when(frame.getPayload()).thenReturn("+TERM".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    assertEquals(1, consumer.getTerminalCount());
    verify(subscription).ackReceived(42L);
  }

  @Test
  void nextAcknowledgesCurrentDelivery() throws Exception {
    when(frame.getPayload()).thenReturn("+NXT".getBytes(StandardCharsets.UTF_8));
    assertNull(handler.handle(frame, null, session));
    verify(subscription).ackReceived(42L);
  }

  @Test
  void invalidSubjectsDoNotTouchMapSubscription() throws Exception {
    when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.1.bad.1.0.0");
    assertNull(handler.handle(frame, null, session));
    verify(subscription, never()).ackReceived(anyLong());
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void acknowledgementCannotAffectDifferentStream() throws Exception {
    when(frame.getSubject()).thenReturn(acknowledgement.toSubject().replace("$JS.ACK.stream.", "$JS.ACK.other."));
    assertNull(handler.handle(frame, null, session));
    verify(subscription, never()).ackReceived(anyLong());
    verify(subscription, never()).rollbackReceived(anyLong());
  }

  @Test
  void doubleAckResponseUsesRequestInboxAndRegisteredSid() throws Exception {
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(session.getJetStreamRequestManager()).thenReturn(requests);
    when(requests.getSid("_INBOX.confirm")).thenReturn("sid4");
    when(frame.getReplyTo()).thenReturn("_INBOX.confirm");
    when(frame.getPayload()).thenReturn("+ACK".getBytes(StandardCharsets.UTF_8));

    PayloadFrame result = assertInstanceOf(PayloadFrame.class, handler.handle(frame, null, session));
    assertEquals("_INBOX.confirm", result.getSubject());
    assertEquals("sid4", result.getSubscriptionId());
    assertArrayEquals(new byte[0], result.getPayload());
    verify(subscription).ackReceived(42L);
  }

  @Test
  void nakDelayParserRejectsInvalidPayloadAndNegativeDelay() {
    assertNull(TransactionHandler.parseNakDelay("-NAK malformed"));
    assertNull(TransactionHandler.parseNakDelay("-NAK {\"delay\":-1}"));
    assertEquals(0L, TransactionHandler.parseNakDelay("-NAK"));
    assertEquals(1000000000L, TransactionHandler.parseNakDelay("-NAK {\"delay\":1000000000}"));
  }
}
