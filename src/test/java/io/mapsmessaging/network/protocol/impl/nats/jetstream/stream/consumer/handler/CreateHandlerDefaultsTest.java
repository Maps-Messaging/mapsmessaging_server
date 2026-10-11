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

import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.AckPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ConsumerConfig;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.DeliverPolicy;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data.ReplayPolicy;
import org.junit.jupiter.api.Test;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
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
  void consumerSubscriptionFailureIsObservableToCreation() {
    SessionState session = mock(SessionState.class);
    StreamInfo stream = mock(StreamInfo.class);
    when(stream.getSubject()).thenReturn("orders.created");
    ConsumerConfig config = new ConsumerConfig();
    CreateHandler.applyDefaults(config);

    assertNull(new CreateHandler().subscribeConsumerSubject(session, config, stream, "consumer"));
    verify(session).subscribe("orders.created", "consumer", null, ClientAcknowledgement.INDIVIDUAL, 1, true);
  }
}
