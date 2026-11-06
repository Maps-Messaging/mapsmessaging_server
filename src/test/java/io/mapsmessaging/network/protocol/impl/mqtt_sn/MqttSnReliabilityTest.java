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

package io.mapsmessaging.network.protocol.impl.mqtt_sn;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline.MessagePipeline;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Connect;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Register;
import io.mapsmessaging.storage.delivery.PacketIdStore;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopicRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.InitialConnectionState;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.TopicAliasManager;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttSnReliabilityTest {
  @Test
  void written_qos_one_publish_is_not_completed_again_after_unsubscribe() throws Exception {
    MQTT_SNProtocol protocol = protocol();
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.findTopicAlias("/sensor")).thenReturn((short) 5);
    var field = MQTT_SNProtocol.class.getDeclaredField("stateEngine");
    field.setAccessible(true);
    field.set(protocol, engine);
    doCallRealMethod().when(protocol).buildPublish(anyShort(), anyInt(), any(), any(), anyShort());
    MessagePipeline pipeline = new MessagePipeline(protocol, engine);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/sensor");
    context.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    when(manager.getContext()).thenReturn(context);
    AtomicInteger completions = new AtomicInteger();
    pipeline.queue(new MessageEvent("/sensor", manager, new MessageBuilder().build(), completions::incrementAndGet));
    var sent = org.mockito.ArgumentCaptor.forClass(MQTT_SNPacket.class);
    verify(engine).sendPublish(eq(protocol), eq("/sensor"), sent.capture());
    sent.getValue().complete();
    assertEquals(1, completions.get());
    when(manager.getContext()).thenReturn(null);

    pipeline.resume();

    assertEquals(1, completions.get());
  }

  @Test
  void concurrent_stale_cleanup_completes_the_removed_entry_once() throws Exception {
    MessagePipeline pipeline = pipeline();
    pipeline.pause();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(manager.getContext()).thenReturn(new SubscriptionContext("/sensor"));
    AtomicInteger completions = new AtomicInteger();
    pipeline.queue(new MessageEvent("/sensor", manager, new MessageBuilder().build(), completions::incrementAndGet));
    var bothReaders = new java.util.concurrent.CyclicBarrier(2);
    when(manager.getContext()).thenAnswer(invocation -> {
      bothReaders.await(5, java.util.concurrent.TimeUnit.SECONDS);
      return null;
    });
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first = executor.submit(pipeline::resume);
      var second = executor.submit(pipeline::resume);
      first.get(5, java.util.concurrent.TimeUnit.SECONDS);
      second.get(5, java.util.concurrent.TimeUnit.SECONDS);
    }
    assertEquals(1, completions.get());
    assertEquals(0, pipeline.size());
  }
  @Test
  void qos_one_waiting_for_registration_is_completed_once_after_unsubscribe() {
    MQTT_SNProtocol protocol = protocol();
    MessagePipeline pipeline = registrationPipeline(protocol);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/sensor");
    context.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    when(manager.getContext()).thenReturn(context);
    AtomicInteger completions = new AtomicInteger();
    pipeline.queue(new MessageEvent("/sensor", manager, new MessageBuilder().build(), completions::incrementAndGet));
    when(manager.getContext()).thenReturn(null);

    pipeline.resume();

    assertEquals(1, completions.get());
    assertEquals(0, pipeline.size());
  }

  @Test
  void waiting_for_topic_registration_does_not_allocate_a_publish_identifier() throws Exception {
    MQTT_SNProtocol protocol = protocol();
    MessagePipeline pipeline = registrationPipeline(protocol);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    SubscriptionContext context = new SubscriptionContext("/sensor");
    context.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
    when(manager.getContext()).thenReturn(context);
    when(manager.getDeliveryKey()).thenReturn(context.getKey());

    try (PacketIdStore deliveries = PacketIdStore.memory(65535)) {
      protocol.getPacketIdManager().bindStore(deliveries);
      pipeline.queue(new MessageEvent("/sensor", manager, new MessageBuilder().build(), () -> {}));

      var sent = org.mockito.ArgumentCaptor.forClass(MQTT_SNPacket.class);
      verify(protocol).writeFrame(sent.capture());
      Register register = assertInstanceOf(Register.class, sent.getValue());
      assertNotEquals(0, register.getMessageId());
      assertEquals(1, protocol.getPacketIdManager().size());
      var control = protocol.getPacketIdManager().receivedPacket(register.getMessageId());
      assertNotNull(control);
      assertNull(control.getSubscription());
      assertEquals(-1, control.getMessageId());
      assertTrue(deliveries.outstanding().isEmpty(), "REGISTER must not create a durable PUBLISH delivery");
      verify(protocol, never()).buildPublish(anyShort(), anyInt(), any(), any(), anyShort());
    }
  }

  private MessagePipeline registrationPipeline(MQTT_SNProtocol protocol) {
    StateEngine engine = mock(StateEngine.class);
    TopicAliasManager aliases = mock(TopicAliasManager.class);
    when(engine.getTopicAliasManager()).thenReturn(aliases);
    when(aliases.findTopicAlias("/sensor")).thenReturn((short) -1);
    when(aliases.findRegisteredTopicAlias(any(), eq("/sensor"))).thenReturn(-1);
    when(aliases.getTopicAlias("/sensor")).thenReturn((short) 5);
    return new MessagePipeline(protocol, engine);
  }
  @Test
  void factory_rejects_missing_configuration_with_io_exception() {
    EndPoint endpoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    when(endpoint.getConfig().getProtocolConfig("mqtt-sn")).thenReturn(null);
    IOException error = assertThrows(IOException.class,
        () -> new MQTT_SNProtocolFactory().create(endpoint, mock(InterfaceInformation.class)));
    assertTrue(error.getMessage().contains("MQTT-SN"));
  }

  @Test
  void state_engine_uses_bound_configuration_without_endpoint_lookup() {
    MQTT_SNProtocol protocol = protocol();
    assertDoesNotThrow(() -> new StateEngine(protocol, mock(RegisteredTopicConfiguration.class)));
  }

  @Test
  void connect_uses_bound_configuration_without_endpoint_lookup() {
    MQTT_SNProtocol protocol = protocol();
    Connect connect = mock(Connect.class);
    when(connect.getControlPacketId()).thenReturn(MQTT_SNPacket.CONNECT);
    when(connect.getClientId()).thenReturn("client");
    when(connect.will()).thenReturn(true);
    EndPoint endpoint = mock(EndPoint.class, RETURNS_DEEP_STUBS);
    when(endpoint.getConfig().getProtocolConfig("mqtt-sn")).thenReturn(null);
    assertInstanceOf(WillTopicRequest.class, assertDoesNotThrow(() ->
        new InitialConnectionState().handleMQTTEvent(connect, null,
            endpoint, protocol, mock(StateEngine.class))));
  }

  @Test
  void pipeline_releases_event_whose_subscription_was_removed_before_queueing() {
    MessagePipeline pipeline = pipeline();
    AtomicInteger completions = new AtomicInteger();
    MessageEvent event = new MessageEvent("/sensor", mock(SubscribedEventManager.class),
        new MessageBuilder().build(), completions::incrementAndGet);
    assertDoesNotThrow(() -> pipeline.queue(event));
    assertEquals(1, completions.get());
    assertEquals(0, pipeline.size());
  }

  @Test
  void pipeline_releases_stale_queued_event_when_resumed() {
    MessagePipeline pipeline = pipeline();
    pipeline.pause();
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(manager.getContext()).thenReturn(new SubscriptionContext("/sensor"));
    AtomicInteger completions = new AtomicInteger();
    MessageEvent event = new MessageEvent("/sensor", manager,
        new MessageBuilder().build(), completions::incrementAndGet);
    pipeline.queue(event);
    when(manager.getContext()).thenReturn(null);
    assertDoesNotThrow(pipeline::resume);
    assertEquals(1, completions.get());
    assertEquals(0, pipeline.size());
  }

  private MessagePipeline pipeline() {
    MQTT_SNProtocol protocol = protocol();
    // Existing code uses this lookup; keep it populated so the delivery regression
    // isolates subscription removal from the separate configuration regression.
    MqttSnConfig config = (MqttSnConfig) protocol.getProtocolConfig();
    when(protocol.getEndPoint().getConfig().getProtocolConfig("mqtt-sn"))
        .thenReturn(config);
    return new MessagePipeline(protocol, mock(StateEngine.class));
  }

  private MQTT_SNProtocol protocol() {
    MQTT_SNProtocol protocol = mock(MQTT_SNProtocol.class, RETURNS_DEEP_STUBS);
    MqttSnConfig config = new MqttSnConfig(new ConfigurationProperties());
    when(protocol.getProtocolConfig()).thenReturn(config);
    when(protocol.getPacketIdManager()).thenReturn(new PacketIdManager());
    when(protocol.getEndPoint().getConfig().getProtocolConfig("mqtt-sn")).thenReturn(null);
    return protocol;
  }
}
