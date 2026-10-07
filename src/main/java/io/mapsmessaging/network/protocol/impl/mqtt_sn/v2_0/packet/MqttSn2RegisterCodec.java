/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** MQTT-SN 2.0 REGISTER request wire format (CSD01). */
public final class MqttSn2RegisterCodec {

  public record Register(int packetIdentifier, String topicName) {
  }

  private MqttSn2RegisterCodec() {
  }

  public static Register decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (frame.type() != MqttSn2PacketType.REGISTER) {
      throw new IOException("Expected REGISTER");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 4) {
      throw new IOException("Truncated REGISTER");
    }
    int flags = Byte.toUnsignedInt(body.get());
    if (flags != 0) {
      throw new IOException("Reserved REGISTER flags must be zero");
    }
    int id = Short.toUnsignedInt(body.getShort());
    if (id == 0) {
      throw new IOException("REGISTER Packet Identifier must be nonzero");
    }
    byte[] bytes = new byte[body.remaining()];
    body.get(bytes);
    return new Register(id, topic(bytes));
  }

  public static ByteBuffer encode(Register request) {
    if (request.packetIdentifier() < 1 || request.packetIdentifier() > 65535) {
      throw new IllegalArgumentException("Invalid REGISTER Packet Identifier");
    }
    String name = request.topicName();
    if (name == null || name.isEmpty() || name.indexOf('#') >= 0
        || name.indexOf('+') >= 0 || name.indexOf('\u0000') >= 0) {
      throw new IllegalArgumentException("Invalid REGISTER Topic Name");
    }
    byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
    ByteBuffer body = ByteBuffer.allocate(3 + bytes.length);
    body.put((byte) 0).putShort((short) request.packetIdentifier()).put(bytes).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.REGISTER, body);
  }

  private static String topic(byte[] bytes) throws IOException {
    try {
      String name = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes)).toString();
      if (name.isEmpty() || name.indexOf('#') >= 0 || name.indexOf('+') >= 0
          || name.indexOf('\u0000') >= 0) {
        throw new IOException("Invalid REGISTER Topic Name");
      }
      return name;
    } catch (CharacterCodingException e) {
      throw new IOException("Invalid REGISTER UTF-8", e);
    }
  }
}
