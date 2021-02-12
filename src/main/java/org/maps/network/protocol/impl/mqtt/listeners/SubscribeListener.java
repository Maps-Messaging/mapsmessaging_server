/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.protocol.impl.mqtt.listeners;

import java.io.IOException;
import java.util.List;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.SubAck;
import org.maps.network.protocol.impl.mqtt.packet.Subscribe;
import org.maps.network.protocol.impl.mqtt.packet.SubscriptionInfo;

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
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
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
