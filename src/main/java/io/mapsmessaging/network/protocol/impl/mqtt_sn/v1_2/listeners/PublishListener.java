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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.*;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

public class PublishListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(
      MQTT_SNPacket mqttPacket,
      Session session,
      EndPoint endPoint,
      Protocol protocol,
      StateEngine stateEngine) {

    Publish publish = (Publish) mqttPacket;

    QualityOfService qos = publish.getQoS();
    if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
      qos = QualityOfService.AT_MOST_ONCE;
    }
    String topicName = stateEngine.getTopicAliasManager().getTopic(mqttPacket.getFromAddress(), publish.getTopicId(), publish.getTopicIdType());
    if (topicName != null
        && (!topicName.startsWith("$")
            || topicName.toLowerCase().startsWith(DestinationMode.SCHEMA.getNamespace()))) {
      processValidMessage(session, qos, publish, protocol, topicName);
    }
    return null;
  }

  private void processValidMessage(Session session, QualityOfService qos, Publish publish, Protocol protocol, String topicName){
    HashMap<String, String> meta = new LinkedHashMap<>();
    meta.put("protocol", "MQTT-SN");
    meta.put("version", "1.2");
    meta.put("sessionId", session.getName());
    meta.put("time_ms", "" + System.currentTimeMillis());


    MessageBuilder messageBuilder = new MessageBuilder();
    messageBuilder.setQoS(qos)
        .setMeta(meta)
        .setRetain(publish.retain())
        .setTransformation(protocol.getProtocolMessageTransformation())
        .setOpaqueData(publish.getMessage());
    CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
    future.thenApply(destination -> {
      if (destination != null) {
        Message message =  MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build();
        try {
          if (message.getQualityOfService().getLevel() == 2) {
            Transaction transaction = session.startTransaction(session.getName() + ":" + publish.getMessageId());
            transaction.add(destination, message);
          } else {
            destination.storeMessage(message);
          }
        } catch (IOException e) {
          ((MQTT_SNProtocol) protocol).writeFrame(new PubAck(publish.getTopicId(), publish.getMessageId(), ReasonCodes.INVALID_TOPIC_ALIAS));
        }
        if (publish.getQoS().equals(QualityOfService.AT_LEAST_ONCE)) {
          ((MQTT_SNProtocol) protocol).writeFrame(new PubAck(publish.getTopicId(), publish.getMessageId(), ReasonCodes.SUCCESS));
        } else if (publish.getQoS().equals(QualityOfService.EXACTLY_ONCE)) {
          ((MQTT_SNProtocol) protocol).writeFrame(new PubRec(publish.getMessageId()));
        }
      } else {
        ((MQTT_SNProtocol) protocol).writeFrame(new PubAck(publish.getTopicId(), publish.getMessageId(), ReasonCodes.INVALID_TOPIC_ALIAS));
      }
      return destination;
    });
  }
}
