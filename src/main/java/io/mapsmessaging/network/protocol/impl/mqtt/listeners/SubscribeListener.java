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

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.*;

import java.io.IOException;
import java.util.List;

public class SubscribeListener extends PacketListener {

  protected static SubscriptionContext createContext(String destinationName, QualityOfService qos, int receiveMaximum) {
    ClientAcknowledgement ackManger = qos.getClientAcknowledgement();
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(destinationName, ackManger);
    builder.setQos(qos);
    builder.setAllowOverlap(true);
    builder.setReceiveMaximum(receiveMaximum);
    return builder.build();
  }

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) throws MalformedException {
    checkState(session);
    Subscribe subscribe = (Subscribe) mqttPacket;
    List<SubscriptionInfo> subscriptionInfos = subscribe.getSubscriptionList();
    byte[] result = new byte[subscriptionInfos.size()];
    for (int x = 0; x < subscriptionInfos.size(); x++) {
      SubscriptionInfo info = subscriptionInfos.get(x);
      //
      // Now add the subscription or wild card subscription
      //
      try {
        SubscriptionContext context = createContext(info.getTopicName(), info.getQualityOfService(), session.getReceiveMaximum());
        session.addSubscription(context);
        result[x] = (byte) info.getQualityOfService().getLevel();
      } catch (IOException e) {
        result[x] = (byte) 0x80;
      }
    }
    return new SubAck(subscribe.getMessageId(), result);
  }
}
