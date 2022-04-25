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

package io.mapsmessaging.network.protocol.impl.mqtt_sn2.state;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.WillMessageRequest;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.WillTopic;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.state.State;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.state.StateEngine;

/**
 * We expect a Will Topic Response here
 */
public class InitialWillTopicState implements State {

  private final MQTT_SNPacket lastPacket;

  InitialWillTopicState(MQTT_SNPacket lastPacket) {
    this.lastPacket = lastPacket;
  }

  @Override
  public MQTT_SNPacket handleMQTTEvent(MQTT_SNPacket mqtt, Session session, EndPoint endPoint, MQTT_SNProtocol protocol, StateEngine stateEngine) {
    if (mqtt.getControlPacketId() == MQTT_SNPacket.CONNECT) {
      return lastPacket; // Its a retransmit request
    }
    if (mqtt.getControlPacketId() == MQTT_SNPacket.WILLTOPIC) {
      // Awesome we are on track
      WillTopic willTopic = (WillTopic) mqtt;
      if (willTopic.getTopic() != null && willTopic.getTopic().length() > 0) {
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
