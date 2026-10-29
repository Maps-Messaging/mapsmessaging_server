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

      verify(fixture.session, times(1)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void incoming_event_uses_configured_namespace_without_accessing_state_subsystems() throws Exception {
    try (Fixture fixture = new Fixture();
        MockedStatic<MessageDaemon> daemon = mockStatic(MessageDaemon.class)) {
      MessageDaemon runningDaemon = mock(MessageDaemon.class);
      daemon.when(MessageDaemon::getInstance).thenReturn(runningDaemon);
      when(fixture.session.findDestination("/configured/cot", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(fixture.destination));
      fixture.protocol.subscribeRemote("remote", "/configured/cot", QualityOfService.AT_MOST_ONCE,
          null, null, null, null);
      byte[] xml = bytes("<event uid='a'></event>");

      fixture.receive(xml);

      ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
      verify(fixture.destination).storeMessage(message.capture());
      assertArrayEquals(xml, message.getValue().getOpaqueData());
      verify(fixture.session).findDestination("/configured/cot", DestinationType.TOPIC);
      verify(fixture.session, times(1)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
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
  void session_creation_resolves_the_destination_before_any_event_arrives() throws Exception {
    try (Fixture fixture = new Fixture()) {
      verify(fixture.session).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void deleted_destination_is_resolved_again_before_storing_the_next_event() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.receive(bytes("<event uid='before-delete'></event>"));
      when(fixture.destination.isClosed()).thenReturn(true);
      Destination recreated = mock(Destination.class);
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(recreated));
      byte[] afterDelete = bytes("<event uid='after-delete'></event>");

      fixture.receive(afterDelete);
      fixture.receive(bytes("<event uid='reused'></event>"));

      verify(fixture.session, times(2)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verify(fixture.destination, times(1)).storeMessage(any(Message.class));
      ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
      verify(recreated, times(2)).storeMessage(messages.capture());
      assertArrayEquals(afterDelete, messages.getAllValues().getFirst().getOpaqueData());
    }
  }

  @Test
  void repeated_events_reuse_the_resolved_destination() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.receive(bytes("<event uid='one'></event>"));
      fixture.receive(bytes("<event uid='two'></event>"));

      verify(fixture.session, times(1)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verify(fixture.destination, times(2)).storeMessage(any(Message.class));
    }
  }

  @Test
  void pending_lookup_is_shared_and_events_publish_in_receive_order() throws Exception {
    CompletableFuture<Destination> pending = new CompletableFuture<>();
    try (Fixture fixture = new Fixture(pending)) {
      byte[] first = bytes("<event uid='one'></event>");
      byte[] second = bytes("<event uid='two'></event>");

      fixture.receive(first);
      fixture.receive(second);
      verify(fixture.session, times(1)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verifyNoInteractions(fixture.destination);
      pending.complete(fixture.destination);

      ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
      verify(fixture.destination, times(2)).storeMessage(messages.capture());
      assertArrayEquals(first, messages.getAllValues().get(0).getOpaqueData());
      assertArrayEquals(second, messages.getAllValues().get(1).getOpaqueData());
    }
  }

  @Test
  void failed_lookup_is_retried_for_the_next_event() throws Exception {
    CompletableFuture<Destination> pending = new CompletableFuture<>();
    try (Fixture fixture = new Fixture(pending)) {
      fixture.receive(bytes("<event uid='failed'></event>"));
      pending.completeExceptionally(new IOException("lookup failed"));
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(fixture.destination));
      fixture.receive(bytes("<event uid='retry'></event>"));
      fixture.receive(bytes("<event uid='cached'></event>"));

      verify(fixture.session, times(2)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verify(fixture.destination, times(2)).storeMessage(any(Message.class));
    }
  }

  @Test
  void null_lookup_result_is_retried_for_the_next_event() throws Exception {
    CompletableFuture<Destination> pending = new CompletableFuture<>();
    try (Fixture fixture = new Fixture(pending)) {
      fixture.receive(bytes("<event uid='missing'></event>"));
      pending.complete(null);
      when(fixture.session.findDestination("/tak/cot/inbound", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(fixture.destination));
      fixture.receive(bytes("<event uid='retry'></event>"));
      fixture.receive(bytes("<event uid='cached'></event>"));

      verify(fixture.session, times(2)).findDestination("/tak/cot/inbound", DestinationType.TOPIC);
      verify(fixture.destination, times(2)).storeMessage(any(Message.class));
    }
  }

  @Test
  void namespace_change_resolves_the_new_destination_without_redirecting_pending_events() throws Exception {
    CompletableFuture<Destination> pending = new CompletableFuture<>();
    try (Fixture fixture = new Fixture(pending)) {
      Destination replacement = mock(Destination.class);
      when(fixture.session.findDestination("/replacement/cot", DestinationType.TOPIC))
          .thenReturn(CompletableFuture.completedFuture(replacement));
      byte[] original = bytes("<event uid='original'></event>");
      fixture.receive(original);

      fixture.protocol.subscribeRemote("remote", "/replacement/cot", QualityOfService.AT_MOST_ONCE,
          null, null, null, null);
      fixture.receive(bytes("<event uid='replacement'></event>"));
      fixture.receive(bytes("<event uid='cached'></event>"));
      pending.complete(fixture.destination);

      ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
      verify(fixture.destination).storeMessage(message.capture());
      assertArrayEquals(original, message.getValue().getOpaqueData());
      verify(replacement, times(2)).storeMessage(any(Message.class));
      verify(fixture.session, times(1)).findDestination("/replacement/cot", DestinationType.TOPIC);
    }
  }

  @Test
  void archiveLookupFailureIsContained() throws Exception {
    CompletableFuture<Destination> failed = CompletableFuture.failedFuture(new IOException("lookup failed"));
    try (Fixture fixture = new Fixture(failed)) {
      assertDoesNotThrow(() -> fixture.receive(bytes("<event></event>")));

      verifyNoInteractions(fixture.destination);
    }
  }

  @Test
  void nullArchiveDestinationIsIgnored() throws Exception {
    try (Fixture fixture = new Fixture(CompletableFuture.completedFuture(null))) {
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
      this(null);
    }

    private Fixture(CompletableFuture<Destination> initialLookup) throws Exception {
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
          .thenReturn(initialLookup != null ? initialLookup : CompletableFuture.completedFuture(destination));
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
