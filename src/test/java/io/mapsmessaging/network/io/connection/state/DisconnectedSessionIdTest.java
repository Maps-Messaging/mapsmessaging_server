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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.connection.ConnectionSessionId;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisconnectedSessionIdTest {

  @Test
  void mqtt_connection_sends_namespace_bound_identity() throws Exception {
    String sessionId = connect("MQTT", List.of(link("pull", "/local", "/remote/#")));

    assertNotEquals("bridge", sessionId);
    assertEquals(ConnectionSessionId.derive("bridge", ConnectionSessionId.namespaces(
        List.of(link("pull", "/local", "/remote/#")))), sessionId);
    assertEquals(sessionId, connect("MQTT", List.of(link("pull", "/local", "/remote/#"))));
  }

  @Test
  void reordered_links_reuse_remote_session_identity() throws Exception {
    LinkConfigDTO pull = link("pull", "/local/in", "/remote/in/#");
    LinkConfigDTO push = link("push", "/local/out", "/remote/out/#");

    assertEquals(connect("MQTT", List.of(pull, push)), connect("MQTT", List.of(push, pull)));
  }

  @Test
  void changed_remote_namespace_starts_fresh_remote_session() throws Exception {
    assertNotEquals(connect("MQTT", List.of(link("pull", "/local", "/old/#"))),
        connect("MQTT", List.of(link("pull", "/local", "/new/#"))));
  }

  @Test
  void changed_local_namespace_starts_fresh_remote_session() throws Exception {
    assertNotEquals(connect("MQTT", List.of(link("pull", "/old", "/remote/#"))),
        connect("MQTT", List.of(link("pull", "/new", "/remote/#"))));
  }

  @Test
  void changed_direction_starts_fresh_remote_session() throws Exception {
    assertNotEquals(connect("MQTT", List.of(link("pull", "/local", "/remote/#"))),
        connect("MQTT", List.of(link("push", "/local", "/remote/#"))));
  }

  @Test
  void transformation_is_included_before_deriving_remote_identity() throws Exception {
    List<LinkConfigDTO> links = List.of(link("pull", "/local", "/remote/#"));
    assertEquals(ConnectionSessionId.derive("bridge?Transformation=transform", ConnectionSessionId.namespaces(links)),
        connect("MQTT", links, "transform"));
  }

  @Test
  void changed_schema_flag_starts_fresh_remote_session() throws Exception {
    LinkConfigDTO original = link("push", "/local", "/remote/#");
    LinkConfigDTO withSchema = link("push", "/local", "/remote/#");
    withSchema.setIncludeSchema(true);
    assertNotEquals(connect("MQTT", List.of(original)), connect("MQTT", List.of(withSchema)));
  }

  @Test
  void unrelated_protocol_preserves_configured_session_identity() throws Exception {
    assertEquals("bridge", connect("STOMP", List.of(link("pull", "/local", "/remote/#"))));
    assertEquals("bridge", connect("MQTT-SN", List.of(link("pull", "/local", "/remote/#"))));
  }

  private LinkConfigDTO link(String direction, String local, String remote) {
    LinkConfigDTO link = new LinkConfigDTO();
    link.setDirection(direction);
    link.setLocalNamespace(local);
    link.setRemoteNamespace(remote);
    return link;
  }

  private String connect(String factoryName, List<LinkConfigDTO> links) throws Exception {
    return connect(factoryName, links, null);
  }

  private String connect(String factoryName, List<LinkConfigDTO> links, String transformationName) throws Exception {
    ConfigurationProperties configuration = new ConfigurationProperties();
    configuration.put("url", "tcp://localhost:1883");
    configuration.put("protocol", "mqtt");
    EndPointConnectionServerConfig properties = new EndPointConnectionServerConfig(configuration);
    properties.getAuthConfig().setSessionId("bridge");
    properties.setLinkConfigs(links);
    properties.setLinkTransformation(transformationName);
    EndPointConnection connection = mock(EndPointConnection.class);
    Disconnected state = new Disconnected(connection);
    when(connection.getState()).thenReturn(state);
    when(connection.isRunning()).thenReturn(true);
    when(connection.getProperties()).thenReturn(properties);
    when(connection.getUrl()).thenReturn(new EndPointURL("tcp://localhost:1883"));
    EndPoint endpoint = mock(EndPoint.class);
    when(endpoint.getConfig()).thenReturn(properties);
    ProtocolImplFactory implementation = mock(ProtocolImplFactory.class);
    when(implementation.getName()).thenReturn(factoryName);
    Protocol protocol = mock(Protocol.class);
    when(implementation.connect(eq(endpoint), anyString(), isNull(), isNull(), anyMap()))
        .thenReturn(protocol);
    TransformationManager transformations = mock(TransformationManager.class);
    if (transformationName != null) {
      ProtocolMessageTransformation transformation = mock(ProtocolMessageTransformation.class);
      when(transformation.getName()).thenReturn(transformationName);
      when(transformations.getTransformation(transformationName)).thenReturn(transformation);
    }
    try (MockedStatic<TransformationManager> manager = mockStatic(TransformationManager.class);
         MockedConstruction<ProtocolFactory> factories = mockConstruction(ProtocolFactory.class,
             (factory, context) -> when(factory.getBoundedProtocol()).thenReturn(implementation))) {
      manager.when(TransformationManager::getInstance).thenReturn(transformations);
      state.connected(endpoint);
      ArgumentCaptor<String> sessionId = ArgumentCaptor.forClass(String.class);
      verify(implementation).connect(eq(endpoint), sessionId.capture(), isNull(), isNull(), anyMap());
      verify(connection).setProtocol(protocol);
      verify(connection).scheduleState(any(Connecting.class));
      assertEquals(1, factories.constructed().size());
      return sessionId.getValue();
    }
  }
}
