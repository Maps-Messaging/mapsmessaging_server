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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream;

import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.NatsJetStreamDestinations;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.dto.rest.config.protocol.impl.NatsConfigDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.protocol.impl.nats.NatsProtocol;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.StreamDeleteHandler;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.StreamUpdateHandler;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StreamArchiveLifecycleTest {

  @Test
  void stream_delete_releases_archive_and_logical_membership_without_deleting_maps_data() throws Exception {
    SessionState state = state();
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/telemetry/data");
    StreamInfoList stream = new StreamInfoList("EVENTS");
    stream.addSubject("telemetry/data", resource);
    NamespaceManager namespaces = mock(NamespaceManager.class);
    when(namespaces.getStream("EVENTS")).thenReturn(stream);
    Destination destination = mock(Destination.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("EVENTS/telemetry/data");
    when(state.getSession().findDestination("EVENTS/telemetry/data", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    JetStreamArchiveManager archive = mock(JetStreamArchiveManager.class);
    try (var namespaceAccess = mockStatic(NamespaceManager.class);
         var archiveAccess = mockStatic(JetStreamArchiveManager.class);
         var membership = mockStatic(NatsJetStreamDestinations.class)) {
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      archiveAccess.when(JetStreamArchiveManager::getInstance).thenReturn(archive);
      PayloadFrame response = assertInstanceOf(PayloadFrame.class,
          new StreamDeleteHandler().handle(request("DELETE"), null, state));
      var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertTrue(body.get("success").getAsBoolean());
      membership.verify(() -> NatsJetStreamDestinations.checkRemoval(destination));
      membership.verify(() -> NatsJetStreamDestinations.disable(destination));
      verify(archive).releaseStream("EVENTS");
      verify(state.getSession(), never()).deleteDestinationImpl(any());
      verify(resource, never()).close();
    }
  }

  @Test
  void unchanged_dotted_subjects_do_not_remove_existing_slash_destinations() throws Exception {
    SessionState state = state();
    DestinationImpl resource = mock(DestinationImpl.class);
    when(resource.getFullyQualifiedNamespace()).thenReturn("EVENTS/telemetry/data");
    StreamInfoList stream = new StreamInfoList("EVENTS");
    stream.addSubject("telemetry/data", resource);
    NamespaceManager namespaces = mock(NamespaceManager.class);
    when(namespaces.getStream("EVENTS")).thenReturn(stream);
    JetStreamArchiveManager archive = mock(JetStreamArchiveManager.class);
    try (var namespaceAccess = mockStatic(NamespaceManager.class);
         var archiveAccess = mockStatic(JetStreamArchiveManager.class);
         var membership = mockStatic(NatsJetStreamDestinations.class)) {
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      archiveAccess.when(JetStreamArchiveManager::getInstance).thenReturn(archive);
      var config = JsonParser.parseString("{\"subjects\":[\"telemetry.data\"]}").getAsJsonObject();
      PayloadFrame response = assertInstanceOf(PayloadFrame.class,
          new StreamUpdateHandler().handle(request("UPDATE"), config, state));
      var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertFalse(body.has("error"), body.toString());
      assertEquals("telemetry.data", body.getAsJsonObject("config").getAsJsonArray("subjects").get(0).getAsString());
      verify(state.getSession(), never()).findDestination(anyString(), any());
      verify(state.getSession(), never()).deleteDestinationImpl(any());
      membership.verifyNoInteractions();
    }
  }

  private static SessionState state() {
    SessionState state = mock(SessionState.class);
    NatsProtocol protocol = mock(NatsProtocol.class);
    NatsConfigDTO config = mock(NatsConfigDTO.class);
    when(state.getProtocol()).thenReturn(protocol);
    when(protocol.getNatsConfig()).thenReturn(config);
    when(config.isEnableStreamDelete()).thenReturn(true);
    when(state.getSession()).thenReturn(mock(Session.class));
    when(state.getJetStreamRequestManager()).thenReturn(mock(JetStreamRequestManager.class));
    return state;
  }

  private static MsgFrame request(String action) {
    MsgFrame request = new MsgFrame(128);
    request.setSubject("$JS.API.STREAM." + action + ".EVENTS");
    request.setReplyTo("_INBOX.lifecycle");
    return request;
  }
}
