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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.Register;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.RegisterAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.state.StateEngine;
import java.io.IOException;

public class RegisterListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {
    Register register = (Register) mqttPacket;
    String topic = register.getTopic();
    short topicId = stateEngine.getTopicAlias(topic);
    if (topicId == -1) {
      // Exceeded the maximum number of registered topics
      return new RegisterAck(topicId, register.getMessageId(), MQTT_SNPacket.NOT_SUPPORTED);
    }
    try {
      session.findDestination(topic, DestinationType.TOPIC);
      // We don't need to do anything with this destination at present
    } catch (IOException e) {
      return new RegisterAck(topicId, register.getMessageId(), MQTT_SNPacket.NOT_SUPPORTED);
    }
    return new RegisterAck(topicId, register.getMessageId(), MQTT_SNPacket.ACCEPTED);
  }
}
