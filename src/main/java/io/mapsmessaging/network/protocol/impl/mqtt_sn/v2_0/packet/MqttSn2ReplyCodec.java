/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Server-side MQTT-SN 2.0 CSD01 SUBACK and REGACK wire responses. */
public final class MqttSn2ReplyCodec {

  public record SubAck(int topicType, Integer topicAlias, int packetIdentifier, Integer reasonCode) {}
  public record RegAck(int packetIdentifier, int topicAlias, Integer reasonCode) {}

  private MqttSn2ReplyCodec() {}

  public static ByteBuffer encodeSubAck(SubAck ack) {
    checkId(ack.packetIdentifier());
    if (ack.topicType() != 0 && ack.topicType() != 1) {
      throw new IllegalArgumentException("SUBACK must reference a topic alias");
    }
    if (ack.topicAlias() != null) checkId(ack.topicAlias());
    checkReason(ack.reasonCode());
    int flags = ack.topicType() | (ack.topicAlias() == null ? 0 : 4);
    ByteBuffer body = ByteBuffer.allocate(3 + (ack.topicAlias() == null ? 0 : 2)
        + (ack.reasonCode() == null ? 0 : 1));
    body.put((byte) flags).putShort((short) ack.packetIdentifier());
    if (ack.topicAlias() != null) body.putShort((short) ack.topicAlias().intValue());
    if (ack.reasonCode() != null) body.put((byte) ack.reasonCode().intValue());
    body.flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.SUBACK, body);
  }

  public static SubAck decodeSubAck(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = body(frame, MqttSn2PacketType.SUBACK);
    if (body.remaining() < 3) throw new IOException("Truncated SUBACK");
    int flags = Byte.toUnsignedInt(body.get());
    int topicType = flags & 3;
    if ((flags & 0xF8) != 0 || (topicType != 0 && topicType != 1)) {
      throw new IOException("Invalid SUBACK flags");
    }
    int id = readId(body);
    Integer alias = null;
    if ((flags & 4) != 0) {
      if (body.remaining() < 2) throw new IOException("Truncated SUBACK alias");
      alias = readId(body);
    }
    if (body.remaining() > 1) throw new IOException("Unexpected SUBACK trailing bytes");
    return new SubAck(topicType, alias, id, body.hasRemaining() ? Byte.toUnsignedInt(body.get()) : null);
  }

  private static ByteBuffer body(MqttSn2FrameCodec.Frame frame, MqttSn2PacketType type)
      throws IOException {
    if (frame.type() != type) throw new IOException("Expected " + type);
    return frame.payload().asReadOnlyBuffer();
  }

  private static int readId(ByteBuffer body) throws IOException {
    int id = Short.toUnsignedInt(body.getShort());
    if (id == 0) throw new IOException("Zero packet identifier or alias");
    return id;
  }

  private static void checkId(int id) {
    if (id < 1 || id > 65535) throw new IllegalArgumentException("Invalid packet identifier or alias");
  }

  private static void checkReason(Integer reason) {
    if (reason != null && (reason < 0 || reason > 255)) {
      throw new IllegalArgumentException("Invalid reason code");
    }
  }
}
