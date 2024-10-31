/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.UnsubAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Unsubscribe5;
import java.util.List;

public class UnsubscribeListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    Unsubscribe5 unsubscribe = (Unsubscribe5) mqttPacket;
    List<String> unsubscribeList = unsubscribe.getUnsubscribeList();
    for (String info : unsubscribeList) {
      session.removeSubscription(info);
    }
    return new UnsubAck5(unsubscribe.getPacketId());
  }
}
