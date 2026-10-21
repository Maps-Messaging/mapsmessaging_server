/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */

package io.mapsmessaging.state.adapter.cot;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.state.drone.core.TwinManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CotIngestAdapterBranchCoverageTest {

  @ParameterizedTest
  @MethodSource("edgeNameCases")
  void edgeNameExtractionHandlesTopicShapes(String destinationName, String expected) throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));

    assertEquals(expected, edgeName(adapter, destinationName));
  }

  @Test
  void publishLocalRejectsMissingSession() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));

    assertFalse(adapter.publishLocal(cot("asset")));
  }

  @Test
  void publishLocalRejectsNullPayloadWithSession() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    setSession(adapter, mock(Session.class));

    assertFalse(adapter.publishLocal(null));
  }

  @Test
  void publishLocalRejectsEmptyPayloadWithSession() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    setSession(adapter, mock(Session.class));

    assertFalse(adapter.publishLocal(new byte[0]));
  }

  @Test
  void publishLocalStoresArchiveMessageWhenDestinationResolves() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    when(session.getName()).thenReturn("cot-session");
    when(session.findDestination(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    setSession(adapter, session);

    assertTrue(adapter.publishLocal(cot("asset")));

    verify(destination).storeMessage(any(Message.class));
  }

  @Test
  void publishLocalTreatsMissingDestinationAsHandled() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("cot-session");
    when(session.findDestination(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));
    setSession(adapter, session);

    assertTrue(adapter.publishLocal(cot("asset")));
  }

  @Test
  void publishLocalTreatsLookupFailureAsHandled() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("cot-session");
    when(session.findDestination(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("lookup")));
    setSession(adapter, session);

    assertTrue(adapter.publishLocal(cot("asset")));
  }

  @Test
  void publishLocalSwallowsArchiveStoreFailure() throws Exception {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    when(session.getName()).thenReturn("cot-session");
    when(session.findDestination(CotIngestAdapter.LOCAL_ARCHIVE_TOPIC, DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    doThrow(new java.io.IOException("store")).when(destination).storeMessage(any(Message.class));
    setSession(adapter, session);

    assertTrue(adapter.publishLocal(cot("asset")));
  }

  @Test
  void sendMessageWithNullPayloadStillCompletes() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getCompletionTask()).thenReturn(completion);
    when(message.getOpaqueData()).thenReturn(null);

    adapter.sendMessage(event);

    verify(completion).run();
    assertEquals(0L, adapter.getRoutedCount());
    assertEquals(0L, adapter.getDroppedCount());
  }

  @Test
  void sendMessageExceptionStillCompletes() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    MessageEvent event = mock(MessageEvent.class);
    Runnable completion = mock(Runnable.class);
    when(event.getMessage()).thenThrow(new IllegalStateException("boom"));
    when(event.getCompletionTask()).thenReturn(completion);

    assertDoesNotThrow(() -> adapter.sendMessage(event));

    verify(completion).run();
  }

  @Test
  void sendMessageWithoutCompletionDoesNotThrow() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", mock(TwinManager.class));
    MessageEvent event = mock(MessageEvent.class);
    Message message = mock(Message.class);
    when(event.getMessage()).thenReturn(message);
    when(message.getOpaqueData()).thenReturn(null);

    assertDoesNotThrow(() -> adapter.sendMessage(event));
  }

  @Test
  void handlingMessageSetsLastMessageAge() {
    CotIngestAdapter adapter =
        new CotIngestAdapter("/tak/cot/inbound/#", new TwinManager());

    adapter.handle("/tak/cot/inbound/edge-a", cot("asset"));

    assertTrue(adapter.getLastMessageAgeMillis() >= 0L);
  }

  private static Stream<Arguments> edgeNameCases() {
    return Stream.of(
        Arguments.of(null, "unknown"),
        Arguments.of("", "unknown"),
        Arguments.of(" ", "unknown"),
        Arguments.of("/", "unknown"),
        Arguments.of("/tak/cot/inbound/", "unknown"),
        Arguments.of("edge-a", "edge-a"),
        Arguments.of("/edge-a", "edge-a"),
        Arguments.of("/tak/cot/inbound/edge-a", "edge-a"),
        Arguments.of("a/b/c", "c"),
        Arguments.of("a//b", "b"),
        Arguments.of("trailing/", "unknown"),
        Arguments.of(" leading", " leading")
    );
  }

  private String edgeName(CotIngestAdapter adapter, String destinationName) throws Exception {
    Method method = CotIngestAdapter.class.getDeclaredMethod("edgeNameFrom", String.class);
    method.setAccessible(true);
    return (String) method.invoke(adapter, destinationName);
  }

  private void setSession(CotIngestAdapter adapter, Session session) throws Exception {
    Field field = CotIngestAdapter.class.getDeclaredField("session");
    field.setAccessible(true);
    field.set(adapter, session);
  }

  private byte[] cot(String uid) {
    return ("""
        <event uid="%s" type="a-f-A-M-F-Q">
          <point lat="38.0" lon="-9.0" hae="10"/>
        </event>
        """.formatted(uid)).getBytes(StandardCharsets.UTF_8);
  }
}
