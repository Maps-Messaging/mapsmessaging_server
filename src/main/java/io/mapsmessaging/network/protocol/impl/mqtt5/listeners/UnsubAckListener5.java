/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdentifierMap;
import io.mapsmessaging.network.protocol.impl.mqtt.PacketIdManager;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.MQTTPacket5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.UnsubAck5;

public class UnsubAckListener5 extends PacketListener5 {
  @Override
  public MQTTPacket5 handlePacket(MQTTPacket5 packet, Session session, EndPoint endPoint, Protocol protocol) {
    int identifier = ((UnsubAck5) packet).getPacketId();
    PacketIdManager manager = ((MQTT5Protocol) protocol).getPacketIdManager();
    PacketIdentifierMap mapping = manager.receivedPacket(identifier);
    if (mapping != null && mapping.getSubscription() == null && mapping.getMessageId() == -1) manager.completePacketId(identifier);
    return null;
  }
}
