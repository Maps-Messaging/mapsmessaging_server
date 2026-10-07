/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * MQTT-SN 2.0 CSD01 control packet payloads independent of MQTT-SN 1.2.
 * Initial scope: PINGREQ, PINGRESP and AUTH.
 */
public final class MqttSn2ControlCodec {

  public record Auth(int packetIdentifier, int reasonCode, String mechanism, byte[] data) {
    public Auth {
      if (packetIdentifier < 1 || packetIdentifier > 65535 || reasonCode < 0 || reasonCode > 255) {
        throw new IllegalArgumentException("Invalid AUTH identifier or reason code");
      }
      Objects.requireNonNull(mechanism, "mechanism");
      data = Objects.requireNonNull(data, "data").clone();
    }

    @Override
    public byte[] data() {
      return data.clone();
    }
  }

  public record PingResponse(int packetIdentifier, Integer remainingMessages) {
  }

  private MqttSn2ControlCodec() {
  }

  public static ByteBuffer encodePingRequest(int identifier) {
    checkIdentifier(identifier);
    ByteBuffer body = ByteBuffer.allocate(2);
    body.putShort((short) identifier).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGREQ, body);
  }

  public static int decodePingRequest(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = requireBody(frame, MqttSn2PacketType.PINGREQ);
    if (body.remaining() != 2) {
      throw new IOException("PINGREQ must contain a two-octet Packet Identifier");
    }
    return requirePacketIdentifier(Short.toUnsignedInt(body.getShort()));
  }

  public static ByteBuffer encodePingResponse(int identifier, Integer remainingMessages) {
    checkIdentifier(identifier);
    if (remainingMessages != null && (remainingMessages < 0 || remainingMessages > 255)) {
      throw new IllegalArgumentException("Invalid PINGRESP remaining messages");
    }
    ByteBuffer body = ByteBuffer.allocate(remainingMessages == null ? 2 : 3);
    body.putShort((short) identifier);
    if (remainingMessages != null) {
      body.put((byte) (int) remainingMessages);
    }
    body.flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.PINGRESP, body);
  }

  public static PingResponse decodePingResponse(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = requireBody(frame, MqttSn2PacketType.PINGRESP);
    if (body.remaining() != 2 && body.remaining() != 3) {
      throw new IOException("Invalid PINGRESP payload length");
    }
    int identifier = requirePacketIdentifier(Short.toUnsignedInt(body.getShort()));
    return new PingResponse(identifier, body.hasRemaining() ? Byte.toUnsignedInt(body.get()) : null);
  }

  public static Auth decodeAuth(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = requireBody(frame, MqttSn2PacketType.AUTH);
    if (body.remaining() < 4) {
      throw new IOException("Truncated AUTH");
    }
    int identifier = requirePacketIdentifier(Short.toUnsignedInt(body.getShort()));
    int reason = Byte.toUnsignedInt(body.get());
    int methodLength = Byte.toUnsignedInt(body.get());
    if (body.remaining() < methodLength) {
      throw new IOException("Truncated AUTH mechanism");
    }
    byte[] method = new byte[methodLength];
    body.get(method);
    String mechanism;
    try {
      mechanism = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(method)).toString();
    } catch (CharacterCodingException e) {
      throw new IOException("Invalid AUTH mechanism UTF-8", e);
    }
    byte[] data = new byte[body.remaining()];
    body.get(data);
    return new Auth(identifier, reason, mechanism, data);
  }

  public static ByteBuffer encodeAuth(Auth auth) {
    Objects.requireNonNull(auth, "auth");
    byte[] method = auth.mechanism().getBytes(StandardCharsets.UTF_8);
    if (method.length > 255) {
      throw new IllegalArgumentException("AUTH mechanism exceeds one-octet length");
    }
    byte[] data = auth.data();
    ByteBuffer body = ByteBuffer.allocate(4 + method.length + data.length);
    body.putShort((short) auth.packetIdentifier()).put((byte) auth.reasonCode())
        .put((byte) method.length).put(method).put(data).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.AUTH, body);
  }

  private static ByteBuffer requireBody(MqttSn2FrameCodec.Frame frame, MqttSn2PacketType type)
      throws IOException {
    Objects.requireNonNull(frame, "frame");
    if (frame.type() != type) {
      throw new IOException("Expected " + type + " but received " + frame.type());
    }
    return frame.payload().asReadOnlyBuffer();
  }

  private static int requirePacketIdentifier(int identifier) throws IOException {
    if (identifier == 0) {
      throw new IOException("MQTT-SN 2.0 Packet Identifier must be non-zero");
    }
    return identifier;
  }

  private static void checkIdentifier(int identifier) {
    if (identifier < 1 || identifier > 65535) {
      throw new IllegalArgumentException("Packet Identifier must be 1..65535");
    }
  }
}
