/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License 2.0 with the Commons Clause
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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.session.ClientConnection;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;

import java.security.Principal;

public class ProtocolClientConnection implements ClientConnection {

  private final Protocol protocol;
  public ProtocolClientConnection(Protocol protocol){
    this.protocol = protocol;
  }

  @Override
  public long getTimeOut() {
    return protocol.getTimeOut();
  }

  @Override
  public long getKeepAliveTaskInterval() {
    return protocol.getKeepAliveTaskInterval();
  }

  @Override
  public boolean tryAcquireSendSlot(SubscribedEventManager subscription) {
    if (protocol instanceof MQTTProtocol mqttProtocol) {
      return mqttProtocol.getPacketIdManager().tryAcquireSendSlot(subscription);
    }
    if (protocol instanceof MQTT5Protocol mqtt5Protocol) {
      return tryAcquire(
          mqtt5Protocol.getPacketIdManager(),
          mqtt5Protocol.getSession() == null ? 0 : mqtt5Protocol.getSession().getReceiveMaximum(),
          subscription);
    }
    return true;
  }

  @Override
  public boolean tryAcquireSendSlot(SubscribedEventManager subscription, Message message) {
    if (protocol instanceof MQTT5Protocol && !mqtt5RequiresPacketIdentifier(subscription, message)) {
      return true;
    }
    if (protocol instanceof MQTTProtocol && !subscriptionRequiresPacketIdentifier(subscription)) {
      return true;
    }
    return tryAcquireSendSlot(subscription);
  }

  @Override
  public void releaseSendSlot(SubscribedEventManager subscription) {
    PacketIdManager manager = packetIdManager();
    if (manager != null) {
      manager.releaseSendSlot(subscription);
    }
  }

  @Override
  public void releaseUnusedSendSlot(SubscribedEventManager subscription) {
    PacketIdManager manager = packetIdManager();
    if (manager != null) {
      manager.releaseUnusedSendSlot(subscription);
    }
  }

  private boolean mqtt5RequiresPacketIdentifier(SubscribedEventManager subscription, Message message) {
    SubscriptionContext context = subscription == null ? null : subscription.getContext();
    QualityOfService subscriptionQos = context == null ? null : context.getQualityOfService();
    if (subscriptionQos == null) {
      return true;
    }
    QualityOfService messageQos = message == null ? null : message.getQualityOfService();
    if (messageQos == null) {
      return subscriptionQos.isSendPacketId();
    }
    QualityOfService effectiveQos = QualityOfService.getInstance(
        Math.min(subscriptionQos.getLevel(), messageQos.getLevel()));
    return effectiveQos.isSendPacketId();
  }

  private boolean subscriptionRequiresPacketIdentifier(SubscribedEventManager subscription) {
    SubscriptionContext context = subscription == null ? null : subscription.getContext();
    QualityOfService qos = context == null ? null : context.getQualityOfService();
    return qos == null || qos.isSendPacketId();
  }

  private boolean tryAcquire(PacketIdManager packetIdManager, int maximumOutstanding, SubscribedEventManager subscription) {
    if (maximumOutstanding > 0) {
      packetIdManager.setMaximumOutstanding(maximumOutstanding);
    }
    return packetIdManager.tryAcquireSendSlot(subscription);
  }

  private PacketIdManager packetIdManager() {
    if (protocol instanceof MQTTProtocol mqttProtocol) {
      return mqttProtocol.getPacketIdManager();
    }
    if (protocol instanceof MQTT5Protocol mqtt5Protocol) {
      return mqtt5Protocol.getPacketIdManager();
    }
    return null;
  }

  @Override
  public String getName() {
    return protocol.getName();
  }

  @Override
  public String getVersion() {
    return protocol.getVersion();
  }

  @Override
  public void sendKeepAlive() {
    protocol.sendKeepAlive();
  }

  @Override
  public Principal getPrincipal() {
    return protocol.getEndPoint().getEndPointPrincipal();
  }

  @Override
  public String getAuthenticationConfig() {
    return protocol.getEndPoint().getAuthenticationConfig();
  }

  @Override
  public String getUniqueName() {
    if (protocol.getEndPoint() != null) {
      return protocol.getEndPoint().getName();
    }
    return "";
  }

  @Override
  public String getProtocolName() {
    if (protocol.getEndPoint() != null) {
      return protocol.getEndPoint().getProtocol();
    }
    return "";
  }

  @Override
  public String getRemoteIp() {
    if (protocol.getEndPoint() != null) {
      return protocol.getEndPoint().getRemoteSocketAddress();
    }
    return "";
  }
}
