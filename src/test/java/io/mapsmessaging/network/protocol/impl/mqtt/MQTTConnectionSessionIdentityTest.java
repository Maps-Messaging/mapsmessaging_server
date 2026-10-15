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

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.auth.AuthConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.ServerPacket;
import io.mapsmessaging.network.io.connection.ConnectionSessionId;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.ConnAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.ConnAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.ConnAckListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MQTTConnectionSessionIdentityTest {

  private static final AtomicInteger IDS = new AtomicInteger();

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void successful_connack_uses_the_exact_connect_identity(boolean mqtt5) throws Exception {
    List<LinkConfigDTO> links = links("/local/a");
    String sentId = ConnectionSessionId.derive("configured-base", ConnectionSessionId.namespaces(links));
    Fixture fixture = fixture(mqtt5, "configured-base", links);
    SessionContext context = connectAndAcknowledge(fixture, sentId);
    assertEquals(sentId, context.getId());
    assertTrue(context.isPersistentSession());
    assertFalse(context.isResetState());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void generated_identity_is_shared_when_the_configured_base_is_absent(boolean mqtt5) throws Exception {
    Fixture fixture = fixture(mqtt5, null, links("/local/a"));
    String sentId = UUID.randomUUID().toString();
    assertEquals(sentId, connectAndAcknowledge(fixture, sentId).getId());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unchanged_namespace_reconnect_uses_the_same_local_identity(boolean mqtt5) throws Exception {
    List<LinkConfigDTO> links = links("/local/a");
    String sentId = ConnectionSessionId.derive("base", ConnectionSessionId.namespaces(links));
    SessionContext first = connectAndAcknowledge(fixture(mqtt5, "base", links), sentId);
    SessionContext reconnect = connectAndAcknowledge(fixture(mqtt5, "base", links), sentId);
    assertEquals(first.getId(), reconnect.getId());
    assertFalse(reconnect.isResetState());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void changed_namespace_uses_a_different_local_identity(boolean mqtt5) throws Exception {
    List<LinkConfigDTO> originalLinks = links("/local/a");
    List<LinkConfigDTO> changedLinks = links("/local/b");
    String originalId = ConnectionSessionId.derive("base", ConnectionSessionId.namespaces(originalLinks));
    String changedId = ConnectionSessionId.derive("base", ConnectionSessionId.namespaces(changedLinks));
    SessionContext original = connectAndAcknowledge(fixture(mqtt5, "base", originalLinks), originalId);
    SessionContext changed = connectAndAcknowledge(fixture(mqtt5, "base", changedLinks), changedId);
    assertNotEquals(original.getId(), changed.getId());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void direct_listener_uses_configured_identity_and_namespaces(boolean mqtt5) throws Exception {
    List<LinkConfigDTO> links = links("/local/a");
    Fixture fixture = fixture(mqtt5, "base", links);
    String expected = ConnectionSessionId.derive("base", ConnectionSessionId.namespaces(links));
    assertEquals(expected, acknowledge(fixture).getId());
  }

  private static SessionContext connectAndAcknowledge(Fixture fixture, String sentId) throws Exception {
    try {
      assertNull(fixture.mqtt5
          ? ((MQTT5Protocol) fixture.protocol).getConnectionSessionId()
          : ((MQTTProtocol) fixture.protocol).getConnectionSessionId());
      fixture.protocol.connect(sentId, null, null);
      assertEquals(sentId, fixture.mqtt5
          ? ((MQTT5Protocol) fixture.protocol).getConnectionSessionId()
          : ((MQTTProtocol) fixture.protocol).getConnectionSessionId());
      assertEquals(1, fixture.frames.size());
      String actualId = fixture.mqtt5
          ? assertInstanceOf(Connect5.class, fixture.frames.get(0)).getSessionId()
          : assertInstanceOf(Connect.class, fixture.frames.get(0)).getSessionId();
      assertEquals(sentId, actualId);
      SessionContext context = acknowledge(fixture);
      assertEquals(actualId, context.getId());
      return context;
    } finally {
      fixture.protocol.setConnected(false);
    }
  }

  private static SessionContext acknowledge(Fixture fixture) throws Exception {
    SessionManager manager = mock(SessionManager.class);
    TransformationManager transformations = mock(TransformationManager.class);
    Session session = mock(Session.class);
    SecurityContext security = mock(SecurityContext.class);
    when(session.getSecurityContext()).thenReturn(security);
    when(security.getUsername()).thenReturn("bridge");
    when(session.getName()).thenReturn("bridge");
    List<SessionContext> captured = new ArrayList<>();
    when(manager.createAsync(any(SessionContext.class), same(fixture.protocol))).thenAnswer(invocation -> {
      captured.add(invocation.getArgument(0));
      return CompletableFuture.completedFuture(session);
    });
    when(manager.create(any(SessionContext.class), same(fixture.protocol))).thenAnswer(invocation -> {
      captured.add(invocation.getArgument(0));
      return session;
    });
    try (MockedStatic<SessionManager> sessions = mockStatic(SessionManager.class);
         MockedStatic<TransformationManager> transformation = mockStatic(TransformationManager.class)) {
      sessions.when(SessionManager::getInstance).thenReturn(manager);
      transformation.when(TransformationManager::getInstance).thenReturn(transformations);
      if (fixture.mqtt5) {
        ConnAck5 ack = new ConnAck5();
        ack.setStatusCode(StatusCode.SUCCESS);
        new ConnAckListener5().handlePacket(ack, null, fixture.endPoint, fixture.protocol);
      } else {
        ConnAck ack = new ConnAck();
        ack.setResponseCode(ConnAck.SUCCESS);
        new ConnAckListener().handlePacket(ack, null, fixture.endPoint, fixture.protocol);
      }
      verify(session).resumeState();
      assertTrue(fixture.protocol.isConnected());
      assertEquals(1, captured.size());
      return captured.get(0);
    }
  }

  private static Fixture fixture(boolean mqtt5, String base, List<LinkConfigDTO> links) throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    EndPointConnectionServerConfig config = mock(EndPointConnectionServerConfig.class);
    AuthConfigDTO auth = new AuthConfigDTO();
    auth.setSessionId(base);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setMaxServerKeepAlive(60);
    when(config.getAuthConfig()).thenReturn(auth);
    when(config.getLinkConfigs()).thenReturn(links);
    when(config.getProtocolConfig("mqtt")).thenReturn(mqttConfig);
    when(config.getEndPointConfig()).thenReturn(new EndPointConfigDTO("tcp"));
    when(endPoint.getConfig()).thenReturn(config);
    int id = IDS.incrementAndGet();
    when(endPoint.getName()).thenReturn("identity-test-" + id);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("Identity=" + id));
    when(endPoint.getEndPointStatus()).thenReturn(mock(EndPointStatus.class));
    when(endPoint.getServer()).thenReturn(mock(EndPointServerStatus.class));
    Protocol protocol = mqtt5 ? spy(new MQTT5Protocol(endPoint)) : spy(new MQTTProtocol(endPoint));
    Field field = protocol.getClass().getDeclaredField("selectorTask");
    field.setAccessible(true);
    field.set(protocol, mock(SelectorTask.class));
    List<ServerPacket> frames = new ArrayList<>();
    org.mockito.stubbing.Answer<Void> captureFrame = invocation -> {
      frames.add(invocation.getArgument(0));
      return null;
    };
    if (mqtt5) {
      doAnswer(captureFrame).when((MQTT5Protocol) protocol).writeFrame(any(ServerPacket.class));
    } else {
      doAnswer(captureFrame).when((MQTTProtocol) protocol).writeFrame(any(ServerPacket.class));
    }
    return new Fixture(protocol, endPoint, mqtt5, frames);
  }

  private static List<LinkConfigDTO> links(String local) {
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection("push");
    link.setLocalNamespace(local);
    link.setRemoteNamespace("/remote/#");
    return List.of(link);
  }

  private record Fixture(Protocol protocol, EndPoint endPoint, boolean mqtt5, List<ServerPacket> frames) {}
}
