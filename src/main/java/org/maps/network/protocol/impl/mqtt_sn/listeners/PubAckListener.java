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

package org.maps.network.protocol.impl.mqtt_sn.listeners;

import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.PacketIdentifierMap;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt_sn.MQTT_SNProtocol;
import org.maps.network.protocol.impl.mqtt_sn.packet.MQTT_SNPacket;
import org.maps.network.protocol.impl.mqtt_sn.packet.PubAck;
import org.maps.network.protocol.impl.mqtt_sn.state.StateEngine;

public class PubAckListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine)
      throws MalformedException {

    PubAck pubAck = (PubAck) mqttPacket;
    PacketIdentifierMap mapping = ((MQTT_SNProtocol) protocol).getPacketIdManager().completePacketId(pubAck.getMessageId());
    if (mapping != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
    } else {
      throw new MalformedException("No such Packet Identifier found " + pubAck.getMessageId());
    }
    return null;
  }
}