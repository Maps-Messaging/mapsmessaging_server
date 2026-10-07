/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2FrameCodec;
import io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet.MqttSn2PacketType;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn2OutboundPacketTest {

  @Test
  void sendsExactV2BytesAndDestinationWithoutUsingLegacyPackets() {
    InetSocketAddress destination = new InetSocketAddress("127.0.0.1", 1884);
    AtomicInteger completions = new AtomicInteger();
    ByteBuffer wire = MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGRESP,
        ByteBuffer.wrap(new byte[]{0, 42}));
    MqttSn2OutboundPacket outbound = new MqttSn2OutboundPacket(wire, destination,
        completions::incrementAndGet);
    Packet packet = new Packet(4, false);
    assertEquals(4, outbound.packFrame(packet));
    packet.flip();
    assertEquals(destination, outbound.getFromAddress());
    assertEquals(destination, packet.getFromAddress());
    assertArrayEquals(new byte[]{4, 13, 0, 42}, packet.getRawBuffer().array());
    outbound.complete();
    assertEquals(1, completions.get());
  }

  @Test
  void refusesUndersizedOutputBuffer() {
    MqttSn2OutboundPacket outbound = new MqttSn2OutboundPacket(
        ByteBuffer.wrap(new byte[]{4, 13, 0, 42}),
        new InetSocketAddress("127.0.0.1", 1884), null);
    assertThrows(IllegalArgumentException.class, () ->
        outbound.packFrame(new Packet(2, false)));
  }
}
