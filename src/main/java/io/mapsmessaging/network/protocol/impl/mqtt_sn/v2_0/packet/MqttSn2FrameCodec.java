/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Transport-independent MQTT-SN 2.0 packet framing (CSD01, section 2.1.2).
 * Parsing never consumes the caller's buffer and never uses the 1.2 packet factory.
 */
public final class MqttSn2FrameCodec {

  public static final int MAX_PACKET_LENGTH = 65535;

  private MqttSn2FrameCodec() {
  }

  public record Frame(MqttSn2PacketType type, ByteBuffer payload, int packetLength) {
  }

  public static Frame decode(ByteBuffer bytes) throws IOException {
    Objects.requireNonNull(bytes, "bytes");
    ByteBuffer input = bytes.asReadOnlyBuffer();
    if (!input.hasRemaining()) {
      throw new IOException("Missing MQTT-SN 2.0 length");
    }

    int first = Byte.toUnsignedInt(input.get());
    int length;
    int header;
    if (first == 1) {
      if (input.remaining() < 3) {
        throw new IOException("Truncated MQTT-SN 2.0 extended header");
      }
      length = Short.toUnsignedInt(input.getShort());
      header = 4;
    } else {
      length = first;
      header = 2;
    }

    if (length < header) {
      throw new IOException("MQTT-SN 2.0 length shorter than header");
    }
    if (length != bytes.remaining()) {
      throw new IOException("MQTT-SN 2.0 frame length mismatch");
    }

    MqttSn2PacketType type;
    try {
      type = MqttSn2PacketType.fromCode(Byte.toUnsignedInt(input.get()));
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid MQTT-SN 2.0 packet type", e);
    }
    return new Frame(type, input.slice().asReadOnlyBuffer(), length);
  }

  public static ByteBuffer encode(MqttSn2PacketType type, ByteBuffer payload) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(payload, "payload");
    ByteBuffer body = payload.asReadOnlyBuffer();
    int shortLength = body.remaining() + 2;
    boolean extended = shortLength > 255;
    int length = body.remaining() + (extended ? 4 : 2);
    if (length > MAX_PACKET_LENGTH) {
      throw new IllegalArgumentException("MQTT-SN 2.0 packet too large");
    }
    ByteBuffer result = ByteBuffer.allocate(length);
    if (extended) {
      result.put((byte) 1).putShort((short) length);
    } else {
      result.put((byte) length);
    }
    result.put((byte) type.code()).put(body).flip();
    return result.asReadOnlyBuffer();
  }
}
