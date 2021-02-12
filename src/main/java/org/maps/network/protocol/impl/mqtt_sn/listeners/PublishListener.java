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

package org.maps.network.protocol.impl.mqtt_sn.listeners;

import java.io.IOException;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.PubAck;
import org.maps.network.protocol.impl.mqtt_sn.packet.PubRec;
import org.maps.network.protocol.impl.mqtt_sn.packet.Publish;
import org.maps.network.protocol.impl.mqtt_sn.state.StateEngine;

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
          .setRetain(publish.retain())
          .setTransformation(protocol.getTransformation())
          .setOpaqueData(publish.getMessage());
      try {
        Destination destination = session.findDestination(topicName);
        if(destination != null) {
          destination.storeMessage(messageBuilder.build());
          if (publish.getQoS().equals(QualityOfService.AT_LEAST_ONCE)) {
            return new PubAck(publish.getTopicId(), publish.getMessageId(), 0);
          } else if (publish.getQoS().equals(QualityOfService.EXACTLY_ONCE)) {
            return new PubRec(publish.getMessageId());
          }
        }
        else{
          return new PubAck(publish.getTopicId(), publish.getMessageId(), MQTT_SNPacket.INVALID_TOPIC);
        }
      } catch (IOException e) {
        return new PubAck(publish.getTopicId(), publish.getMessageId(), MQTT_SNPacket.INVALID_TOPIC);
      }
    }
    return null;
  }
}
