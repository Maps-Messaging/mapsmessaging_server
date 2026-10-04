/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.protocol.impl.local;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.Logger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalLoopProtocolCoreCoverageTest {

  @Test
  void findAndSendStoresMessageAndRunsCompletion() throws Exception {
    Harness h = harness();
    CountDownLatch completed = new CountDownLatch(1);
    Message message = new MessageBuilder().setOpaqueData("data".getBytes()).build();
    MessageEvent event = mock(MessageEvent.class);
    when(event.getCompletionTask()).thenReturn(completed::countDown);

    invokeFindAndSend(h.protocol, "/loop/topic", message, event);

    assertTrue(completed.await(2, TimeUnit.SECONDS));
    verify(h.destination).storeMessage(message);
  }

  @Test
  void nullDestinationStillCompletesEvent() throws Exception {
    Harness h = harness();
    when(h.session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(null));
    CountDownLatch completed = new CountDownLatch(1);
    MessageEvent event = mock(MessageEvent.class);
    when(event.getCompletionTask()).thenReturn(completed::countDown);

    invokeFindAndSend(
        h.protocol,
        "/loop/missing",
        new MessageBuilder().setOpaqueData(new byte[]{1}).build(),
        event);

    assertTrue(completed.await(2, TimeUnit.SECONDS));
  }

  @Test
  void failedDestinationLookupStillCompletesEvent() throws Exception {
    Harness h = harness();
    CompletableFuture<Destination> failed = new CompletableFuture<>();
    failed.completeExceptionally(new IOExceptionForTest());
    when(h.session.findDestination(anyString(), eq(DestinationType.TOPIC))).thenReturn(failed);
    CountDownLatch completed = new CountDownLatch(1);
    MessageEvent event = mock(MessageEvent.class);
    when(event.getCompletionTask()).thenReturn(completed::countDown);

    invokeFindAndSend(
        h.protocol,
        "/loop/failure",
        new MessageBuilder().setOpaqueData(new byte[]{1}).build(),
        event);

    assertTrue(completed.await(2, TimeUnit.SECONDS));
    verify(h.destination, never()).storeMessage(any());
  }

  @Test
  void storeFailureStillCompletesEvent() throws Exception {
    Harness h = harness();
    doThrow(new java.io.IOException("store failed")).when(h.destination).storeMessage(any());
    CountDownLatch completed = new CountDownLatch(1);
    MessageEvent event = mock(MessageEvent.class);
    when(event.getCompletionTask()).thenReturn(completed::countDown);

    invokeFindAndSend(
        h.protocol,
        "/loop/store-failure",
        new MessageBuilder().setOpaqueData(new byte[]{1}).build(),
        event);

    assertTrue(completed.await(2, TimeUnit.SECONDS));
  }

  @Test
  void processPacketIsAlwaysFalseForLoopbackTransport() throws Exception {
    Harness h = harness();

    assertFalse(h.protocol.processPacket(mock(io.mapsmessaging.network.io.Packet.class)));
  }

  @Test
  void protocolIdentityAndSessionIdAreStable() throws Exception {
    Harness h = harness();

    assertEquals("LocalLoop", h.protocol.getName());
    assertEquals("1.0", h.protocol.getVersion());
    assertEquals("loop-session", h.protocol.getSessionId());
  }

  @Test
  void subjectComesFromLoopbackSessionSecurityContext() throws Exception {
    Harness h = harness();
    var security = mock(io.mapsmessaging.security.access.SecurityContext.class);
    var subject = new javax.security.auth.Subject();
    when(h.session.getSecurityContext()).thenReturn(security);
    when(security.getSubject()).thenReturn(subject);

    assertSame(subject, h.protocol.getSubject());
  }

  private static Harness harness() throws Exception {
    LocalLoopProtocol protocol = mock(LocalLoopProtocol.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);

    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(destination));

    set(protocol, "session", session);
    set(protocol, "logger", mock(Logger.class));
    set(protocol, "closed", false);
    set(protocol, "sessionId", "loop-session");

    return new Harness(protocol, session, destination);
  }

  private static void invokeFindAndSend(
      LocalLoopProtocol protocol,
      String topic,
      Message message,
      MessageEvent event) throws Exception {
    Method method = LocalLoopProtocol.class.getDeclaredMethod(
        "findAndSendMessage", String.class, Message.class, MessageEvent.class);
    method.setAccessible(true);
    method.invoke(protocol, topic, message, event);
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = LocalLoopProtocol.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private record Harness(LocalLoopProtocol protocol, Session session, Destination destination) {
  }

  private static final class IOExceptionForTest extends RuntimeException {
  }
}
