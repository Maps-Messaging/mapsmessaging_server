/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;

/** MQTT-SN 2.0 CSD01 SLEEPREQ, SLEEPRESP and WAKEUP wire formats. */
public final class MqttSn2SleepCodec {

  public record SleepRequest(boolean retainAliases, int packetIdentifier, long durationSeconds) {}
  public record SleepResponse(int packetIdentifier, Long durationSeconds, Integer reasonCode) {}
  public record Wakeup() {}

  private MqttSn2SleepCodec() {}

  public static ByteBuffer encodeRequest(SleepRequest request) {
    checkIdentifier(request.packetIdentifier());
    if (request.durationSeconds() < 1 || request.durationSeconds() > 0xFFFFFFFFL) {
      throw new IllegalArgumentException("Invalid SLEEPREQ duration");
    }
    ByteBuffer body = ByteBuffer.allocate(7);
    body.put((byte) (request.retainAliases() ? 1 : 0))
        .putShort((short) request.packetIdentifier())
        .putInt((int) request.durationSeconds()).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.SLEEPREQ, body);
  }

  public static SleepRequest decodeRequest(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer b = body(frame, MqttSn2PacketType.SLEEPREQ);
    if (b.remaining() != 7) throw new IOException("SLEEPREQ must have seven payload octets");
    int flags = Byte.toUnsignedInt(b.get());
    if ((flags & 0xFE) != 0) throw new IOException("Reserved SLEEPREQ flags");
    int id = identifier(b);
    long duration = Integer.toUnsignedLong(b.getInt());
    if (duration == 0) throw new IOException("SLEEPREQ duration must be non-zero");
    return new SleepRequest((flags & 1) != 0, id, duration);
  }

  public static ByteBuffer encodeResponse(SleepResponse response) {
    checkIdentifier(response.packetIdentifier());
    long duration = response.durationSeconds() == null ? 0 : response.durationSeconds();
    if (response.durationSeconds() != null && (duration < 1 || duration > 0xFFFFFFFFL)) {
      throw new IllegalArgumentException("Invalid SLEEPRESP duration");
    }
    if (response.reasonCode() != null && (response.reasonCode() < 0 || response.reasonCode() > 255)) {
      throw new IllegalArgumentException("Invalid SLEEPRESP reason code");
    }
    int size = 3 + (response.durationSeconds() == null ? 0 : 4)
        + (response.reasonCode() == null ? 0 : 1);
    ByteBuffer b = ByteBuffer.allocate(size);
    b.put((byte) (response.durationSeconds() == null ? 0 : 1))
        .putShort((short) response.packetIdentifier());
    if (response.durationSeconds() != null) b.putInt((int) duration);
    if (response.reasonCode() != null) b.put((byte) response.reasonCode().intValue());
    b.flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.SLEEPRESP, b);
  }

  public static SleepResponse decodeResponse(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer b = body(frame, MqttSn2PacketType.SLEEPRESP);
    if (b.remaining() < 3) throw new IOException("Truncated SLEEPRESP");
    int flags = Byte.toUnsignedInt(b.get());
    if ((flags & 0xFE) != 0) throw new IOException("Reserved SLEEPRESP flags");
    int id = identifier(b);
    Long duration = null;
    if ((flags & 1) != 0) {
      if (b.remaining() < 4) throw new IOException("Missing SLEEPRESP duration");
      duration = Integer.toUnsignedLong(b.getInt());
      if (duration == 0) throw new IOException("Zero SLEEPRESP duration");
    }
    if (b.remaining() > 1) throw new IOException("Unexpected SLEEPRESP trailing bytes");
    return new SleepResponse(id, duration, b.hasRemaining() ? Byte.toUnsignedInt(b.get()) : null);
  }

  public static Wakeup decodeWakeup(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = body(frame, MqttSn2PacketType.WAKEUP);
    if (body.hasRemaining()) throw new IOException("WAKEUP must have an empty body");
    return new Wakeup();
  }

  public static ByteBuffer encodeWakeup() {
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.WAKEUP, ByteBuffer.allocate(0));
  }

  private static ByteBuffer body(MqttSn2FrameCodec.Frame frame, MqttSn2PacketType expected)
      throws IOException {
    if (frame.type() != expected) throw new IOException("Expected " + expected);
    return frame.payload().asReadOnlyBuffer();
  }

  private static int identifier(ByteBuffer b) throws IOException {
    int id = Short.toUnsignedInt(b.getShort());
    if (id == 0) throw new IOException("Zero Packet Identifier");
    return id;
  }

  private static void checkIdentifier(int id) {
    if (id < 1 || id > 65535) throw new IllegalArgumentException("Invalid Packet Identifier");
  }
}
