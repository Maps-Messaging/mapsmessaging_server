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

import io.mapsmessaging.network.protocol.impl.mqtt.listeners.SubAckListener;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubAck;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.SubAckListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.SubAck5;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import io.mapsmessaging.config.protocol.impl.MqttConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.Protocol;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemoteSubscriptionTrackerTest {
  private final PacketIdManager ids = new PacketIdManager();
  private final RemoteSubscriptionTracker tracker = new RemoteSubscriptionTracker(ids);

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void protocol_close_completion_cannot_reenter_selector_teardown(boolean mqtt5) throws Exception {
    EndPoint endPoint = mock(EndPoint.class);
    MqttConfig mqttConfig = new MqttConfig();
    mqttConfig.setMaxServerKeepAlive(60);
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setEndPointConfig(new EndPointConfigDTO("tcp"));
    config.setProtocolConfigs(List.of(mqttConfig));
    when(endPoint.getConfig()).thenReturn(config);
    when(endPoint.getEndPointStatus()).thenReturn(mock(io.mapsmessaging.network.io.EndPointStatus.class));
    when(endPoint.getName()).thenReturn("suback-close-" + mqtt5);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("SubAckClose=" + mqtt5));
    Protocol protocol = mqtt5 ? new MQTT5Protocol(endPoint) : new MQTTProtocol(endPoint);
    SelectorTask selector = mock(SelectorTask.class);
    when(selector.isOpen()).thenReturn(true);
    Field field = protocol.getClass().getDeclaredField("selectorTask");
    field.setAccessible(true);
    field.set(protocol, selector);
    RemoteSubscriptionTracker pending = ((RemoteSubscriptionProtocol) protocol).getRemoteSubscriptionTracker();
    pending.register(List.of("topic"));
    pending.awaitAcknowledgements().whenComplete((ignored, failure) -> {
      try { protocol.close(); } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
    });
    protocol.close();
    verify(selector).close();
    verify(endPoint).close();
  }

  @Test
  void all_packet_ids_must_acknowledge_before_success() {
    int first = tracker.register(List.of("first"));
    int second = tracker.register(List.of("second"));
    var completion = tracker.awaitAcknowledgements();
    tracker.acknowledge(second, new int[] {2});
    assertFalse(completion.isDone());
    tracker.acknowledge(first, new int[] {0});
    assertDoesNotThrow(completion::join);
    assertEquals(0, ids.size());
  }

  @Test
  void mixed_results_report_exact_failed_topic_and_code() {
    int id = tracker.register(List.of("allowed", "restricted"));
    var completion = tracker.awaitAcknowledgements();
    tracker.acknowledge(id, new int[] {1, 0x87});
    var exception = assertThrows(CompletionException.class, completion::join);
    assertTrue(exception.getCause().getMessage().contains("restricted"));
    assertTrue(exception.getCause().getMessage().contains("0x87"));
  }

  @Test
  void unknown_suback_cannot_release_publish_identifier() {
    int publish = ids.nextPacketIdentifier(null, 42);
    int subscribe = tracker.register(List.of("topic"));
    var completion = tracker.awaitAcknowledgements();
    assertNotEquals(publish, subscribe);
    tracker.acknowledge(publish, new int[] {0});
    assertNotNull(ids.receivedPacket(publish));
    assertFalse(completion.isDone());
  }

  @Test
  void missing_result_is_failure() {
    int id = tracker.register(List.of("one", "two"));
    var completion = tracker.awaitAcknowledgements();
    tracker.acknowledge(id, new int[] {0});
    assertThrows(CompletionException.class, completion::join);
  }

  @Test
  void close_releases_pending_ids_and_new_attempt_can_succeed() {
    tracker.register(List.of("topic"));
    var completion = tracker.awaitAcknowledgements();
    tracker.close();
    assertThrows(CompletionException.class, completion::join);
    assertEquals(0, ids.size());
    var retry = new RemoteSubscriptionTracker(ids);
    int id = retry.register(List.of("topic"));
    var retried = retry.awaitAcknowledgements();
    retry.acknowledge(id, new int[] {0});
    assertDoesNotThrow(retried::join);
  }

  @Test
  void early_rejection_does_not_abort_registration_of_remaining_filters() {
    int first = tracker.register(List.of("restricted"));
    tracker.acknowledge(first, new int[] {0x80});
    int second = assertDoesNotThrow(() -> tracker.register(List.of("other")));
    var completion = tracker.awaitAcknowledgements();
    tracker.acknowledge(second, new int[] {0});
    assertThrows(CompletionException.class, completion::join);
  }

  @Test
  void mqtt3_listener_preserves_unsigned_rejection_code() throws Exception {
    int id = tracker.register(List.of("topic"));
    var completion = tracker.awaitAcknowledgements();
    MQTTProtocol protocol = mock(MQTTProtocol.class);
    when(protocol.getRemoteSubscriptionTracker()).thenReturn(tracker);
    new SubAckListener().handlePacket(new SubAck(id, new byte[] {(byte) 0x80}), null, null, protocol);
    var exception = assertThrows(CompletionException.class, completion::join);
    assertTrue(exception.getCause().getMessage().contains("0x80"));
  }

  @Test
  void mqtt5_listener_preserves_broker_reason_code() {
    int id = tracker.register(List.of("restricted"));
    var completion = tracker.awaitAcknowledgements();
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    when(protocol.getRemoteSubscriptionTracker()).thenReturn(tracker);
    new SubAckListener5().handlePacket(new SubAck5(id, new StatusCode[] {StatusCode.getInstance((byte) 0x87)}), null, null, protocol);
    var exception = assertThrows(CompletionException.class, completion::join);
    assertTrue(exception.getCause().getMessage().contains("0x87"));
  }
}
