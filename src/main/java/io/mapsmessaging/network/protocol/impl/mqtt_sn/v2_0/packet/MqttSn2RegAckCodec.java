/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

/** MQTT-SN 2.0 CSD01 section 3.5 REGACK packet layout. */
public final class MqttSn2RegAckCodec {

  public record RegAck(int topicType, Integer topicAlias, int packetIdentifier, Integer reasonCode) {}

  private MqttSn2RegAckCodec() {}

  public static RegAck decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (frame.type() != MqttSn2PacketType.REGACK) throw new IOException("Expected REGACK");
    ByteBuffer data = frame.payload().asReadOnlyBuffer();
    if (data.remaining() < 3) throw new IOException("Truncated REGACK");
    int flags = Byte.toUnsignedInt(data.get());
    int type = flags & 3;
    if ((flags & 0xF8) != 0 || (type != 1 && type != 2)) {
      throw new IOException("Malformed REGACK flags");
    }
    int id = Short.toUnsignedInt(data.getShort());
    if (id == 0) throw new IOException("Zero REGACK Packet Identifier");
    Integer alias = null;
    if ((flags & 4) != 0) {
      if (data.remaining() < 2) throw new IOException("Truncated REGACK Topic Alias");
      alias = Short.toUnsignedInt(data.getShort());
      if (alias == 0) throw new IOException("Zero REGACK Topic Alias");
    }
    if (data.remaining() > 1) throw new IOException("REGACK trailing bytes");
    return new RegAck(type, alias, id, data.hasRemaining() ? Byte.toUnsignedInt(data.get()) : null);
  }

  public static ByteBuffer encode(RegAck ack) {
    if (ack.topicType() != 1 && ack.topicType() != 2) {
      throw new IllegalArgumentException("REGACK must specify an alias type");
    }
    checkId(ack.packetIdentifier());
    if (ack.topicAlias() != null) checkId(ack.topicAlias());
    if (ack.reasonCode() != null && (ack.reasonCode() < 0 || ack.reasonCode() > 255)) {
      throw new IllegalArgumentException("Invalid REGACK reason code");
    }
    ByteBuffer data = ByteBuffer.allocate(3 + (ack.topicAlias() == null ? 0 : 2)
        + (ack.reasonCode() == null ? 0 : 1));
    data.put((byte) (ack.topicType() | (ack.topicAlias() == null ? 0 : 4)));
    data.putShort((short) ack.packetIdentifier());
    if (ack.topicAlias() != null) data.putShort((short) ack.topicAlias().intValue());
    if (ack.reasonCode() != null) data.put((byte) ack.reasonCode().intValue());
    data.flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.REGACK, data);
  }

  private static void checkId(int id) {
    if (id < 1 || id > 65535) throw new IllegalArgumentException("Identifier must be 1..65535");
  }
}
