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

package io.mapsmessaging.network.protocol.impl.nats;

import com.google.gson.JsonParser;
import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.nats.frames.*;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamArchiveManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.TransactionSubject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.StreamUpdateHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler.MessageHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler.TransactionHandler;
import io.mapsmessaging.network.protocol.impl.nats.listener.PubListener;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NatsDispatchRefactorTest {
  private final NamedConsumer consumer = mock(NamedConsumer.class);
  private final SessionState state = mock(SessionState.class);
  private final MessageEvent event = mock(MessageEvent.class);
  private final Message message = mock(Message.class);
  private final TransactionSubject acknowledgement = new TransactionSubject("EVENTS", "reader", 1, 42, 1, 100, 0);
  private final MsgFrame outbound = new MsgFrame(128);

  private void prepareDelivery() throws IOException {
    when(consumer.getStreamName()).thenReturn("EVENTS");
    when(event.getDestinationName()).thenReturn("EVENTS/orders");
    when(event.getMessage()).thenReturn(message);
    when(message.getIdentifier()).thenReturn(42L);
    when(consumer.getNextMessage(true)).thenReturn(event);
    when(consumer.recordDelivery("EVENTS/orders", message)).thenReturn(acknowledgement);
    when(state.buildPayloadFrame(message, "orders")).thenReturn(outbound);
    when(state.getJetStreamRequestManager()).thenReturn(mock(JetStreamRequestManager.class));
  }

  @Test
  void max_delivery_skip_consumes_batch_slot_and_continues() throws Exception {
    prepareDelivery();
    when(consumer.terminateIfMaxDelivered("EVENTS/orders", 42L)).thenReturn(true, false);
    when(state.send(outbound)).thenReturn(true);
    assertEquals(1, new MessageHandler().deliverNext(consumer, "_INBOX.pull", state, 2, true));
    verify(consumer, times(2)).getNextMessage(true);
    verify(consumer, times(1)).recordDelivery("EVENTS/orders", message);
  }

  @Test
  void unsent_delivery_rolls_back_and_stops_batch() throws Exception {
    prepareDelivery();
    assertEquals(0, new MessageHandler().deliverNext(consumer, "_INBOX.pull", state, 3, true));
    verify(consumer).rollbackUnsent(acknowledgement);
    verify(consumer, times(1)).getNextMessage(true);
  }

  @Test
  void send_exception_rolls_back_and_preserves_failure() throws Exception {
    prepareDelivery();
    RuntimeException failure = new IllegalStateException("writer failed");
    when(state.send(outbound)).thenThrow(failure);
    assertSame(failure, assertThrows(RuntimeException.class,
        () -> new MessageHandler().deliverNext(consumer, "_INBOX.pull", state, 3, true)));
    verify(consumer).rollbackUnsent(acknowledgement);
  }

  @Test
  void failed_registration_rolls_back_before_any_send() throws Exception {
    prepareDelivery();
    when(consumer.recordDelivery("EVENTS/orders", message)).thenReturn(null);
    IOException failure = assertThrows(IOException.class,
        () -> new MessageHandler().deliverNext(consumer, "_INBOX.pull", state, 1, true));
    assertEquals("Unable to register pending JetStream delivery", failure.getMessage());
    verify(consumer).rollbackUnregistered("EVENTS/orders", 42L);
    verify(state, never()).send(any());
  }

  private SessionState realState() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    when(protocol.getEndPoint()).thenReturn(mock(EndPoint.class));
    return new SessionState(protocol);
  }

  @Test
  void consumer_failure_does_not_prevent_remaining_cleanup_or_session_close() throws Exception {
    SessionState connection = realState();
    NamedConsumer other = mock(NamedConsumer.class);
    connection.getNamedConsumers().put("first", consumer);
    connection.getNamedConsumers().put("second", other);
    RuntimeException failure = new IllegalStateException("consumer failed");
    doThrow(failure).when(consumer).close();
    Session session = mock(Session.class);
    connection.setSession(session);
    SessionManager manager = mock(SessionManager.class);
    when(manager.closeAsync(session, false)).thenReturn(CompletableFuture.completedFuture(session));
    try (var access = mockStatic(SessionManager.class)) {
      access.when(SessionManager::getInstance).thenReturn(manager);
      IOException problem = assertThrows(IOException.class, connection::close);
      assertSame(failure, problem.getCause());
      verify(other).close();
      verify(manager).closeAsync(session, false);
      assertTrue(connection.getNamedConsumers().isEmpty());
      assertFalse(connection.send(new MsgFrame(32)));
    }
  }

  @Test
  void interrupted_session_close_preserves_interrupt_and_error() throws Exception {
    SessionState connection = realState();
    Session session = mock(Session.class);
    connection.setSession(session);
    SessionManager manager = mock(SessionManager.class);
    CompletableFuture<Session> closing = mock(CompletableFuture.class);
    when(closing.get()).thenThrow(new InterruptedException("interrupted"));
    when(manager.closeAsync(session, false)).thenReturn(closing);
    try (var access = mockStatic(SessionManager.class)) {
      access.when(SessionManager::getInstance).thenReturn(manager);
      IOException problem = assertThrows(IOException.class, connection::close);
      assertEquals("Interrupted closing NATS session", problem.getMessage());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  private Session preparePublish() {
    NatsProtocol protocol = mock(NatsProtocol.class);
    NatsConfigDTO config = new NatsConfigDTO();
    config.setEnableStreams(false);
    when(state.getProtocol()).thenReturn(protocol);
    when(protocol.getNatsConfig()).thenReturn(config);
    when(state.getMapping("orders")).thenReturn("orders");
    when(state.getJetStreamRequestManager()).thenReturn(mock(JetStreamRequestManager.class));
    Session session = mock(Session.class);
    when(state.getSession()).thenReturn(session);
    return session;
  }

  private MsgFrame publish() {
    MsgFrame frame = new MsgFrame(128);
    frame.setSubject("orders");
    return frame;
  }

  @Test
  void failed_destination_future_propagates_original_io_exception_without_error_frame() {
    Session session = preparePublish();
    IOException failure = new IOException("lookup failed");
    when(session.findDestination("orders", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.failedFuture(failure));
    assertSame(failure, assertThrows(IOException.class, () -> new PubListener().frameEvent(publish(), state, true)));
    verify(state, never()).send(any());
  }

  @Test
  void interrupted_publish_future_preserves_interrupt() throws Exception {
    Session session = preparePublish();
    CompletableFuture<Destination> pending = mock(CompletableFuture.class);
    when(pending.get()).thenThrow(new InterruptedException("interrupted"));
    when(session.findDestination("orders", DestinationType.TOPIC)).thenReturn(pending);
    try {
      IOException problem = assertThrows(IOException.class, () -> new PubListener().frameEvent(publish(), state, true));
      assertEquals("Interrupted while resolving NATS destination", problem.getMessage());
      assertTrue(Thread.currentThread().isInterrupted());
      verify(state, never()).send(any());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void next_with_invalid_batch_does_not_acknowledge_current_delivery() throws Exception {
    when(state.getNamedConsumers()).thenReturn(Map.of("reader", consumer));
    when(consumer.getStreamName()).thenReturn("EVENTS");
    MsgFrame request = publish();
    request.setSubject(acknowledgement.toSubject());
    request.setPayload("+NXT 0".getBytes(StandardCharsets.UTF_8));
    assertNull(new TransactionHandler().handle(request, null, state));
    verify(consumer, never()).acknowledge(any(), anyBoolean());
  }

  @Test
  void next_acknowledges_before_fetching_with_batch_and_no_wait() throws Exception {
    when(state.getNamedConsumers()).thenReturn(Map.of("reader", consumer));
    when(consumer.getStreamName()).thenReturn("EVENTS");
    when(consumer.acknowledge(any(TransactionSubject.class), eq(false))).thenReturn(true);
    MsgFrame request = publish();
    request.setSubject(acknowledgement.toSubject());
    request.setReplyTo("_INBOX.pull");
    request.setPayload("+NXT {\"batch\":2,\"no_wait\":true}".getBytes(StandardCharsets.UTF_8));
    assertNull(new TransactionHandler().handle(request, null, state));
    var order = inOrder(consumer);
    order.verify(consumer).acknowledge(argThat(subject -> subject.toSubject().equals(acknowledgement.toSubject())), eq(false));
    order.verify(consumer).getNextMessage(true);
  }

  @Test
  void update_starts_additions_before_resolving_removals_and_checks_removal_before_waiting() throws Exception {
    Session session = preparePublish();
    NatsConfigDTO config = state.getProtocol().getNatsConfig();
    config.setEnableStreamDelete(true);
    StreamInfoList stream = new StreamInfoList("EVENTS");
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/old");
    stream.addSubject("old", resource);
    NamespaceManager namespaces = mock(NamespaceManager.class);
    when(namespaces.getStream("EVENTS")).thenReturn(stream);
    Destination removed = mock(Destination.class);
    CompletableFuture<Destination> removal = mock(CompletableFuture.class);
    when(removal.get(20, TimeUnit.SECONDS)).thenReturn(removed);
    when(session.findDestination("EVENTS/old", DestinationType.TOPIC)).thenReturn(removal);
    CompletableFuture<Destination> addition = new CompletableFuture<>();
    when(session.findDestination("EVENTS/new", DestinationType.TOPIC)).thenReturn(addition);
    JetStreamArchiveManager archives = mock(JetStreamArchiveManager.class);
    try (var namespaceAccess = mockStatic(NamespaceManager.class);
         var archiveAccess = mockStatic(JetStreamArchiveManager.class);
         var membership = mockStatic(NatsJetStreamDestinations.class)) {
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      archiveAccess.when(JetStreamArchiveManager::getInstance).thenReturn(archives);
      membership.when(() -> NatsJetStreamDestinations.checkRemoval(removed))
          .thenThrow(new IOException("in use"));
      MsgFrame request = publish();
      request.setSubject("$JS.API.STREAM.UPDATE.EVENTS");
      request.setReplyTo("_INBOX.update");
      PayloadFrame response = assertInstanceOf(PayloadFrame.class, new StreamUpdateHandler().handle(request,
          JsonParser.parseString("{\"subjects\":[\"new\"]}").getAsJsonObject(), state));
      String body = new String(response.getPayload(), StandardCharsets.UTF_8);
      assertTrue(body.contains("Unable to remove stream subjects: in use"), body);
      var order = inOrder(session, removal);
      order.verify(session).findDestination("EVENTS/new", DestinationType.TOPIC);
      order.verify(session).findDestination("EVENTS/old", DestinationType.TOPIC);
      order.verify(removal).get(20, TimeUnit.SECONDS);
      verifyNoInteractions(archives);
      assertFalse(addition.isDone());
    }
  }
}
