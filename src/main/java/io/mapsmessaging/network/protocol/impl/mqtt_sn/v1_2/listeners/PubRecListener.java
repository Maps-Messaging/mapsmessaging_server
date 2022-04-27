package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdentifierMap;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.MQTT_SNProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubRec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.PubRel;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class PubRecListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine)
      throws MalformedException {

    PubRec pubRec = (PubRec) mqttPacket;
    PacketIdentifierMap mapping = ((MQTT_SNProtocol) protocol).getPacketIdManager().completePacketId(pubRec.getMessageId());
    if (mapping != null) {
      mapping.getSubscription().ackReceived(mapping.getMessageId());
    } else {
      throw new MalformedException("No such Packet Identifier found " + pubRec.getMessageId());
    }
    return new PubRel(pubRec.getMessageId());
  }
}