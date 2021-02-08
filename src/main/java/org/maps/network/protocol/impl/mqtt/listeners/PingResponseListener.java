package org.maps.network.protocol.impl.mqtt.listeners;

import org.maps.logging.LogMessages;
import org.maps.messaging.api.Session;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;

public class PingResponseListener extends PacketListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) {
    logger.log(LogMessages.MQTT_PING);
    return null;
  }
}
