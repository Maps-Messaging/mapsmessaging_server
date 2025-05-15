/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.UnsubAck;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Unsubscribe;

import java.util.List;

public class UnsubscribeListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(
      MQTTPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol) {
    Unsubscribe unsubscribe = (Unsubscribe) mqttPacket;
    List<String> unsubscribeList = unsubscribe.getUnsubscribeList();
    for (String info : unsubscribeList) {
      session.removeSubscription(info);
    }
    return new UnsubAck(unsubscribe.getPacketId());
  }
}
