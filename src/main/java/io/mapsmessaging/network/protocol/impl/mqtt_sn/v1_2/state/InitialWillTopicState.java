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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillMessageRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.WillTopic;

/**
 * We expect a Will Topic Response here
 */
public class InitialWillTopicState implements State {

  private final MQTT_SNPacket lastPacket;

  InitialWillTopicState(MQTT_SNPacket lastPacket) {
    this.lastPacket = lastPacket;
  }

  @Override
  public String getName() {
    return "WillTopic";
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.CONNECT) {
      return lastPacket; // Its a retransmit request
    }
    if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLTOPIC) {
      // Awesome we are on track
      WillTopic willTopic = (WillTopic) mqtt;
      if (willTopic.getTopic() != null && !willTopic.getTopic().isEmpty()) {
        SessionContextBuilder scb = stateEngine.getSessionContextBuilder();
        scb.setWillTopic(willTopic.getTopic());
      }
      WillMessageRequest request = new WillMessageRequest();
      stateEngine.setState(new InitialWillMessageState(request, willTopic.getQoS(), willTopic.getRetain()));
      return request;
    }
    return null;
  }

}
