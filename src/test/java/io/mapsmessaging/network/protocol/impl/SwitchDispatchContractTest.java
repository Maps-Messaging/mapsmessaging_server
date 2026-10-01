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

package io.mapsmessaging.network.protocol.impl;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.interceptors.impl.JMSDeliveryModeInterceptor;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.*;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.listeners.GetListener;
import io.mapsmessaging.network.protocol.impl.coap.listeners.PatchListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.NamedConsumer;
import io.mapsmessaging.network.protocol.impl.nats.streams.StreamSubscriptionInfo;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.transactions.handler.TransactionHandler;
import io.mapsmessaging.hardware.device.DeviceSessionManagement;
import io.mapsmessaging.hardware.device.handler.DeviceHandler;
import io.mapsmessaging.hardware.device.filter.DataFilter;
import io.mapsmessaging.hardware.device.handler.BusHandler;
import io.mapsmessaging.hardware.trigger.Trigger;
import io.mapsmessaging.devices.DeviceType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwitchDispatchContractTest {

  @Test
  void all_qos_levels_keep_existing_jms_delivery_modes() {
    JMSDeliveryModeInterceptor interceptor = new JMSDeliveryModeInterceptor();
    for (QualityOfService qos : QualityOfService.values()) {
      String expected = qos == QualityOfService.AT_MOST_ONCE ? "NON_PERSISTENT" : "PERSISTENT";
      assertEquals(expected, interceptor.get(new MessageBuilder().setQoS(qos).build()));
    }
  }

  @Test
  void patch_requests_only_reply_to_confirmable_packets() {
    for (TYPE type : TYPE.values()) {
      BasePacket request = new BasePacket(0, type, Code.EMPTY, 1, 42, new byte[]{1});
      BasePacket response = new PatchListener().handle(request, null);
      if (type == TYPE.CON) {
        assertEquals(Code.METHOD_NOT_ALLOWED, response.getCode());
        assertEquals(TYPE.ACK, response.getType());
        assertEquals(42, response.getMessageId());
      } else {
        assertNull(response);
      }
    }
  }

  @Test
  void get_con_and_non_requests_use_the_destination_lookup() throws Exception {
    for (TYPE type : List.of(TYPE.CON, TYPE.NON)) {
      Session session = mock(Session.class);
      CoapProtocol protocol = mock(CoapProtocol.class);
      when(protocol.getSession()).thenReturn(session);
      when(session.findDestination("absent", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(null));
      BasePacket request = new BasePacket(0, type, Code.EMPTY, 1, 42, new byte[0]);
      UriPath path = new UriPath();
      path.add("absent");
      request.getOptions().putOption(path);
      assertEquals(Code.NOT_FOUND, new GetListener().handle(request, protocol).getCode());
      verify(session).findDestination("absent", DestinationType.TOPIC);
    }
  }

  @Test
  void mqtt_sn_v1_duplicate_connect_and_willmsg_return_the_previous_response() throws Exception {
    MQTT_SNPacket previous = mock(MQTT_SNPacket.class);
    var state = new io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.ConnectedState(previous);
    for (int packetId : new int[]{MQTT_SNPacket.CONNECT, MQTT_SNPacket.WILLMSG}) {
      MQTT_SNPacket packet = mock(MQTT_SNPacket.class);
      when(packet.getControlPacketId()).thenReturn(packetId);
      assertSame(previous, state.handleMQTTEvent(packet, null, null, null, null));
    }
  }

  @Test
  void mqtt_sn_v2_duplicate_connect_and_willmsg_return_the_previous_response() throws Exception {
    MQTT_SNPacket previous = mock(MQTT_SNPacket.class);
    var state = new io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.state.ConnectedState(previous);
    for (int packetId : new int[]{MQTT_SNPacket.CONNECT, MQTT_SNPacket.WILLMSG}) {
      MQTT_SNPacket packet = mock(MQTT_SNPacket.class);
      when(packet.getControlPacketId()).thenReturn(packetId);
      assertSame(previous, state.handleMQTTEvent(packet, null, null, null, null));
    }
  }

  @Test
  void nats_ack_and_term_acknowledge_the_same_message() throws Exception {
    for (String command : List.of("+ack", "+term")) {
      SessionState state = mock(SessionState.class);
      NamedConsumer consumer = mock(NamedConsumer.class);
      StreamSubscriptionInfo info = mock(StreamSubscriptionInfo.class);
      SubscribedEventManager events = mock(SubscribedEventManager.class);
      when(state.getNamedConsumers()).thenReturn(Map.of("consumer", consumer));
      when(consumer.getStreams()).thenReturn(List.of(info));
      when(info.getSubscribedEventManager()).thenReturn(events);
      PayloadFrame frame = mock(PayloadFrame.class);
      when(frame.getSubject()).thenReturn("$JS.ACK.stream.consumer.1.0.42.123.token");
      when(frame.getPayload()).thenReturn(command.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertNull(new TransactionHandler().handle(frame, null, state));
      verify(events).ackReceived(42);
    }
  }

  @Test
  void sensors_and_clocks_register_with_the_trigger() throws Exception {
    for (DeviceType type : List.of(DeviceType.SENSOR, DeviceType.CLOCK)) {
      DeviceHandler device = mock(DeviceHandler.class);
      Trigger trigger = mock(Trigger.class);
      Session session = mock(Session.class);
      when(device.getSchemaId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000356"));
      when(device.getController()).thenReturn(mock(io.mapsmessaging.devices.DeviceController.class));
      when(device.getType()).thenReturn(type);
      when(device.getTrigger()).thenReturn(trigger);
      when(device.getTopicName(anyString())).thenReturn("device/data");
      when(session.findDestination("device/data", DestinationType.TOPIC)).thenReturn(CompletableFuture.completedFuture(null));
      DeviceSessionManagement management = new DeviceSessionManagement(device, "device", DataFilter.ALWAYS_SEND, mock(BusHandler.class), null);
      management.setSession(session);
      management.start();
      verify(trigger).addTask(management);
    }
  }
}
