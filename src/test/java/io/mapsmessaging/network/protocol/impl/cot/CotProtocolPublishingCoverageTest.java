/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.SubSystemManager;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.state.StateManagerAgent;
import io.mapsmessaging.state.adapter.cot.CotIngestAdapter;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.tak.CotToTwinMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CotProtocolPublishingCoverageTest {

  @Test
  void oversizedIncompleteDocumentIsDiscarded() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.buffer.write(bytes("<event" + "x".repeat(1_048_576)));

      invoke(fixture.protocol, "processBuffer", new Class<?>[0]);

      assertEquals(0, fixture.buffer.size());
      verify(fixture.logger).warn(anyString(), eq(1_048_576));
      verifyNoInteractions(fixture.adapter);
      verifyNoInteractions(fixture.mapper);
    }
  }

  @Test
  void adapterSuccessSkipsProtocolSessionArchive() throws Exception {
    try (Fixture fixture = new Fixture()) {
      byte[] xml = bytes("<event uid='a'></event>");

      invoke(fixture.protocol, "publish", new Class<?>[]{byte[].class}, xml);

      verify(fixture.adapter).publishLocal(same(xml));
      verify(fixture.session, never()).findDestination(anyString(), any(DestinationType.class));
      verify(fixture.mapper).routeToTwinManager(fixture.twinManager, xml, "cot-ingest");
    }
  }

  @Test
  void adapterFalseFallsBackToProtocolSessionArchive() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.adapter.publishLocal(any(byte[].class))).thenReturn(false);
      Destination destination = fixture.archiveDestination();
      byte[] xml = bytes("<event uid='fallback'></event>");

      invoke(fixture.protocol, "publish", new Class<?>[]{byte[].class}, xml);

      ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
      verify(destination).storeMessage(message.capture());
      assertArrayEquals(xml, message.getValue().getOpaqueData());
      assertEquals("text/xml", message.getValue().getContentType());
      assertEquals(QualityOfService.AT_MOST_ONCE, message.getValue().getQualityOfService());
      assertFalse(message.getValue().isRetain());
    }
  }

  @Test
  void missingAdapterFallsBackToProtocolSessionArchive() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.stateManager.getStateMessageAdapter(CotIngestAdapter.class))
          .thenReturn(Optional.empty());
      Destination destination = fixture.archiveDestination();

      invoke(fixture.protocol, "publish",
          new Class<?>[]{byte[].class}, bytes("<event uid='missing-adapter'></event>"));

      verify(destination).storeMessage(any(Message.class));
    }
  }

  @Test
  void adapterRuntimeFailureFallsBackToProtocolSessionArchive() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.adapter.publishLocal(any(byte[].class)))
          .thenThrow(new IllegalStateException("adapter unavailable"));
      Destination destination = fixture.archiveDestination();

      invoke(fixture.protocol, "publish",
          new Class<?>[]{byte[].class}, bytes("<event uid='adapter-failure'></event>"));

      verify(destination).storeMessage(any(Message.class));
    }
  }

  @Test
  void archiveLookupFailureIsContained() throws Exception {
    try (Fixture fixture = new Fixture()) {
      CompletableFuture<Destination> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IOException("lookup failed"));
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(failed);

      assertDoesNotThrow(() -> invoke(
          fixture.protocol,
          "publishArchiveWithProtocolSession",
          new Class<?>[]{byte[].class},
          bytes("<event></event>")));
    }
  }

  @Test
  void nullArchiveDestinationIsIgnored() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(null));

      assertDoesNotThrow(() -> invoke(
          fixture.protocol,
          "publishArchiveWithProtocolSession",
          new Class<?>[]{byte[].class},
          bytes("<event></event>")));
    }
  }

  @Test
  void archiveStoreFailureIsContained() throws Exception {
    try (Fixture fixture = new Fixture()) {
      Destination destination = fixture.archiveDestination();
      doThrow(new IOException("store failed")).when(destination).storeMessage(any(Message.class));

      assertDoesNotThrow(() -> invoke(
          fixture.protocol,
          "publishArchiveWithProtocolSession",
          new Class<?>[]{byte[].class},
          bytes("<event></event>")));

      verify(destination).storeMessage(any(Message.class));
    }
  }

  @Test
  void missingTwinManagerSkipsMapper() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.stateManager.getTwinManager()).thenReturn(null);

      invoke(fixture.protocol, "routeToTwinManager",
          new Class<?>[]{byte[].class}, bytes("<event uid='a'></event>"));

      verifyNoInteractions(fixture.mapper);
    }
  }

  @Test
  void mapperRejectionIsContained() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.mapper.routeToTwinManager(
          eq(fixture.twinManager), any(byte[].class), eq("cot-ingest"))).thenReturn(false);

      invoke(fixture.protocol, "routeToTwinManager",
          new Class<?>[]{byte[].class}, bytes("<event></event>"));

      verify(fixture.mapper).routeToTwinManager(
          eq(fixture.twinManager), any(byte[].class), eq("cot-ingest"));
    }
  }

  @Test
  void twinManagerResolutionFailureReturnsNull() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.daemon.getSubSystemManager()).thenThrow(new IllegalStateException("not ready"));

      TwinManager result = invoke(
          fixture.protocol, "resolveTwinManager", new Class<?>[0]);

      assertNull(result);
    }
  }

  @Test
  void nullSessionHasNoSubject() throws Exception {
    try (Fixture fixture = new Fixture()) {
      setField(fixture.protocol, CotProtocol.class, "session", null);

      assertNull(fixture.protocol.getSubject());
    }
  }

  @Test
  void selectedClosedEndpointReturnsWithoutSchedulingRead() throws Exception {
    try (Fixture fixture = new Fixture()) {
      doThrow(new ClosedChannelException()).when(fixture.endpoint).deregister(anyInt());

      fixture.protocol.selected(mock(Selectable.class), null, 0);

      verify(fixture.endpoint).deregister(SelectionKey.OP_READ);
      verify(fixture.endpoint, never()).readPacket(any());
    }
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  @SuppressWarnings("unchecked")
  private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = CotProtocol.class.getDeclaredMethod(name, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(target, args);
  }

  private static void setField(Object target, Class<?> owner, String name, Object value)
      throws Exception {
    Field field = owner.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static final class Fixture implements AutoCloseable {
    private final MessageDaemon daemon = mock(MessageDaemon.class);
    private final StateManagerAgent stateManager = mock(StateManagerAgent.class);
    private final CotIngestAdapter adapter = mock(CotIngestAdapter.class);
    private final TwinManager twinManager = mock(TwinManager.class);
    private final CotToTwinMapper mapper = mock(CotToTwinMapper.class);
    private final Session session = mock(Session.class);
    private final EndPoint endpoint = mock(EndPoint.class);
    private final Logger logger = mock(Logger.class);
    private final CotProtocol protocol = mock(CotProtocol.class, CALLS_REAL_METHODS);
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final MockedStatic<MessageDaemon> daemonStatic;

    private Fixture() throws Exception {
      SubSystemManager subSystemManager = mock(SubSystemManager.class);
      when(daemon.getSubSystemManager()).thenReturn(subSystemManager);
      when(subSystemManager.getStateManager()).thenReturn(stateManager);
      when(stateManager.getStateMessageAdapter(CotIngestAdapter.class))
          .thenReturn(Optional.of(adapter));
      when(stateManager.getTwinManager()).thenReturn(twinManager);
      when(adapter.publishLocal(any(byte[].class))).thenReturn(true);
      when(mapper.routeToTwinManager(
          eq(twinManager), any(byte[].class), eq("cot-ingest"))).thenReturn(true);

      daemonStatic = mockStatic(MessageDaemon.class);
      daemonStatic.when(MessageDaemon::getInstance).thenReturn(daemon);

      setField(protocol, CotProtocol.class, "buffer", buffer);
      setField(protocol, CotProtocol.class, "cotToTwinMapper", mapper);
      setField(protocol, CotProtocol.class, "session", session);
      setField(protocol, CotProtocol.class, "logger", logger);
      setField(protocol, Protocol.class, "endPoint", endpoint);
    }

    private Destination archiveDestination() {
      Destination destination = mock(Destination.class);
      when(session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(destination));
      return destination;
    }

    @Override
    public void close() {
      daemonStatic.close();
    }
  }
}
