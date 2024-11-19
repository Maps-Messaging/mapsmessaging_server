/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdentifierMap;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRec5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.PubRel5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;

public class PubRecListener5 extends PacketListener5 {

  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    PubRec5 pubRec = (PubRec5) mqttPacket;
    PacketIdentifierMap mapping = ((MQTT5Protocol) protocol).getPacketIdManager().receivedPacket(pubRec.getPacketIdentifier());
    if (mapping != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
      return new PubRel5(mapping.getPacketIdentifier());
    }
    PubRel5 pubRel = new PubRel5(pubRec.getPacketIdentifier());
    pubRel.setStatusCode(StatusCode.PACKET_IDENTIFIER_NOT_FOUND);
    return pubRec;
  }
}
