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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JetStreamApiDispatchTest {

  @Test
  void streamCreateReachesHandlerAndRepliesToOfficialClientInbox() throws Exception {
    SessionState sessionState = mock(SessionState.class);
    Session session = mock(Session.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(sessionState.getSession()).thenReturn(session);
    when(sessionState.getJetStreamRequestManager()).thenReturn(requests);
    when(requests.getSid("_INBOX.api.reply")).thenReturn("sid-api");
    Destination destination = mock(Destination.class);
    when(destination.getFullyQualifiedNamespace()).thenReturn("EVENTS/telemetry/data");
    when(session.findDestination("EVENTS/telemetry/data", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));

    MsgFrame request = new MsgFrame(1024);
    request.setSubject("$JS.API.STREAM.CREATE.EVENTS");
    request.setReplyTo("_INBOX.api.reply");
    request.setPayload("{\"name\":\"EVENTS\",\"subjects\":[\"telemetry.data\"]}".getBytes(StandardCharsets.UTF_8));

    NatsFrame response;
    JetStreamArchiveManager archive = mock(JetStreamArchiveManager.class);
    try (var archives = mockStatic(JetStreamArchiveManager.class);
         var memberships = mockStatic(io.mapsmessaging.api.NatsJetStreamDestinations.class)) {
      archives.when(JetStreamArchiveManager::getInstance).thenReturn(archive);
      response = new JetStreamApiManager().process(request.getSubject(), request, sessionState);
      verify(archive).ensureArchive(sessionState, "EVENTS", "EVENTS/telemetry/data");
      memberships.verify(() -> io.mapsmessaging.api.NatsJetStreamDestinations.enable(destination));
    }

    PayloadFrame reply = assertInstanceOf(PayloadFrame.class, response);
    assertEquals("_INBOX.api.reply", reply.getSubject());
    assertEquals("sid-api", reply.getSubscriptionId());
    JsonObject body = JsonParser.parseString(new String(reply.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals("io.nats.jetstream.api.v1.stream_create_response", body.get("type").getAsString());
    assertFalse(body.has("error"), body.toString());
    assertEquals("EVENTS", body.getAsJsonObject("config").get("name").getAsString());
    verify(session).findDestination("EVENTS/telemetry/data", DestinationType.TOPIC);
  }

  @Test
  void consumerApiStillReachesConsumerHandler() throws Exception {
    SessionState sessionState = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(sessionState.getJetStreamRequestManager()).thenReturn(requests);
    when(sessionState.getNamedConsumers()).thenReturn(Map.of());
    MsgFrame request = new MsgFrame(128);
    request.setSubject("$JS.API.CONSUMER.MSG.NEXT.EVENTS.missing");
    request.setReplyTo("_INBOX.api.reply");
    request.setPayload(new byte[0]);

    NatsFrame response = new JetStreamApiManager().process(request.getSubject(), request, sessionState);

    PayloadFrame reply = assertInstanceOf(PayloadFrame.class, response);
    JsonObject body = JsonParser.parseString(new String(reply.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals("io.nats.jetstream.api.v1.consumer_next_response", body.get("type").getAsString());
    assertTrue(body.has("error"));
  }

  @Test
  void unrelatedSubjectsCannotMatchStreamApiPrefix() throws Exception {
    MsgFrame request = new MsgFrame(128);
    request.setSubject("$JS.API.STREAMER.CREATE.EVENTS");

    ErrFrame error = assertInstanceOf(ErrFrame.class,
        new JetStreamApiManager().process(request.getSubject(), request, mock(SessionState.class)));

    assertTrue(error.getError().contains("Unknown JetStream API call"));
  }

  @Test
  void invalidStreamCreateReturnsTypedErrorToReplyInbox() throws Exception {
    SessionState state = mock(SessionState.class);
    JetStreamRequestManager requests = mock(JetStreamRequestManager.class);
    when(state.getJetStreamRequestManager()).thenReturn(requests);
    when(requests.getSid("_INBOX.api.reply")).thenReturn("sid-api");
    MsgFrame request = new MsgFrame(128);
    request.setSubject("$JS.API.STREAM.CREATE.EVENTS");
    request.setReplyTo("_INBOX.api.reply");
    request.setPayload(new byte[0]);

    PayloadFrame response = assertInstanceOf(PayloadFrame.class,
        new JetStreamApiManager().process(request.getSubject(), request, state));

    assertEquals("_INBOX.api.reply", response.getSubject());
    assertEquals("sid-api", response.getSubscriptionId());
    JsonObject body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
    assertEquals("io.nats.jetstream.api.v1.stream_create_response", body.get("type").getAsString());
    assertEquals(400, body.getAsJsonObject("error").get("code").getAsInt());
    verify(state, never()).getSession();
  }
}
