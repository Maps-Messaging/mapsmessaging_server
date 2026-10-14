/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt.listeners;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.UnsubAck;

public class UnsubAckListener extends PacketListener {
  @Override
  public MQTTPacket handlePacket(MQTTPacket packet, Session session, EndPoint endPoint, Protocol protocol) {
    int identifier = ((UnsubAck) packet).getPacketId();
    var manager = ((MQTTProtocol) protocol).getPacketIdManager();
    var mapping = manager.receivedPacket(identifier);
    if (mapping != null && mapping.getSubscription() == null && mapping.getMessageId() == -1) manager.completePacketId(identifier);
    return null;
  }
}
