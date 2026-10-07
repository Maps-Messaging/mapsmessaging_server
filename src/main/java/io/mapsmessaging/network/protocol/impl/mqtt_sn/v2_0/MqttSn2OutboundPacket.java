/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.ServerPacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transport adapter for independently encoded MQTT-SN 2.0 wire frames.
 * Does not extend or reference MQTT-SN 1.2 packets.
 */
public final class MqttSn2OutboundPacket implements ServerPacket {

  private final byte[] wire;
  private final SocketAddress destination;
  private final AtomicReference<Runnable> completion;

  public MqttSn2OutboundPacket(ByteBuffer frame, SocketAddress destination, Runnable completion) {
    Objects.requireNonNull(frame, "frame");
    ByteBuffer data = frame.asReadOnlyBuffer();
    wire = new byte[data.remaining()];
    data.get(wire);
    this.destination = Objects.requireNonNull(destination, "destination");
    this.completion = new AtomicReference<>(completion);
  }

  @Override
  public int packFrame(Packet packet) {
    if (packet.limit() - packet.position() < wire.length) {
      throw new IllegalArgumentException("Insufficient outbound MQTT-SN 2.0 packet capacity");
    }
    packet.put(wire);
    packet.setFromAddress(destination);
    return wire.length;
  }

  @Override
  public void complete() {
    Runnable callback = completion.getAndSet(null);
    if (callback != null) {
      callback.run();
    }
  }

  @Override
  public SocketAddress getFromAddress() {
    return destination;
  }
}
