/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdentifierMap;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubComp;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class PubCompListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, Protocol protocol, StateEngine stateEngine)
      throws MalformedException {
    int packetId = ((PubComp) mqttPacket).getMessageId();
    MQTT_SNProtocol mqttSn = (MQTT_SNProtocol) protocol;
    if (!mqttSn.getPacketIdManager().isAwaitingPubComp(packetId) || mqttSn.isEngineAckPending(packetId)) {
      throw new MalformedException("No PUBREL outstanding for Packet Identifier " + packetId);
    }
    if (mqttSn.completeRecoveredPubRel(packetId)) {
      return null;
    }
    PacketIdentifierMap mapping = mqttSn.getPacketIdManager().completePacketId(packetId);
    if (!mqttSn.consumeReconciledDelivery(packetId) && mapping != null && mapping.getSubscription() != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
    }
    stateEngine.sendNextPublish();
    return null;
  }
}