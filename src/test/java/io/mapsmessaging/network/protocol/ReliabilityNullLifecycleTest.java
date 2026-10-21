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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.io.SatelliteEndPoint;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.listeners.LinkLocalCloseEventListener;
import io.mapsmessaging.network.protocol.impl.amqp.proton.tasks.SendMessageTask;
import io.mapsmessaging.network.protocol.impl.mavlink.MavlinkProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.protocol.SatelliteGatewayProtocol;
import io.mapsmessaging.network.protocol.impl.semtech.GatewayInfo;
import io.mapsmessaging.network.protocol.impl.semtech.SemTechProtocol;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Transport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ReliabilityNullLifecycleTest {

  @Test
  void filtered_outbound_message_completes_without_running_analyser_on_null() throws Exception {
    MQTTProtocol protocol = mock(MQTTProtocol.class, CALLS_REAL_METHODS);
    setField(protocol, "packetIdManager", new PacketIdManager());
    var transformer = mock(io.mapsmessaging.api.transformers.InterServerTransformation.class);
    setField(protocol, "destinationTransformerMap", java.util.Map.of("/sensor", transformer));
    setField(protocol, "topicNameMapping", new ConcurrentHashMap<>());
    setField(protocol, "topicNameAnalyserMap", new ConcurrentHashMap<>());
    setField(protocol, "resourceNameAnalyserMap", new ConcurrentHashMap<>());
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(manager.getContext()).thenReturn(new SubscriptionContext("/sensor"));
    AtomicInteger completions = new AtomicInteger();

    assertDoesNotThrow(() -> protocol.sendMessage(event(manager, completions)));

    assertEquals(1, completions.get());
    verify(protocol, never()).writeFrame(any());
  }

  @Test
  void mqtt_delivery_uses_the_captured_context_when_it_is_removed_during_processing() throws Exception {
    MQTTProtocol protocol = mock(MQTTProtocol.class, CALLS_REAL_METHODS);
    setField(protocol, "packetIdManager", new PacketIdManager());
    setField(protocol, "destinationTransformerMap", new ConcurrentHashMap<>());
    setField(protocol, "topicNameMapping", new ConcurrentHashMap<>());
    setField(protocol, "topicNameAnalyserMap", new ConcurrentHashMap<>());
    ConcurrentHashMap<String, StatisticsConfigDTO> statistics = new ConcurrentHashMap<>();
    statistics.put("/unused", new StatisticsConfigDTO());
    setField(protocol, "resourceNameAnalyserMap", statistics);
    doNothing().when(protocol).writeFrame(any());
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(manager.getContext()).thenReturn(new SubscriptionContext("/sensor")).thenReturn(null);

    assertDoesNotThrow(() -> protocol.sendMessage(event(manager, new AtomicInteger())));

    verify(protocol).writeFrame(any(io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish.class));
  }


  static Stream<Class<? extends Protocol>> metadata_protocols() {
    return Stream.of(MQTTProtocol.class, MQTT5Protocol.class, MQTT_SNProtocol.class,
        MavlinkProtocol.class, SatelliteGatewayProtocol.class);
  }

  static Stream<Class<? extends Protocol>> outbound_protocols() {
    return Stream.of(MQTTProtocol.class, MQTT5Protocol.class, SemTechProtocol.class,
        SatelliteGatewayProtocol.class);
  }

  @ParameterizedTest
  @MethodSource("metadata_protocols")
  void metadata_before_session_creation_omits_session_details(Class<? extends Protocol> type) throws Exception {
    Protocol protocol = metadataProtocol(type);

    ProtocolInformationDTO information = assertDoesNotThrow(protocol::getInformation);

    assertNotNull(information);
    assertNull(information.getClass().getMethod("getSessionInfo").invoke(information));
  }

  @ParameterizedTest
  @MethodSource("metadata_protocols")
  void metadata_preserves_active_session_details(Class<? extends Protocol> type) throws Exception {
    Protocol protocol = metadataProtocol(type);
    Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    setField(protocol, "session", session);

    ProtocolInformationDTO information = protocol.getInformation();
    assertSame(session.getSessionInformation(), information.getClass().getMethod("getSessionInfo").invoke(information));
  }

  @ParameterizedTest
  @MethodSource("outbound_protocols")
  void removed_subscription_completes_delivery_without_writing(Class<? extends Protocol> type) throws Exception {
    Protocol protocol = mock(type);
    setField(protocol, "logger", mock(Logger.class));
    if (type == MQTTProtocol.class || type == MQTT5Protocol.class) {
      setField(protocol, "packetIdManager", new PacketIdManager());
      doCallRealMethod().when(protocol).parseOutboundMessage(any());
    }
    doCallRealMethod().when(protocol).sendMessage(any());
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    AtomicInteger completions = new AtomicInteger();
    MessageEvent event = event(manager, completions);

    assertDoesNotThrow(() -> protocol.sendMessage(event));

    assertEquals(1, completions.get());
    verify(manager, never()).ackReceived(anyLong());
  }

  @Test
  void rest_delivery_after_subscription_removal_completes_without_buffering() {
    RestMessageListener listener = new RestMessageListener();
    AtomicInteger completions = new AtomicInteger();

    assertDoesNotThrow(() -> listener.sendMessage(event(mock(SubscribedEventManager.class), completions)));

    assertEquals(1, completions.get());
    assertEquals(0, listener.subscriptionDepth("/sensor"));
  }

  @Test
  void gateway_close_after_subscription_removal_releases_queued_delivery() {
    GatewayInfo gateway = new GatewayInfo(new byte[8], "gateway", null, null, null,
        mock(SubscribedEventManager.class), null);
    AtomicInteger completions = new AtomicInteger();
    gateway.getWaitingMessages().add(event(mock(SubscribedEventManager.class), completions));
    Session session = mock(Session.class);

    assertDoesNotThrow(() -> gateway.close(session));
    assertDoesNotThrow(() -> gateway.close(session));

    assertEquals(1, completions.get());
    assertTrue(gateway.getWaitingMessages().isEmpty());
    verifyNoInteractions(session);
  }

  @Test
  void amqp_send_after_subscription_removal_skips_delivery() throws Exception {
    ProtonEngine engine = mock(ProtonEngine.class);
    Transport transport = mock(Transport.class);
    when(engine.getTransport()).thenReturn(transport);
    SendMessageTask task = new SendMessageTask(engine, new MessageBuilder().build(),
        mock(SubscribedEventManager.class));

    assertTrue(assertDoesNotThrow(task::call));
  }

  @Test
  void amqp_close_uses_one_context_snapshot() {
    ProtonEngine engine = mock(ProtonEngine.class);
    AMQPProtocol protocol = mock(AMQPProtocol.class);
    Event event = mock(Event.class);
    org.apache.qpid.proton.engine.Session protonSession = mock(org.apache.qpid.proton.engine.Session.class);
    Link link = mock(Link.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(event.getSession()).thenReturn(protonSession);
    when(protonSession.getContext()).thenReturn(mock(Session.class));
    when(event.getLink()).thenReturn(link);
    when(link.getContext()).thenReturn(manager);
    when(manager.getContexts()).thenReturn(List.of(new SubscriptionContext("/sensor")));

    assertTrue(assertDoesNotThrow(() -> new LinkLocalCloseEventListener(protocol, engine).handleEvent(event)));
    verify(link).setContext(null);
  }

  private Protocol metadataProtocol(Class<? extends Protocol> type) throws Exception {
    Protocol protocol = mock(type);
    EndPoint endpoint = type == SatelliteGatewayProtocol.class
        ? mock(SatelliteEndPoint.class, RETURNS_DEEP_STUBS) : mock(EndPoint.class);
    setField(protocol, "endPoint", endpoint);
    doCallRealMethod().when(protocol).getInformation();
    return protocol;
  }

  private MessageEvent event(SubscribedEventManager manager, AtomicInteger completions) {
    return new MessageEvent("/sensor", manager, new MessageBuilder().build(), completions::incrementAndGet);
  }

  private void setField(Object target, String name, Object value) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
        return;
      } catch (NoSuchFieldException ignored) {
        // Fields may be inherited from a protocol superclass.
      }
    }
    throw new NoSuchFieldException(name);
  }
}
