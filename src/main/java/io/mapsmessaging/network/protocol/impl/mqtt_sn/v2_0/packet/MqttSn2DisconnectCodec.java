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

/** MQTT-SN 2.0 CSD01 DISCONNECT flags and optional fields. */
public final class MqttSn2DisconnectCodec {

  public record Disconnect(Integer packetIdentifier, Integer reasonCode, Long sessionExpiry,
                           String reasonString) {}

  private MqttSn2DisconnectCodec() {}

  public static Disconnect decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (frame.type() != MqttSn2PacketType.DISCONNECT) throw new IOException("Expected DISCONNECT");
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (!body.hasRemaining()) throw new IOException("DISCONNECT flags missing");
    int flags = Byte.toUnsignedInt(body.get());
    if ((flags & 0xF8) != 0) throw new IOException("Reserved DISCONNECT flags");
    Integer packetIdentifier = null;
    Integer reasonCode = null;
    Long expiry = null;
    if ((flags & 1) != 0) {
      if (body.remaining() < 2) throw new IOException("Truncated DISCONNECT packet identifier");
      packetIdentifier = Short.toUnsignedInt(body.getShort());
      if (packetIdentifier == 0) throw new IOException("Zero DISCONNECT packet identifier");
    }
    if ((flags & 4) != 0) {
      if (!body.hasRemaining()) throw new IOException("Truncated DISCONNECT reason code");
      reasonCode = Byte.toUnsignedInt(body.get());
    }
    if ((flags & 2) != 0) {
      if (body.remaining() < 4) throw new IOException("Truncated DISCONNECT expiry");
      expiry = Integer.toUnsignedLong(body.getInt());
    }
    String message;
    try {
      message = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(body).toString();
    } catch (CharacterCodingException e) {
      throw new IOException("Invalid DISCONNECT reason string", e);
    }
    if (message.indexOf('\u0000') >= 0) throw new IOException("Invalid DISCONNECT reason string");
    return new Disconnect(packetIdentifier, reasonCode, expiry, message);
  }

  public static ByteBuffer encode(Disconnect packet) {
    int flags = 0;
    int length = 1;
    if (packet.packetIdentifier() != null) {
      if (packet.packetIdentifier() < 1 || packet.packetIdentifier() > 65535) {
        throw new IllegalArgumentException("Invalid DISCONNECT Packet Identifier");
      }
      flags |= 1;
      length += 2;
    }
    if (packet.reasonCode() != null) {
      if (packet.reasonCode() < 0 || packet.reasonCode() > 255) {
        throw new IllegalArgumentException("Invalid DISCONNECT reason code");
      }
      flags |= 4;
      length++;
    }
    if (packet.sessionExpiry() != null) {
      if (packet.sessionExpiry() < 0 || packet.sessionExpiry() > 0xFFFFFFFFL) {
        throw new IllegalArgumentException("Invalid DISCONNECT expiry");
      }
      flags |= 2;
      length += 4;
    }
    String reasonString = packet.reasonString() == null ? "" : packet.reasonString();
    if (reasonString.indexOf('\u0000') >= 0) throw new IllegalArgumentException("Invalid reason string");
    byte[] reason = reasonString.getBytes(StandardCharsets.UTF_8);
    ByteBuffer body = ByteBuffer.allocate(length + reason.length).put((byte) flags);
    if (packet.packetIdentifier() != null) body.putShort((short) packet.packetIdentifier().intValue());
    if (packet.reasonCode() != null) body.put((byte) packet.reasonCode().intValue());
    if (packet.sessionExpiry() != null) body.putInt(packet.sessionExpiry().intValue());
    body.put(reason).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.DISCONNECT, body);
  }
}
