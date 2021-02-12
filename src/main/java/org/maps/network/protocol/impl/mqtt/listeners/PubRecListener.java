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

package org.maps.network.protocol.impl.mqtt.listeners;

import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.MQTTProtocol;
import org.maps.network.protocol.impl.mqtt.PacketIdentifierMap;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt.packet.PubRec;
import org.maps.network.protocol.impl.mqtt.packet.PubRel;

public class PubRecListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    PubRec pubRec = (PubRec) mqttPacket;
    PacketIdentifierMap mapping = ((MQTTProtocol) protocol).getPacketIdManager().receivedPacket(pubRec.getPacketIdentifier());
    if (mapping != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
      return new PubRel(mapping.getPacketIdentifier());
    }
    throw new MalformedException("No known packet identifier found :" + pubRec.getPacketIdentifier());
  }
}
