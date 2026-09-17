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

package io.mapsmessaging.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import org.junit.jupiter.api.Test;

class ProtocolClientConnectionFlowControlTest {

  @Test
  void mqtt5EffectiveQosZeroBypassesReceiveMaximumWindow() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    Session session = mock(Session.class);
    PacketIdManager manager = mock(PacketIdManager.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);

    when(protocol.getSession()).thenReturn(session);
    when(session.getReceiveMaximum()).thenReturn(1);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(subscription.getContext()).thenReturn(context);
    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertTrue(connection.tryAcquireSendSlot(subscription, message));
    verifyNoInteractions(manager);
  }

  @Test
  void mqtt5EffectiveQosOneUsesReceiveMaximumWindow() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    Session session = mock(Session.class);
    PacketIdManager manager = mock(PacketIdManager.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);

    when(protocol.getSession()).thenReturn(session);
    when(session.getReceiveMaximum()).thenReturn(1);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(subscription.getContext()).thenReturn(context);
    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(manager.tryAcquireSendSlot(subscription)).thenReturn(false);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertFalse(connection.tryAcquireSendSlot(subscription, message));
    verify(manager).setMaximumOutstanding(1);
    verify(manager).tryAcquireSendSlot(subscription);
  }

  @Test
  void mqtt5QosZeroContinuesWhileQosOneWindowIsFull() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    Session session = mock(Session.class);
    PacketIdManager manager = new PacketIdManager();
    SubscribedEventManager qosOneA = mock(SubscribedEventManager.class);
    SubscribedEventManager qosZeroB = mock(SubscribedEventManager.class);
    SubscribedEventManager qosOneC = mock(SubscribedEventManager.class);
    SubscriptionContext contextA = mock(SubscriptionContext.class);
    SubscriptionContext contextB = mock(SubscriptionContext.class);
    SubscriptionContext contextC = mock(SubscriptionContext.class);
    Message firstQosOne = mock(Message.class);
    Message qosZero = mock(Message.class);
    Message secondQosOne = mock(Message.class);

    when(protocol.getSession()).thenReturn(session);
    when(session.getReceiveMaximum()).thenReturn(1);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(qosOneA.getContext()).thenReturn(contextA);
    when(qosZeroB.getContext()).thenReturn(contextB);
    when(qosOneC.getContext()).thenReturn(contextC);
    when(contextA.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(contextB.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(contextC.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(firstQosOne.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(qosZero.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(secondQosOne.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertTrue(connection.tryAcquireSendSlot(qosOneA, firstQosOne));
    int packetId = manager.nextPacketIdentifier(qosOneA, 1L);

    assertTrue(connection.tryAcquireSendSlot(qosZeroB, qosZero));
    assertFalse(connection.tryAcquireSendSlot(qosOneC, secondQosOne));

    manager.completePacketId(packetId);

    verify(qosZeroB, never()).resumeDelivery();
    verify(qosOneC).resumeDelivery();
  }

  @Test
  void mqtt5QosTwoSubscriptionWithQosZeroMessageBypassesWindow() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    PacketIdManager manager = mock(PacketIdManager.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);

    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(subscription.getContext()).thenReturn(context);
    when(context.getQualityOfService()).thenReturn(QualityOfService.EXACTLY_ONCE);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertTrue(connection.tryAcquireSendSlot(subscription, message));
    verifyNoInteractions(manager);
  }

  @Test
  void mqtt5QosTwoSubscriptionWithQosOneMessageUsesWindow() {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    Session session = mock(Session.class);
    PacketIdManager manager = mock(PacketIdManager.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);

    when(protocol.getSession()).thenReturn(session);
    when(session.getReceiveMaximum()).thenReturn(2);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(subscription.getContext()).thenReturn(context);
    when(context.getQualityOfService()).thenReturn(QualityOfService.EXACTLY_ONCE);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(manager.tryAcquireSendSlot(subscription)).thenReturn(true);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertTrue(connection.tryAcquireSendSlot(subscription, message));
    verify(manager).setMaximumOutstanding(2);
    verify(manager).tryAcquireSendSlot(subscription);
  }

  @Test
  void mqtt311RetainsSubscriptionQosFlowControl() {
    MQTTProtocol protocol = mock(MQTTProtocol.class);
    Session session = mock(Session.class);
    PacketIdManager manager = mock(PacketIdManager.class);
    SubscribedEventManager subscription = mock(SubscribedEventManager.class);
    SubscriptionContext context = mock(SubscriptionContext.class);
    Message message = mock(Message.class);

    when(protocol.getSession()).thenReturn(session);
    when(session.getReceiveMaximum()).thenReturn(1);
    when(protocol.getPacketIdManager()).thenReturn(manager);
    when(subscription.getContext()).thenReturn(context);
    when(context.getQualityOfService()).thenReturn(QualityOfService.AT_LEAST_ONCE);
    when(message.getQualityOfService()).thenReturn(QualityOfService.AT_MOST_ONCE);
    when(manager.tryAcquireSendSlot(subscription)).thenReturn(false);

    ProtocolClientConnection connection = new ProtocolClientConnection(protocol);

    assertFalse(connection.tryAcquireSendSlot(subscription, message));
    verify(manager).setMaximumOutstanding(1);
    verify(manager).tryAcquireSendSlot(subscription);
    verify(manager, never()).releaseSendSlot(subscription);
  }
}
