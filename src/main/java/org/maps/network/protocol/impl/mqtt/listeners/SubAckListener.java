package org.maps.network.protocol.impl.mqtt.listeners;

import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.MQTTProtocol;
import org.maps.network.protocol.impl.mqtt.PacketIdentifierMap;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.impl.mqtt.packet.SubAck;

public class SubAckListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {
    SubAck subAck = (SubAck) mqttPacket;
    PacketIdentifierMap mapping = ((MQTTProtocol) protocol).getPacketIdManager().completePacketId(subAck.getPacketId());
    return null;
  }
}
