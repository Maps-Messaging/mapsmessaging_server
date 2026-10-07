/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** MQTT-SN 2.0 CSD01 CONNACK encoder matching the reference-client decoder. */
public final class MqttSn2ConnAckCodec {

  public record ConnAck(boolean sessionPresent, int packetIdentifier, int reasonCode,
                        Long sessionExpirySeconds, Integer serverKeepAliveSeconds,
                        String authenticationMethod, byte[] authenticationData,
                        String assignedClientIdentifier) {
    public ConnAck {
      authenticationData = authenticationData == null ? null : authenticationData.clone();
    }

    @Override
    public byte[] authenticationData() {
      return authenticationData == null ? null : authenticationData.clone();
    }
  }

  private MqttSn2ConnAckCodec() {
  }

  public static ByteBuffer encode(ConnAck ack) {
    if (ack.packetIdentifier() < 1 || ack.packetIdentifier() > 65535
        || ack.reasonCode() < 0 || ack.reasonCode() > 255) {
      throw new IllegalArgumentException("Invalid CONNACK Packet Identifier or Reason Code");
    }
    if (ack.sessionPresent() && ack.reasonCode() != 0) {
      throw new IllegalArgumentException("Session Present cannot be set on failure");
    }
    if (ack.sessionExpirySeconds() != null
        && (ack.sessionExpirySeconds() < 0 || ack.sessionExpirySeconds() > 0xFFFFFFFFL)) {
      throw new IllegalArgumentException("Invalid Session Expiry");
    }
    if (ack.serverKeepAliveSeconds() != null
        && (ack.serverKeepAliveSeconds() < 1 || ack.serverKeepAliveSeconds() > 65535)) {
      throw new IllegalArgumentException("Invalid Server Keep Alive");
    }
    if (ack.authenticationMethod() == null && ack.authenticationData() != null) {
      throw new IllegalArgumentException("Authentication data without mechanism");
    }

    byte[] method = ack.authenticationMethod() == null ? new byte[0]
        : ack.authenticationMethod().getBytes(StandardCharsets.UTF_8);
    if (ack.authenticationMethod() != null && (method.length == 0 || method.length > 255)) {
      throw new IllegalArgumentException("Invalid CONNACK Authentication Method");
    }
    byte[] data = ack.authenticationData() == null ? new byte[0] : ack.authenticationData();
    if (data.length > 65535) {
      throw new IllegalArgumentException("CONNACK authentication data too large");
    }
    byte[] clientId = ack.assignedClientIdentifier() == null ? new byte[0]
        : ack.assignedClientIdentifier().getBytes(StandardCharsets.UTF_8);

    int flags = ack.sessionPresent() ? 1 : 0;
    int length = 4 + clientId.length;
    if (ack.sessionExpirySeconds() != null) {
      flags |= 2;
      length += 4;
    }
    if (ack.serverKeepAliveSeconds() != null) {
      flags |= 4;
      length += 2;
    }
    if (ack.authenticationMethod() != null) {
      flags |= 8;
      length += 3 + method.length + data.length;
    }

    ByteBuffer body = ByteBuffer.allocate(length);
    body.put((byte) flags).putShort((short) ack.packetIdentifier())
        .put((byte) ack.reasonCode());
    if (ack.sessionExpirySeconds() != null) {
      body.putInt(ack.sessionExpirySeconds().intValue());
    }
    if (ack.serverKeepAliveSeconds() != null) {
      body.putShort((short) ack.serverKeepAliveSeconds().intValue());
    }
    if (ack.authenticationMethod() != null) {
      body.put((byte) method.length).put(method)
          .putShort((short) data.length).put(data);
    }
    body.put(clientId).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.CONNACK, body);
  }
}
