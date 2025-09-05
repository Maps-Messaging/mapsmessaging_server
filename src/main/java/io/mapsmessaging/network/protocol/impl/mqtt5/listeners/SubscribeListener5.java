/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.SubAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Subscribe5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessageProperty;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.UserProperty;

import java.io.IOException;
import java.util.List;

public class SubscribeListener5 extends PacketListener5 {

  protected static SubscriptionContext createContext(SubscriptionInfo info, String selector, int receiveMaximum) {
    QualityOfService qos = info.getQualityOfService();
    ClientAcknowledgement ackManger = qos.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(info.getTopicName(), ackManger);
    builder.setQos(info.getQualityOfService());
    if (info.isSharedSubscription()) {
      builder.setSharedName(info.getSharedSubscriptionName());
    }
    builder.setAllowOverlap(true);
    builder.setNoLocalMessages(info.noLocalMessages());
    builder.setRetainAsPublish(info.isRetainAsPublished());
    builder.setRetainHandler(info.getRetainHandling());
    builder.setReceiveMaximum(receiveMaximum);
    if (selector != null) {
      builder.setSelector(selector);
    }
    if (info.getSubscriptionIdentifier() != null) {
      builder.setSubscriptionId(info.getSubscriptionIdentifier().getSubscriptionIdentifier());
    }
    return builder.build();
  }

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, Protocol protocol) {
    Subscribe5 subscribe = (Subscribe5) mqttPacket;
    List<SubscriptionInfo> subscriptionInfos = subscribe.getSubscriptionList();
    StatusCode[] result = new StatusCode[subscriptionInfos.size()];
    String selector = null;
    if (mqttPacket.getProperties() != null) {
      for (MessageProperty property : mqttPacket.getProperties().values()) {
        if (property instanceof UserProperty) {
          UserProperty userProperty = (UserProperty) property;
          if (userProperty.getUserPropertyName().equalsIgnoreCase("selector")) {
            selector = userProperty.getUserPropertyValue();
          }
        }
      }
    }
    for (int x = 0; x < subscriptionInfos.size(); x++) {
      SubscriptionInfo info = subscriptionInfos.get(x);
      //
      // Now add the subscription or wild card subscription
      //
      try {
        SubscriptionContext context = createContext(info, selector, session.getReceiveMaximum());
        session.addSubscription(context);
        result[x] = StatusCode.getInstance((byte) info.getQualityOfService().getLevel());
      } catch (IOException e) {
        result[x] = StatusCode.UNSPECIFIED_ERROR;
      }
    }
    return new SubAck5(subscribe.getMessageId(), result);
  }
}
