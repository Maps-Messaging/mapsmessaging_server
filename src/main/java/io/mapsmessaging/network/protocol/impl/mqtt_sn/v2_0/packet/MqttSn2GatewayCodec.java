/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/** Gateway discovery messages from MQTT-SN 2.0 CSD01. */
public final class MqttSn2GatewayCodec {

  public record Advertise(int gatewayIdentifier, int durationSeconds) {
  }

  public record GatewayInfo(int gatewayIdentifier, byte[] address) {
    public GatewayInfo {
      address = Objects.requireNonNull(address, "address").clone();
    }

    @Override
    public byte[] address() {
      return address.clone();
    }
  }

  private MqttSn2GatewayCodec() {
  }

  public static Advertise decodeAdvertise(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = body(frame, MqttSn2PacketType.ADVERTISE);
    if (body.remaining() != 3) {
      throw new IOException("ADVERTISE must contain three payload octets");
    }
    return new Advertise(Byte.toUnsignedInt(body.get()), Short.toUnsignedInt(body.getShort()));
  }

  public static ByteBuffer encodeAdvertise(Advertise advertisement) {
    requireByte(advertisement.gatewayIdentifier());
    requireShort(advertisement.durationSeconds());
    ByteBuffer body = ByteBuffer.allocate(3);
    body.put((byte) advertisement.gatewayIdentifier())
        .putShort((short) advertisement.durationSeconds()).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.ADVERTISE, body);
  }

  public static int decodeSearchGateway(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = body(frame, MqttSn2PacketType.SEARCHGW);
    if (body.remaining() != 1) {
      throw new IOException("SEARCHGW must contain exactly one Radius octet");
    }
    return Byte.toUnsignedInt(body.get());
  }

  public static ByteBuffer encodeSearchGateway(int radius) {
    requireByte(radius);
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.SEARCHGW,
        ByteBuffer.wrap(new byte[]{(byte) radius}));
  }

  public static GatewayInfo decodeGatewayInfo(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer body = body(frame, MqttSn2PacketType.GWINFO);
    if (!body.hasRemaining()) {
      throw new IOException("Missing GWINFO Gateway Identifier");
    }
    int identifier = Byte.toUnsignedInt(body.get());
    byte[] address = new byte[body.remaining()];
    body.get(address);
    return new GatewayInfo(identifier, address);
  }

  public static ByteBuffer encodeGatewayInfo(GatewayInfo info) {
    requireByte(info.gatewayIdentifier());
    byte[] address = info.address();
    ByteBuffer body = ByteBuffer.allocate(1 + address.length);
    body.put((byte) info.gatewayIdentifier()).put(address).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.GWINFO, body);
  }

  private static ByteBuffer body(MqttSn2FrameCodec.Frame frame, MqttSn2PacketType type)
      throws IOException {
    if (frame.type() != type) {
      throw new IOException("Expected " + type + ", received " + frame.type());
    }
    return frame.payload().asReadOnlyBuffer();
  }

  private static void requireByte(int value) {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException("Value must fit an unsigned octet");
    }
  }

  private static void requireShort(int value) {
    if (value < 0 || value > 65535) {
      throw new IllegalArgumentException("Value must fit an unsigned short");
    }
  }
}
