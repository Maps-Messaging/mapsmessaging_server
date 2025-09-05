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

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.Register;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.RegisterAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class RegisterListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol, StateEngine stateEngine) {
    Register register = (Register) mqttPacket;
    String topic = register.getTopic();
    short topicId = stateEngine.getTopicAliasManager().getTopicAlias(topic);
    if (topicId == -1) {
      // Exceeded the maximum number of registered topics
      return new RegisterAck(topicId, register.getMessageId(), ReasonCodes.NOT_SUPPORTED);
    }
    session.findDestination(topic, DestinationType.TOPIC);
    // We don't need to do anything with this destination at present
    return new RegisterAck(topicId, register.getMessageId(), ReasonCodes.SUCCESS);
  }
}
