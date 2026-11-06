/*
 *
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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessagePipelineCharacterizationTest {
  @Test
  void missing_context_completes_without_sending() {
    Fixture f = fixture();
    when(f.subscription.getContext()).thenReturn(null);
    f.pipeline.queue(f.event);
    assertEquals(1, f.completed.get());
    verifyNoInteractions(f.engine);
  }

  @Test
  void paused_qos_zero_drops_only_when_depth_exceeds_one() {
    Fixture f = fixture();
    f.pipeline.pause();
    when(f.subscription.getDepth()).thenReturn(2);
    f.pipeline.queue(f.event);
    assertEquals(1, f.completed.get());
    assertEquals(0, f.pipeline.size());
    when(f.subscription.getDepth()).thenReturn(1);
    f.pipeline.queue(f.event);
    assertEquals(1, f.pipeline.size());
    assertEquals(1, f.completed.get());
  }

  @Test
  void abandoned_queued_context_completes_once_and_finishes_drain() {
    Fixture f = fixture();
    f.pipeline.pause();
    f.pipeline.queue(f.event);
    when(f.subscription.getContext()).thenReturn(null);
    AtomicInteger drain = new AtomicInteger();
    f.pipeline.emptyQueue(0, drain::incrementAndGet);
    f.pipeline.ackReceived();
    assertEquals(1, f.completed.get());
    assertEquals(1, drain.get());
    verify(f.aliases, times(2)).clear();
  }

  @Test
  void expired_queued_event_completes_before_drain_callback() {
    Fixture f = fixture();
    when(f.message.getCreation()).thenReturn(0L);
    f.pipeline.pause();
    f.pipeline.queue(f.event);
    f.pipeline.emptyQueue(1, () -> assertEquals(1, f.completed.get()));
    assertEquals(1, f.completed.get());
    assertEquals(0, f.pipeline.size());
    verify(f.engine, never()).sendPublish(any(), any(), any());
  }

  private Fixture fixture() {
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class);
    MqttSnConfig config = mock(MqttSnConfig.class);
    when(protocol.getProtocolConfig()).thenReturn(config);
    when(config.getMaxInFlightEvents()).thenReturn(2);
    when(config.isDropQoS0()).thenReturn(true);
    when(config.getEventQueueTimeout()).thenReturn(1);
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    when(subscription.getContext()).thenReturn(context);
    when(subscription.getDepth()).thenReturn(1);
    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    Message message = mock(Message.class);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(message.getCreation()).thenReturn(System.currentTimeMillis());
    AtomicInteger completed = new AtomicInteger();
    return new Fixture(new MessagePipeline(protocol, engine), engine, aliases, subscription,
        message, new MessageEvent("topic", subscription, message, completed::incrementAndGet), completed);
  }

  private record Fixture(MessagePipeline pipeline, StateEngine engine, TopicAliasManager aliases,
      SubscribedEventManager subscription, Message message, MessageEvent event, AtomicInteger completed) {}
}
