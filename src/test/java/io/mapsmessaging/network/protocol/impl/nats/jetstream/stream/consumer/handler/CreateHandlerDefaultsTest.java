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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.handler;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.nats.frames.MsgFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamRequestManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamArchiveManager;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamSubscriptionInitializer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.DurableConsumerSession;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.AckPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.DeliverPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ReplayPolicy;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.streams.NamespaceManager;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfoList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateHandlerDefaultsTest {
  @Test
  void defaultsDoNotOverrideExplicitConsumerPolicies() {
    ConsumerConfig config = new ConsumerConfig();
    config.setAckPolicy(AckPolicy.NONE);
    config.setMaxDeliver(4);
    config.setMaxAckPending(23);
    CreateHandler.applyDefaults(config);

    assertEquals(AckPolicy.NONE, config.getAckPolicy());
    assertEquals(4, config.getMaxDeliver());
    assertEquals(23, config.getMaxAckPending());
    assertEquals(ReplayPolicy.INSTANT, config.getReplayPolicy());
    assertEquals(DeliverPolicy.ALL, config.getDeliverPolicy());
  }

  @Test
  void absentConsumerPoliciesReceiveDefaults() {
    ConsumerConfig config = new ConsumerConfig();
    CreateHandler.applyDefaults(config);

    assertEquals(AckPolicy.EXPLICIT, config.getAckPolicy());
    assertEquals(-1, config.getMaxDeliver());
    assertEquals(1000, config.getMaxAckPending());
    assertEquals(ReplayPolicy.INSTANT, config.getReplayPolicy());
    assertEquals(DeliverPolicy.ALL, config.getDeliverPolicy());
  }
  @Test
  void consumerSubscriptionFailureIsObservableToCreation() throws Exception {
    Session session = mock(Session.class);
    StreamInfo stream = mock(StreamInfo.class);
    when(stream.getSubject()).thenReturn("orders.created");
    when(session.findDestination("orders_stream/orders/created", DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(mock(Destination.class)));
    ConsumerConfig config = new ConsumerConfig();
    CreateHandler.applyDefaults(config);

    try (var subscriptions = mockStatic(io.mapsmessaging.api.NatsJetStreamSubscriptions.class)) {
      assertNull(new CreateHandler().subscribeConsumerSubject(session, config, stream,
          "orders_stream", "jetstream:orders_stream:consumer:0"));
      subscriptions.verify(() -> io.mapsmessaging.api.NatsJetStreamSubscriptions.ensure(
          eq(session), any(Destination.class), argThat((SubscriptionContext context) ->
              context.getAlias().equals("jetstream:orders_stream:consumer:0")
                  && context.isSync()
                  && context.getRetainHandler() == io.mapsmessaging.api.features.RetainHandler.DO_NOT_SEND
                  && context.getAcknowledgementController() == ClientAcknowledgement.INDIVIDUAL)));
    }
  }
  @Test
  void wrappedOfficialConsumerConfigurationRetainsDurableFields() {
    JsonObject config = new JsonObject();
    config.addProperty("durable_name", "durable");
    config.addProperty("ack_policy", "explicit");
    config.addProperty("max_deliver", 5);
    JsonObject request = new JsonObject();
    request.add("config", config);

    ConsumerConfig parsed = new CreateHandler().parseConfig(request);

    assertEquals("durable", parsed.getDurableName());
    assertEquals(AckPolicy.EXPLICIT, parsed.getAckPolicy());
    assertEquals(5, parsed.getMaxDeliver());
  }

  @Test
  void filterSubjectUsesNatsTokensNotMapsSlashes() {
    assertTrue(CreateHandler.matchesFilter("orders/created", "orders.*"));
    assertTrue(CreateHandler.matchesFilter("orders/*", "orders.created"));
    assertTrue(CreateHandler.matchesFilter("orders/>", "orders.created.new"));
    assertTrue(CreateHandler.matchesFilter("orders/created/new", "orders.>"));
    assertFalse(CreateHandler.matchesFilter("orders/created", "other.*"));
    assertFalse(CreateHandler.matchesFilter("orders", "orders.>"));
    assertFalse(CreateHandler.matchesFilter("orders/created/new", "orders.*"));
    assertFalse(CreateHandler.matchesFilter("orders/created", "orders.created.extra"));
  }
  @Test
  void unknown_delivery_policy_is_rejected_instead_of_defaulting_to_all() {
    JsonObject config = new JsonObject();
    config.addProperty("deliver_policy", "unsupported_start_position");
    assertThrows(IllegalArgumentException.class, () -> new CreateHandler().parseConfig(config));
  }

  @Test
  void known_unimplemented_starting_policy_is_preserved_for_explicit_rejection() {
    ConsumerConfig config = new ConsumerConfig();
    config.setDeliverPolicy(DeliverPolicy.LAST);
    CreateHandler.applyDefaults(config);
    assertEquals(DeliverPolicy.LAST, config.getDeliverPolicy());
  }

  @Test
  void multi_destination_consumer_registers_and_seeds_each_maps_subscription() throws Exception {
    var state = mock(SessionState.class);
    var namespaces = mock(NamespaceManager.class);
    var stream = new StreamInfoList("EVENTS");
    var first = mock(DestinationImpl.class);
    var second = mock(DestinationImpl.class);
    when(first.getFullyQualifiedNamespace()).thenReturn("EVENTS/topic1");
    when(second.getFullyQualifiedNamespace()).thenReturn("EVENTS/topic2");
    stream.addSubject("topic1", first);
    stream.addSubject("topic2", second);
    when(namespaces.getStream("EVENTS")).thenReturn(stream);
    var requests = mock(JetStreamRequestManager.class);
    when(state.getJetStreamRequestManager()).thenReturn(requests);
    when(state.addNamedConsumer(any())).thenReturn(true);
    var consumerSession = mock(Session.class);
    when(consumerSession.getName()).thenReturn("consumer-session");
    var firstManager = mock(SubscribedEventManager.class);
    var secondManager = mock(SubscribedEventManager.class);
    var archive = mock(JetStreamArchiveManager.class);
    when(archive.ensureArchive(state, "EVENTS", "EVENTS/topic1")).thenReturn("archive-session");
    when(archive.ensureArchive(state, "EVENTS", "EVENTS/topic2")).thenReturn("archive-session");
    var request = new MsgFrame(128);
    request.setSubject("$JS.API.CONSUMER.CREATE.EVENTS.worker");
    request.setReplyTo("_INBOX.consumer");
    try (var namespaceAccess = mockStatic(NamespaceManager.class);
         var sessions = mockStatic(DurableConsumerSession.class);
         var archiveAccess = mockStatic(JetStreamArchiveManager.class);
         var seed = mockStatic(JetStreamSubscriptionInitializer.class)) {
      namespaceAccess.when(NamespaceManager::getInstance).thenReturn(namespaces);
      sessions.when(() -> DurableConsumerSession.openEphemeral(state))
          .thenReturn(consumerSession);
      sessions.when(() -> DurableConsumerSession.subscribe(
          eq(consumerSession), eq("EVENTS.topic1"), anyString(), eq(ClientAcknowledgement.INDIVIDUAL), eq(1)))
          .thenReturn(firstManager);
      sessions.when(() -> DurableConsumerSession.subscribe(
          eq(consumerSession), eq("EVENTS.topic2"), anyString(), eq(ClientAcknowledgement.INDIVIDUAL), eq(1)))
          .thenReturn(secondManager);
      archiveAccess.when(JetStreamArchiveManager::getInstance).thenReturn(archive);
      var response = assertInstanceOf(PayloadFrame.class,
          new CreateHandler().handle(request, new JsonObject(), state));
      var body = JsonParser.parseString(new String(response.getPayload(), StandardCharsets.UTF_8)).getAsJsonObject();
      assertFalse(body.has("error"), body.toString());
      assertEquals("worker", body.get("name").getAsString());
      var registered = ArgumentCaptor.forClass(NamedConsumer.class);
      verify(state).addNamedConsumer(registered.capture());
      assertEquals(2, registered.getValue().getStreams().size());
      assertTrue(registered.getValue().getStreams().stream().anyMatch(info -> info.getSubscribedEventManager() == firstManager));
      assertTrue(registered.getValue().getStreams().stream().anyMatch(info -> info.getSubscribedEventManager() == secondManager));
      seed.verify(() -> JetStreamSubscriptionInitializer.seedFromArchive(
          first, "archive-session", "consumer-session", null));
      seed.verify(() -> JetStreamSubscriptionInitializer.seedFromArchive(
          second, "archive-session", "consumer-session", null));
    }
  }

}
