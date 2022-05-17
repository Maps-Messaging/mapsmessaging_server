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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners;

import static io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket.TOPIC_NAME;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.DefaultConstants;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MQTT_SN_2_Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.SubAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Subscribe;
import java.io.IOException;

public class SubscribeListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {

    Subscribe subscribe = (Subscribe) mqttPacket;
    String topicName;
    if(subscribe.getTopicIdType() == MQTT_SN_2_Packet.LONG_TOPIC_NAME ||
        subscribe.getTopicIdType() == TOPIC_NAME){
      topicName = subscribe.getTopicName();
    }
    else{
      topicName = stateEngine.getTopicAliasManager().getTopic(mqttPacket.getFromAddress(), subscribe.getTopicId(), subscribe.getTopicIdType());
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
        topicId = stateEngine.getTopicAliasManager().getTopicAlias(topicName);
      }
      ClientAcknowledgement ackManger = subscribe.getQoS().getClientAcknowledgement();
      SubscriptionContextBuilder builder = new SubscriptionContextBuilder(topicName, ackManger);
      builder.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
      builder.setNoLocalMessages(subscribe.isNoLocal());
      builder.setRetainHandler(subscribe.getRetainHandler());
      builder.setQos(subscribe.getQoS());
      try {
        SubscriptionContext context = builder.build();
        session.addSubscription(context);
        SubAck subAck = new SubAck(
            topicId,  TOPIC_NAME,
            subscribe.getQoS(),
            subscribe.getMsgId(),
            ReasonCodes.Success);
        subAck.setQoS(subscribe.getQoS());
        stateEngine.addSubscribeResponse(topicName, subAck);
        return subAck;
      } catch (IOException e) {
        SubAck subAck = new SubAck(
            topicId,  TOPIC_NAME,
            subscribe.getQoS(),
            subscribe.getMsgId(),
            ReasonCodes.InvalidTopicAlias);
        subAck.setQoS(subscribe.getQoS());
        return subAck;
      }
    } else {
      SubAck subAck = new SubAck(
          (short)0,  TOPIC_NAME,
          subscribe.getQoS(),
          subscribe.getMsgId(),
          ReasonCodes.InvalidTopicAlias);
      subAck.setQoS(subscribe.getQoS());
      return subAck;
    }
  }
}
