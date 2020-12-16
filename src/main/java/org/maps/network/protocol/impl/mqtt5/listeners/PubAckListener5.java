/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.mqtt5.listeners;

import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.PacketIdentifierMap;
import org.maps.network.protocol.impl.mqtt5.MQTT5Protocol;
import org.maps.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import org.maps.network.protocol.impl.mqtt5.packet.PubAck5;

public class PubAckListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    PubAck5 pubAck = (PubAck5) mqttPacket;
    PacketIdentifierMap mapping = ((MQTT5Protocol) protocol).getPacketIdManager().completePacketId(pubAck.getPacketIdentifier());
    if (mapping != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
    }
    return null;
  }
}
