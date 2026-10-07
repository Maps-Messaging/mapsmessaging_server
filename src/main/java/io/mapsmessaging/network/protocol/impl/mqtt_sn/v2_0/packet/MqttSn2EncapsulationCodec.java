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
import java.util.EnumSet;

/**
 * MQTT-SN 2.0 CSD01 forwarder/connection encapsulation wire structures.
 * Protection encapsulation is handled by a separately negotiated crypto provider.
 */
public final class MqttSn2EncapsulationCodec {

  private static final EnumSet<MqttSn2PacketType> CONNECTION_ALLOWED = EnumSet.of(
      MqttSn2PacketType.PUBLISH, MqttSn2PacketType.SUBSCRIBE,
      MqttSn2PacketType.UNSUBSCRIBE, MqttSn2PacketType.REGISTER,
      MqttSn2PacketType.DISCONNECT, MqttSn2PacketType.SLEEPREQ,
      MqttSn2PacketType.PINGREQ);

  public record Forwarder(byte[] addressing, byte[] embeddedPacket) {
    public Forwarder {
      addressing = addressing.clone();
      embeddedPacket = embeddedPacket.clone();
    }
    @Override public byte[] addressing() { return addressing.clone(); }
    @Override public byte[] embeddedPacket() { return embeddedPacket.clone(); }
  }

  public record Connection(String clientIdentifier, byte[] embeddedPacket) {
    public Connection { embeddedPacket = embeddedPacket.clone(); }
    @Override public byte[] embeddedPacket() { return embeddedPacket.clone(); }
  }

  private MqttSn2EncapsulationCodec() {}

  public static Forwarder decodeForwarder(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer b = payload(frame, MqttSn2PacketType.FORWARDER_ENCAPSULATION);
    if (!b.hasRemaining()) throw new IOException("Missing forwarder addressing length");
    int count = Byte.toUnsignedInt(b.get());
    if (b.remaining() < count) throw new IOException("Truncated forwarder addressing");
    byte[] addressing = new byte[count];
    b.get(addressing);
    byte[] inner = remaining(b);
    validateEmbedded(inner, false);
    return new Forwarder(addressing, inner);
  }

  public static Connection decodeConnection(MqttSn2FrameCodec.Frame frame) throws IOException {
    ByteBuffer b = payload(frame, MqttSn2PacketType.CONNECTION_ENCAPSULATION);
    if (b.remaining() < 2) throw new IOException("Missing encapsulated Client Identifier length");
    int count = Short.toUnsignedInt(b.getShort());
    if (b.remaining() < count) throw new IOException("Truncated encapsulated Client Identifier");
    byte[] name = new byte[count];
    b.get(name);
    String clientId;
    try {
      clientId = StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(name)).toString();
    } catch (CharacterCodingException e) {
      throw new IOException("Invalid encapsulated Client Identifier", e);
    }
    byte[] inner = remaining(b);
    validateEmbedded(inner, true);
    return new Connection(clientId, inner);
  }

  public static ByteBuffer encodeForwarder(Forwarder packet) throws IOException {
    byte[] address = packet.addressing();
    byte[] inner = packet.embeddedPacket();
    if (address.length > 255) throw new IllegalArgumentException("Forwarder address too long");
    validateEmbedded(inner, false);
    ByteBuffer body = ByteBuffer.allocate(1 + address.length + inner.length);
    body.put((byte) address.length).put(address).put(inner).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.FORWARDER_ENCAPSULATION, body);
  }

  public static ByteBuffer encodeConnection(Connection packet) throws IOException {
    byte[] name = packet.clientIdentifier().getBytes(StandardCharsets.UTF_8);
    byte[] inner = packet.embeddedPacket();
    if (name.length > 65535) throw new IllegalArgumentException("Client Identifier too long");
    validateEmbedded(inner, true);
    ByteBuffer body = ByteBuffer.allocate(2 + name.length + inner.length);
    body.putShort((short) name.length).put(name).put(inner).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.CONNECTION_ENCAPSULATION, body);
  }

  private static void validateEmbedded(byte[] inner, boolean connection) throws IOException {
    if (inner.length == 0) throw new IOException("Missing embedded MQTT-SN 2.0 packet");
    MqttSn2FrameCodec.Frame frame = MqttSn2FrameCodec.decode(ByteBuffer.wrap(inner));
    if (connection && !CONNECTION_ALLOWED.contains(frame.type())) {
      throw new IOException("Packet not permitted inside Connection Encapsulation");
    }
  }

  private static ByteBuffer payload(MqttSn2FrameCodec.Frame frame, MqttSn2PacketType type)
      throws IOException {
    if (frame.type() != type) throw new IOException("Expected " + type);
    return frame.payload().asReadOnlyBuffer();
  }

  private static byte[] remaining(ByteBuffer body) {
    byte[] data = new byte[body.remaining()];
    body.get(data);
    return data;
  }
}
