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

/**
 * MQTT-SN 2.0 CSD01 CONNECT decoder, section 3.1.
 * Does not depend on the MQTT-SN 1.2 packet layout.
 */
public final class MqttSn2ConnectCodec {

  public record Connect(boolean cleanStart, boolean will, boolean authentication,
                        boolean addressChanges, boolean serverSuggestedValues,
                        int packetIdentifier, int keepAliveSeconds, int maximumPacketSize,
                        String clientIdentifier, String authenticationMethod,
                        byte[] authenticationData) {
    public Connect {
      authenticationData = authenticationData.clone();
    }

    @Override
    public byte[] authenticationData() {
      return authenticationData.clone();
    }
  }

  private MqttSn2ConnectCodec() {
  }

  public static ByteBuffer encode(Connect request) {
    if (request == null || request.packetIdentifier() < 1 || request.packetIdentifier() > 65535
        || request.keepAliveSeconds() < 1 || request.keepAliveSeconds() > 65535
        || request.maximumPacketSize() < 0 || request.maximumPacketSize() > 65535
        || (request.maximumPacketSize() != 0 && request.maximumPacketSize() < 10)) {
      throw new IllegalArgumentException("Invalid MQTT-SN 2.0 CONNECT parameters");
    }
    byte[] id = request.clientIdentifier().getBytes(StandardCharsets.UTF_8);
    byte[] method = request.authenticationMethod() == null ? new byte[0]
        : request.authenticationMethod().getBytes(StandardCharsets.UTF_8);
    byte[] data = request.authenticationData();
    if (request.authentication() && (method.length == 0 || method.length > 255
        || data.length > 65535)) {
      throw new IllegalArgumentException("Invalid CONNECT authentication fields");
    }
    if (!request.authentication() && (method.length != 0 || data.length != 0)) {
      throw new IllegalArgumentException("Unexpected CONNECT authentication fields");
    }
    int flags = (request.cleanStart() ? 1 : 0) | (request.will() ? 2 : 0)
        | (request.authentication() ? 4 : 0)
        | (request.addressChanges() ? 0x20 : 0)
        | (request.serverSuggestedValues() ? 0x40 : 0);
    ByteBuffer body = ByteBuffer.allocate(8 + id.length
        + (request.authentication() ? method.length + data.length + 3 : 0));
    body.put((byte) flags).putShort((short) request.packetIdentifier()).put((byte) 2)
        .putShort((short) request.keepAliveSeconds())
        .putShort((short) request.maximumPacketSize());
    if (request.authentication()) {
      body.put((byte) method.length).put(method).putShort((short) data.length).put(data);
    }
    body.put(id).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.CONNECT, body);
  }

  public static Connect decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (frame.type() != MqttSn2PacketType.CONNECT) {
      throw new IOException("Expected MQTT-SN 2.0 CONNECT");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 8) {
      throw new IOException("Truncated MQTT-SN 2.0 CONNECT");
    }
    int flags = Byte.toUnsignedInt(body.get());
    if ((flags & 0x98) != 0) {
      throw new IOException("Reserved MQTT-SN 2.0 CONNECT flags");
    }
    int id = Short.toUnsignedInt(body.getShort());
    if (id == 0) {
      throw new IOException("CONNECT Packet Identifier must be non-zero");
    }
    int protocolId = Byte.toUnsignedInt(body.get());
    if (protocolId != 2) {
      throw new IOException("Unsupported MQTT-SN protocol identifier " + protocolId);
    }
    int keepAlive = Short.toUnsignedInt(body.getShort());
    int maxPacket = Short.toUnsignedInt(body.getShort());
    if (keepAlive == 0 || (maxPacket != 0 && maxPacket < 10)) {
      throw new IOException("Invalid CONNECT keep alive or maximum packet size");
    }

    boolean auth = (flags & 0x04) != 0;
    String method = null;
    byte[] authData = new byte[0];
    if (auth) {
      if (body.remaining() < 3) {
        throw new IOException("Truncated CONNECT authentication");
      }
      int methodLength = Byte.toUnsignedInt(body.get());
      if (methodLength == 0 || body.remaining() < methodLength + 2) {
        throw new IOException("Invalid CONNECT authentication method");
      }
      byte[] methodBytes = new byte[methodLength];
      body.get(methodBytes);
      method = utf8(methodBytes);
      int dataLength = Short.toUnsignedInt(body.getShort());
      if (body.remaining() < dataLength) {
        throw new IOException("Truncated CONNECT authentication data");
      }
      authData = new byte[dataLength];
      body.get(authData);
    }
    byte[] clientBytes = new byte[body.remaining()];
    body.get(clientBytes);
    return new Connect((flags & 1) != 0, (flags & 2) != 0, auth,
        (flags & 0x20) != 0, (flags & 0x40) != 0, id, keepAlive, maxPacket,
        utf8(clientBytes), method, authData);
  }

  private static String utf8(byte[] bytes) throws IOException {
    try {
      return StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException e) {
      throw new IOException("Invalid MQTT-SN 2.0 UTF-8", e);
    }
  }
}
