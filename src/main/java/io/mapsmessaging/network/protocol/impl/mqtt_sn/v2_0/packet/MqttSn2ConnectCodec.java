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

/** MQTT-SN 2.0 CSD01 CONNECT decoder and encoder, section 3.1. */
public final class MqttSn2ConnectCodec {

  public record Connect(boolean cleanStart, boolean will, boolean authentication,
                        boolean addressChanges, boolean serverSuggestedValues,
                        int packetIdentifier, int keepAliveSeconds, int maximumPacketSize,
                        String clientIdentifier, String authenticationMethod,
                        byte[] authenticationData, Long sessionExpirySeconds,
                        Integer defaultAwakeMessages, int willTopicType, int willQos,
                        boolean willRetained, int willTopicAlias, String willTopicName,
                        byte[] willPayload) {
    public Connect {
      authenticationData = authenticationData == null ? new byte[0] : authenticationData.clone();
      willPayload = willPayload == null ? new byte[0] : willPayload.clone();
    }

    public Connect(boolean cleanStart, boolean will, boolean authentication,
        boolean addressChanges, boolean serverSuggestedValues, int packetIdentifier,
        int keepAliveSeconds, int maximumPacketSize, String clientIdentifier,
        String authenticationMethod, byte[] authenticationData) {
      this(cleanStart, will, authentication, addressChanges, serverSuggestedValues,
          packetIdentifier, keepAliveSeconds, maximumPacketSize, clientIdentifier,
          authenticationMethod, authenticationData, null, null, 3, 0, false, 0,
          null, new byte[0]);
    }

    @Override
    public byte[] authenticationData() {
      return authenticationData.clone();
    }

    @Override
    public byte[] willPayload() {
      return willPayload.clone();
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
    byte[] id = utf8(request.clientIdentifier(), "Client Identifier");
    byte[] method = request.authenticationMethod() == null ? new byte[0]
        : utf8(request.authenticationMethod(), "Authentication Method");
    byte[] authData = request.authenticationData();
    if (request.authentication() && (method.length == 0 || method.length > 255
        || authData.length > 65535)) {
      throw new IllegalArgumentException("Invalid CONNECT authentication fields");
    }
    if (!request.authentication() && (method.length != 0 || authData.length != 0)) {
      throw new IllegalArgumentException("Unexpected CONNECT authentication fields");
    }
    if (request.sessionExpirySeconds() != null
        && (request.sessionExpirySeconds() < 0 || request.sessionExpirySeconds() > 0xFFFFFFFFL)) {
      throw new IllegalArgumentException("Invalid CONNECT Session Expiry Interval");
    }
    if (request.defaultAwakeMessages() != null
        && (request.defaultAwakeMessages() < 0 || request.defaultAwakeMessages() > 255)) {
      throw new IllegalArgumentException("Invalid Default Awake Messages");
    }

    byte[] willTopic = new byte[0];
    byte[] willPayload = request.willPayload();
    if (request.will()) {
      if (request.willQos() < 0 || request.willQos() > 2 || request.willTopicType() == 2
          || request.willTopicType() < 0 || request.willTopicType() > 3
          || willPayload.length > 65535) {
        throw new IllegalArgumentException("Invalid CONNECT Will fields");
      }
      if (request.willTopicType() == 3) {
        willTopic = utf8(request.willTopicName(), "Will Topic Name");
        if (willTopic.length == 0 || willTopic.length > 65535) {
          throw new IllegalArgumentException("Invalid CONNECT Will Topic Name");
        }
      } else if (request.willTopicAlias() < 1 || request.willTopicAlias() > 65535) {
        throw new IllegalArgumentException("Invalid CONNECT Will Topic Alias");
      }
    } else if (request.willTopicName() != null || willPayload.length != 0
        || request.willTopicAlias() != 0) {
      throw new IllegalArgumentException("Unexpected CONNECT Will fields");
    }

    int flags = (request.cleanStart() ? 1 : 0) | (request.will() ? 2 : 0)
        | (request.authentication() ? 4 : 0)
        | (request.sessionExpirySeconds() != null ? 8 : 0)
        | (request.defaultAwakeMessages() != null ? 0x10 : 0)
        | (request.addressChanges() ? 0x20 : 0)
        | (request.serverSuggestedValues() ? 0x40 : 0);
    int willFlags = request.willTopicType() | (request.willQos() << 2)
        | (request.willRetained() ? 0x10 : 0);
    int bodyLength = 8 + (request.will() ? 1 : 0) + id.length;
    if (request.sessionExpirySeconds() != null) bodyLength += 4;
    if (request.defaultAwakeMessages() != null) bodyLength++;
    if (request.will()) bodyLength += 4 + willTopic.length + willPayload.length;
    if (request.authentication()) bodyLength += 3 + method.length + authData.length;

    ByteBuffer body = ByteBuffer.allocate(bodyLength).put((byte) flags);
    if (request.will()) body.put((byte) willFlags);
    body.putShort((short) request.packetIdentifier()).put((byte) 2)
        .putShort((short) request.keepAliveSeconds()).putShort((short) request.maximumPacketSize());
    if (request.sessionExpirySeconds() != null) body.putInt(request.sessionExpirySeconds().intValue());
    if (request.defaultAwakeMessages() != null) body.put((byte) request.defaultAwakeMessages().intValue());
    if (request.will()) {
      int topicValue = request.willTopicType() == 3 ? willTopic.length : request.willTopicAlias();
      body.putShort((short) topicValue).put(willTopic).putShort((short) willPayload.length).put(willPayload);
    }
    if (request.authentication()) {
      body.put((byte) method.length).put(method).putShort((short) authData.length).put(authData);
    }
    body.put(id).flip();
    return MqttSn2FrameCodec.encode(MqttSn2PacketType.CONNECT, body);
  }

  public static Connect decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    if (frame.type() != MqttSn2PacketType.CONNECT) {
      throw new IOException("Expected MQTT-SN 2.0 CONNECT");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 8) throw new IOException("Truncated MQTT-SN 2.0 CONNECT");
    int flags = Byte.toUnsignedInt(body.get());
    if ((flags & 0x80) != 0) throw new IOException("Reserved MQTT-SN 2.0 CONNECT flags");
    boolean will = (flags & 0x02) != 0;
    int willFlags = will ? readUnsignedByte(body, "Will Flags") : 0;
    int willTopicType = willFlags & 3;
    int willQos = (willFlags >>> 2) & 3;
    boolean willRetained = (willFlags & 0x10) != 0;
    if (will && ((willFlags & 0xE0) != 0 || willTopicType == 2 || willQos == 3)) {
      throw new IOException("Invalid MQTT-SN 2.0 CONNECT Will Flags");
    }

    int packetIdentifier = Short.toUnsignedInt(body.getShort());
    if (packetIdentifier == 0) throw new IOException("CONNECT Packet Identifier must be non-zero");
    int protocolId = Byte.toUnsignedInt(body.get());
    if (protocolId != 2) throw new IOException("Unsupported MQTT-SN protocol identifier " + protocolId);
    int keepAlive = Short.toUnsignedInt(body.getShort());
    int maxPacket = Short.toUnsignedInt(body.getShort());
    if (keepAlive == 0 || (maxPacket != 0 && maxPacket < 10)) {
      throw new IOException("Invalid CONNECT keep alive or maximum packet size");
    }

    Long expiry = null;
    if ((flags & 8) != 0) {
      if (body.remaining() < 4) throw new IOException("Truncated CONNECT Session Expiry Interval");
      expiry = Integer.toUnsignedLong(body.getInt());
    }
    Integer awakeMessages = null;
    if ((flags & 0x10) != 0) awakeMessages = readUnsignedByte(body, "Default Awake Messages");

    int willAlias = 0;
    String willTopicName = null;
    byte[] willPayload = new byte[0];
    if (will) {
      if (body.remaining() < 2) throw new IOException("Truncated CONNECT Will Topic field");
      int topicLengthOrAlias = Short.toUnsignedInt(body.getShort());
      if (topicLengthOrAlias == 0) throw new IOException("Empty CONNECT Will Topic field");
      if (willTopicType == 3) {
        if (body.remaining() < topicLengthOrAlias) throw new IOException("Truncated CONNECT Will Topic Name");
        byte[] topicBytes = new byte[topicLengthOrAlias];
        body.get(topicBytes);
        willTopicName = utf8(topicBytes);
        if (willTopicName.isEmpty() || willTopicName.indexOf('#') >= 0
            || willTopicName.indexOf('+') >= 0 || willTopicName.indexOf('\u0000') >= 0) {
          throw new IOException("Invalid CONNECT Will Topic Name");
        }
      } else {
        willAlias = topicLengthOrAlias;
      }
      if (body.remaining() < 2) throw new IOException("Missing CONNECT Will Payload Length");
      int payloadLength = Short.toUnsignedInt(body.getShort());
      if (body.remaining() < payloadLength) throw new IOException("Truncated CONNECT Will Payload");
      willPayload = new byte[payloadLength];
      body.get(willPayload);
    }

    boolean authentication = (flags & 4) != 0;
    String method = null;
    byte[] authData = new byte[0];
    if (authentication) {
      if (body.remaining() < 3) throw new IOException("Truncated CONNECT authentication");
      int methodLength = readUnsignedByte(body, "Authentication Method Length");
      if (methodLength == 0 || body.remaining() < methodLength + 2) {
        throw new IOException("Invalid CONNECT authentication method");
      }
      byte[] methodBytes = new byte[methodLength];
      body.get(methodBytes);
      method = utf8(methodBytes);
      int dataLength = Short.toUnsignedInt(body.getShort());
      if (body.remaining() < dataLength) throw new IOException("Truncated CONNECT authentication data");
      authData = new byte[dataLength];
      body.get(authData);
    }
    byte[] clientBytes = new byte[body.remaining()];
    body.get(clientBytes);
    return new Connect((flags & 1) != 0, will, authentication, (flags & 0x20) != 0,
        (flags & 0x40) != 0, packetIdentifier, keepAlive, maxPacket, utf8(clientBytes),
        method, authData, expiry, awakeMessages, willTopicType, willQos,
        willRetained, willAlias, willTopicName, willPayload);
  }

  private static int readUnsignedByte(ByteBuffer body, String field) throws IOException {
    if (!body.hasRemaining()) throw new IOException("Missing CONNECT " + field);
    return Byte.toUnsignedInt(body.get());
  }

  private static byte[] utf8(String value, String field) {
    if (value == null || value.indexOf('\u0000') >= 0) {
      throw new IllegalArgumentException("Invalid " + field);
    }
    return value.getBytes(StandardCharsets.UTF_8);
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
