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

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubRec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.PubAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Publish;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class PublishListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(
      MQTT_SNPacket mqttPacket,
      Session session,
      EndPoint endPoint,
      ProtocolImpl protocol,
      StateEngine stateEngine) {

    Publish publish = (Publish) mqttPacket;

    short topicId = (short) publish.getTopicId();
    QualityOfService qos = publish.getQoS();
    if (qos.equals(QualityOfService.MQTT_SN_REGISTERED)) {
      qos = QualityOfService.AT_MOST_ONCE;
    }
    String topicName = stateEngine.getTopic(topicId);
    if (topicName != null) {
      MessageBuilder messageBuilder = new MessageBuilder();
      messageBuilder.setQoS(qos)
          .setRetain(publish.isRetain())
          .setTransformation(protocol.getTransformation())
          .setOpaqueData(publish.getMessage());
      try {
        CompletableFuture<Destination> future = session.findDestination(topicName, DestinationType.TOPIC);
        future.thenApply(destination -> {
          if(destination != null) {
            try {
            destination.storeMessage(messageBuilder.build());
            } catch (IOException e) {
              ((MQTT_SNProtocol)protocol).writeFrame(new PubAck(publish.getMessageId(), ReasonCodes.InvalidTopicAlias));
            }
            if (publish.getQoS().equals(QualityOfService.AT_LEAST_ONCE)) {
              ((MQTT_SNProtocol)protocol).writeFrame( new PubAck(publish.getMessageId(), ReasonCodes.Success));
            } else if (publish.getQoS().equals(QualityOfService.EXACTLY_ONCE)) {
              ((MQTT_SNProtocol)protocol).writeFrame( new PubRec(publish.getMessageId()));
            }
          }
          else{
            ((MQTT_SNProtocol)protocol).writeFrame(new PubAck(publish.getMessageId(), ReasonCodes.InvalidTopicAlias));
          }
          return destination;
        });

      } catch (IOException e) {
        return new PubAck(publish.getMessageId(), ReasonCodes.InvalidTopicAlias);
      }
    }
    return null;
  }
}
