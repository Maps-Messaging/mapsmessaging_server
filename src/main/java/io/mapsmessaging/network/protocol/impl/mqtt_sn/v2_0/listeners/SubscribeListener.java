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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.config.protocol.impl.MqttSnConfig;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.SubAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Subscribe;

import java.io.IOException;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.*;

public class SubscribeListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol, StateEngine stateEngine) {

    Subscribe subscribe = (Subscribe) mqttPacket;
    String topicName;
    short topicId = 0;

    if (subscribe.getTopicIdType() == LONG_TOPIC_NAME ||
        subscribe.getTopicIdType() == TOPIC_NAME) {
      topicName = subscribe.getTopicName();
    } else {
      topicName = stateEngine.getTopicAliasManager().getTopic(mqttPacket.getFromAddress(), subscribe.getTopicId(), subscribe.getTopicIdType());
      if (subscribe.getTopicIdType() == TOPIC_PRE_DEFINED_ID) {
        topicId = (short) subscribe.getTopicIdType();
      }
    }
    if (topicName != null) {
      //
      // Handle packet retransmit on a subscribe
      //
      if (stateEngine.isSubscribed(topicName)) {
        return stateEngine.getPreviousResponse(topicName);
      }

      //
      // Do NOT register wildcard subscriptions
      //
      short topicIdType = (short) subscribe.getTopicIdType();
      if (topicName.contains("+") ||
          topicName.contains("#")) {
        topicId = stateEngine.getTopicAliasManager().getTopicAlias(topicName);
        topicIdType = 0;
      }
      else if(topicId == 0 ){
        topicId = stateEngine.getTopicAliasManager().getTopicAlias(topicName);
        topicIdType = 0;
      }
      ClientAcknowledgement ackManger = subscribe.getQos().getClientAcknowledgement();
      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(topicName, ackManger);
      int receiveMax = ((MqttSnConfig)endPoint.getConfig().getProtocolConfig("mqtt-sn")).getReceiveMaximum();
      builder.setReceiveMaximum(receiveMax);
      builder.setNoLocalMessages(subscribe.isNoLocal());
      builder.setRetainHandler(subscribe.getRetainHandler());
      builder.setQos(subscribe.getQos());
      try {
        SubscriptionContext context = builder.build();
        session.addSubscription(context);
        SubAck subAck = new SubAck(
            topicId, topicIdType,
            subscribe.getQos(),
            subscribe.getMsgId(),
            ReasonCodes.SUCCESS);
        subAck.setQoS(subscribe.getQos());
        stateEngine.addSubscribeResponse(topicName, subAck);
        return subAck;
      } catch (IOException e) {
        SubAck subAck = new SubAck(
            topicId, TOPIC_NAME,
            subscribe.getQos(),
            subscribe.getMsgId(),
            ReasonCodes.INVALID_TOPIC_ALIAS);
        subAck.setQoS(subscribe.getQos());
        return subAck;
      }
    } else {
      SubAck subAck = new SubAck(
          (short) 0, TOPIC_NAME,
          subscribe.getQos(),
          subscribe.getMsgId(),
          ReasonCodes.INVALID_TOPIC_ALIAS);
      subAck.setQoS(subscribe.getQos());
      return subAck;
    }
  }
}
