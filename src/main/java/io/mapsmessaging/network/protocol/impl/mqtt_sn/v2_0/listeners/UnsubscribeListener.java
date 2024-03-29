/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners.PacketListener;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.ReasonCodes;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.UnSubAck;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.Unsubscribe;

public class UnsubscribeListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine) {

    Unsubscribe unsubscribe = (Unsubscribe) mqttPacket;
    int id = unsubscribe.getTopicId();
    String topicName = unsubscribe.getTopicName();
    if (id != -1) {
      topicName = stateEngine.getTopicAliasManager().getTopic((short) id);
    }
    if (topicName != null) {
      stateEngine.removeSubscribeResponse(topicName);
      session.removeSubscription(topicName);
      return new UnSubAck(unsubscribe.getMsgId(), ReasonCodes.SUCCESS);
    }
    return new UnSubAck(unsubscribe.getMsgId(), ReasonCodes.INVALID_TOPIC_ALIAS);
  }
}
