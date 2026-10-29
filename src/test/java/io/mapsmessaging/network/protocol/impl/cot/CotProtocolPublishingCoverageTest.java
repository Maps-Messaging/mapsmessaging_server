/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.network.protocol.impl.cot;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.CotProtocolConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CotProtocolPublishingCoverageTest {

  @Test
  void oversizedIncompleteDocumentIsRejectedBeforePublishing() throws Exception {
    try (Fixture fixture = new Fixture()) {
      byte[] data = bytes("<event>" + "x".repeat(1_048_576));

      assertThrows(IOException.class, () -> fixture.receive(data));

      verify(fixture.session, never()).findDestination(anyString(), any(DestinationType.class));
      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void incoming_event_uses_configured_namespace_without_accessing_state_subsystems() throws Exception {
    try (Fixture fixture = new Fixture();
        MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
      MessageDaemon runningDaemon = mock(MessageDaemon.class);
      daemon.when(MessageDaemon::getInstance).thenReturn(runningDaemon);
      fixture.protocol.subscribeRemote("remote", "/configured/cot", QualityOfService.AT_MOST_ONCE,
          null, null, null, null);
      when(fixture.session.findDestination("/configured/cot", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(fixture.destination));
      byte[] xml = bytes("<event uid='a'></event>");

      fixture.receive(xml);

      ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
      verify(fixture.destination).storeMessage(message.capture());
      assertArrayEquals(xml, message.getValue().getOpaqueData());
      verify(fixture.session).findDestination("/configured/cot", DestinationType.TOPIC);
      verify(fixture.session, never()).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verify(runningDaemon, never()).getSubSystemManager();
    }
  }

  @Test
  void unbound_listener_publishes_original_xml_with_existing_message_attributes() throws Exception {
    try (Fixture fixture = new Fixture()) {
      byte[] xml = bytes("<event uid='listener'><detail><remarks>café</remarks></detail></event>");

      fixture.receive(xml);

      ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
      verify(fixture.destination).storeMessage(message.capture());
      assertArrayEquals(xml, message.getValue().getOpaqueData());
      assertEquals("text/xml", message.getValue().getContentType());
      assertEquals(QualityOfService.AT_MOST_ONCE, message.getValue().getQualityOfService());
      assertFalse(message.getValue().isRetain());
      verify(fixture.session).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
    }
  }

  @Test
  void archiveLookupFailureIsContained() throws Exception {
    try (Fixture fixture = new Fixture()) {
      CompletableFuture<Destination> failed = new CompletableFuture<>();
      failed.completeExceptionally(new IOException("lookup failed"));
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC)).thenReturn(failed);

      assertDoesNotThrow(() -> fixture.receive(bytes("<event></event>")));

      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void nullArchiveDestinationIsIgnored() throws Exception {
    try (Fixture fixture = new Fixture()) {
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(null));

      assertDoesNotThrow(() -> fixture.receive(bytes("<event></event>")));

      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void archiveStoreFailureIsContained() throws Exception {
    try (Fixture fixture = new Fixture()) {
      doThrow(new IOException("store failed")).when(fixture.destination).storeMessage(any(Message.class));

      assertDoesNotThrow(() -> fixture.receive(bytes("<event></event>")));

      verify(fixture.destination).storeMessage(any(Message.class));
    }
  }

  @Test
  void nullSessionHasNoSubject() throws Exception {
    try (Fixture fixture = new Fixture()) {
      Field sessionField = CotProtocol.class.getDeclaredField("session");
      sessionField.setAccessible(true);
      sessionField.set(fixture.protocol, null);

      assertNull(fixture.protocol.getSubject());
    }
  }

  @Test
  void close_stops_the_shared_selector_and_internal_session_once() throws Exception {
    try (Fixture fixture = new Fixture()) {
      ArgumentCaptor<Selectable> selector = ArgumentCaptor.forClass(Selectable.class);
      verify(fixture.endpoint).register(eq(SelectionKey.OP_READ), selector.capture());

      fixture.protocol.close();
      fixture.protocol.close();
      selector.getValue().selected(null, null, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

      verify(fixture.manager).close(fixture.session, false);
      verify(fixture.endpoint).close();
      verify(fixture.endpoint, never()).readPacket(any());
      verify(fixture.endpoint, never()).sendPacket(any());
    }
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private static final class Fixture implements AutoCloseable {
    private final boolean originalJmx = JMXManager.isEnableJMX();
    private final SessionManager manager = mock(SessionManager.class);
    private final Session session = mock(Session.class);
    private final EndPoint endpoint = mock(EndPoint.class);
    private final Destination destination = mock(Destination.class);
    private final MockedStatic<SessionManager> managerStatic;
    private final CotProtocol protocol;

    private Fixture() throws Exception {
      JMXManager.setEnableJMX(false);
      EndPointServerConfigDTO config = mock(EndPointServerConfigDTO.class);
      when(endpoint.getConfig()).thenReturn(config);
      when(config.getEndPointConfig()).thenReturn(new EndPointConfigDTO("tcp"));
      when(config.getUrl()).thenReturn("tcp://localhost:8087");
      when(endpoint.getJMXTypePath()).thenReturn(List.of());
      when(endpoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
      when(manager.create(any(), any())).thenReturn(session);
      managerStatic = mockStatic(SessionManager.class);
      managerStatic.when(SessionManager::getInstance).thenReturn(manager);
      when(session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(destination));
      protocol = new CotProtocol(endpoint, new Packet(0, false), new CotProtocolConfigDTO());
    }

    private void receive(byte[] xml) throws IOException {
      Packet packet = new Packet(xml.length, false);
      packet.put(xml);
      packet.flip();
      protocol.processPacket(packet);
    }

    @Override
    public void close() throws Exception {
      try {
        protocol.close();
      } finally {
        managerStatic.close();
        JMXManager.setEnableJMX(originalJmx);
      }
    }
  }
}
