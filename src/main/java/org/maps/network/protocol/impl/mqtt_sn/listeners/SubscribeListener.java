/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt_sn.listeners;

import java.io.IOException;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt_sn.DefaultConstants;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.SubAck;
import org.maps.network.protocol.impl.mqtt_sn.packet.Subscribe;
import org.maps.network.protocol.impl.mqtt_sn.state.StateEngine;

public class SubscribeListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {

    Subscribe subscribe = (Subscribe) mqttPacket;

    String topicName;
    if (subscribe.topicIdType() == MQTT_SNPacket.TOPIC_NAME) {
      topicName = subscribe.getTopicName();
    } else {
      topicName = stateEngine.getTopic(subscribe.getTopicId());
    }
    if (topicName != null) {
      //
      // Handle packet retransmit on a subscribe
      //
      if (stateEngine.isSubscribed(topicName)) {
        return stateEngine.getPreviousResponse(topicName);
      }

      //
      // Now map the topic name to a short
      //

      short topicId = 0;
      //
      // Do NOT register wildcard subscriptions
      //
      if (!(topicName.contains("+") || topicName.contains("#"))) {
        topicId = stateEngine.getTopicAlias(topicName);
      }
      ClientAcknowledgement ackManger = subscribe.getQoS().getClientAcknowledgement();
      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(topicName, ackManger);
      builder.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
      builder.setQos(subscribe.getQoS());

      try {
        SubscriptionContext context = builder.build();
        session.addSubscription(context);
        SubAck subAck = new SubAck(topicId, subscribe.getMsgId(), (byte) SubAck.ACCEPTED);
        subAck.setQoS(subscribe.getQoS());
        stateEngine.addSubscribeResponse(topicName, subAck);
        return subAck;
      } catch (IOException e) {
        SubAck subAck = new SubAck(topicId, subscribe.getMsgId(), (byte) MQTT_SNPacket.INVALID_TOPIC);
        subAck.setQoS(subscribe.getQoS());
        return subAck;
      }
    } else {
      SubAck subAck = new SubAck((short) 0, subscribe.getMsgId(), (byte) MQTT_SNPacket.INVALID_TOPIC);
      subAck.setQoS(subscribe.getQoS());
      return subAck;
    }
  }
}
