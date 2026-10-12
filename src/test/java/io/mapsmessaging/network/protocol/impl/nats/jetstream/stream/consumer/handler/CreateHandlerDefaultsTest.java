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

import com.google.gson.JsonObject;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.AckPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.DeliverPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ReplayPolicy;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.api.features.DestinationType;
import java.util.concurrent.CompletableFuture;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamInfo;
import io.mapsmessaging.api.features.ClientAcknowledgement;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

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

    assertNull(new CreateHandler().subscribeConsumerSubject(session, config, stream,
        "orders_stream", "jetstream:orders_stream:consumer:0"));
    verify(session).addSubscription(argThat((SubscriptionContext context) ->
        context.getAlias().equals("jetstream:orders_stream:consumer:0")
            && context.isSync() && context.getAcknowledgementController() == ClientAcknowledgement.INDIVIDUAL));
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
    assertTrue(CreateHandler.matchesFilter("orders/created/new", "orders.>"));
    assertFalse(CreateHandler.matchesFilter("orders/created", "other.*"));
    assertFalse(CreateHandler.matchesFilter("orders", "orders.>"));
    assertFalse(CreateHandler.matchesFilter("orders/created/new", "orders.*"));
    assertFalse(CreateHandler.matchesFilter("orders/created", "orders.created.extra"));
  }
}
