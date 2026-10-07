/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * Packet-identifier based MQTT-SN 2.0 acknowledgement wire representation.
 * Packet-specific optional fields are not assumed by this common codec.
 */
public final class MqttSn2AckCodec {

  private static final EnumSet<MqttSn2PacketType> SIMPLE_ACKS =
      EnumSet.of(MqttSn2PacketType.PUBACK, MqttSn2PacketType.PUBREC,
          MqttSn2PacketType.PUBREL, MqttSn2PacketType.PUBCOMP,
          MqttSn2PacketType.UNSUBACK);

  public record Ack(MqttSn2PacketType type, int packetIdentifier, Integer reasonCode) {
  }

  private MqttSn2AckCodec() {
  }

  public static Ack decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (!SIMPLE_ACKS.contains(frame.type())) {
      throw new IOException("Not a simple MQTT-SN 2.0 acknowledgement: " + frame.type());
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() != 2 && body.remaining() != 3) {
      throw new IOException("Invalid acknowledgement length");
    }
    int id = Short.toUnsignedInt(body.getShort());
    if (id == 0) {
      throw new IOException("Zero acknowledgement Packet Identifier");
    }
    return new Ack(frame.type(), id, body.hasRemaining() ? Byte.toUnsignedInt(body.get()) : null);
  }

  public static ByteBuffer encode(Ack ack) {
    if (!SIMPLE_ACKS.contains(ack.type())) {
      throw new IllegalArgumentException("Not a simple acknowledgement: " + ack.type());
    }
    if (ack.packetIdentifier() < 1 || ack.packetIdentifier() > 65535) {
      throw new IllegalArgumentException("Invalid acknowledgement Packet Identifier");
    }
    if (ack.reasonCode() != null && (ack.reasonCode() < 0 || ack.reasonCode() > 255)) {
      throw new IllegalArgumentException("Invalid acknowledgement Reason Code");
    }
    ByteBuffer body = ByteBuffer.allocate(ack.reasonCode() == null ? 2 : 3);
    body.putShort((short) ack.packetIdentifier());
    if (ack.reasonCode() != null) {
      body.put((byte) ack.reasonCode().intValue());
    }
    body.flip();
    return MqttSn2FrameCodec.encode(ack.type(), body);
  }
}
