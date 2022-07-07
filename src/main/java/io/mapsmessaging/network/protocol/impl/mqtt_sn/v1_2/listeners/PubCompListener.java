package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet.MQTT_SNPacket;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.state.StateEngine;

public class PubCompListener extends PacketListener {

  @Override
  public MQTT_SNPacket handlePacket(MQTT_SNPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol, StateEngine stateEngine)
      throws MalformedException {
    stateEngine.sendNextPublish();
    return null;
  }
}