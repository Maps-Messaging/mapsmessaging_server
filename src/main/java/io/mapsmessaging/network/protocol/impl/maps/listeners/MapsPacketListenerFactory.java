/*
 * Copyright [ 2020 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.network.protocol.impl.maps.listeners;

import io.mapsmessaging.network.protocol.impl.maps.MapsPacketType;
import java.util.EnumMap;
import java.util.Map;

public final class MapsPacketListenerFactory {

  private final Map<MapsPacketType, MapsPacketListener> listeners = new EnumMap<>(MapsPacketType.class);

  public MapsPacketListenerFactory() {
    listeners.put(MapsPacketType.CONNECT, new ConnectListener());
    listeners.put(MapsPacketType.CONNACK, new ConnAckListener());
    listeners.put(MapsPacketType.SUBSCRIBE, new SubscribeListener());
    listeners.put(MapsPacketType.SUBACK, new AckListener());
    listeners.put(MapsPacketType.UNSUBSCRIBE, new UnsubscribeListener());
    listeners.put(MapsPacketType.UNSUBACK, new AckListener());
    listeners.put(MapsPacketType.PUBLISH, new PublishListener());
    listeners.put(MapsPacketType.PUBACK, new AckListener());
    listeners.put(MapsPacketType.PING, new PingListener());
    listeners.put(MapsPacketType.PONG, new PongListener());
    listeners.put(MapsPacketType.DISCONNECT, new DisconnectListener());
  }

  public MapsPacketListener get(MapsPacketType type) {
    return listeners.get(type);
  }
}
