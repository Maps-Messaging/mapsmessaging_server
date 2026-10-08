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
 * Standalone CSD01 MQTT-SN 2.0 PUBLISH/PUBWOS payload decoding.
 * The caller resolves topic aliases and dispatches messages.
 */
public final class MqttSn2PublishCodec {

  public record Publish(boolean withoutSession, int qos, boolean duplicate, boolean retained,
                        int topicType, int packetIdentifier, String topicName, int topicAlias,
                        byte[] payload) {
    public Publish {
      payload = payload.clone();
    }
    @Override
    public byte[] payload() {
      return payload.clone();
    }
  }

  private MqttSn2PublishCodec() {
  }

  public static ByteBuffer encode(Publish publish) {
    if (publish == null) throw new IllegalArgumentException("Missing PUBLISH");
    int type = publish.topicType();
    int qos = publish.qos();
    boolean wos = publish.withoutSession();
    if ((type != 0 && type != 1 && type != 3) || qos < 0 || qos > 2
        || (wos && (type == 0 || qos != 0 || publish.duplicate()))
        || (!wos && publish.duplicate() && qos == 0)) {
      throw new IllegalArgumentException("Invalid MQTT-SN 2.0 PUBLISH flags");
    }
    if (!wos && qos > 0 && (publish.packetIdentifier() < 1 || publish.packetIdentifier() > 65535)) {
      throw new IllegalArgumentException("Invalid PUBLISH Packet Identifier");
    }
    byte[] topic;
    if (type == 3) {
      if (publish.topicName() == null || publish.topicName().isEmpty()
          || publish.topicName().indexOf('#') >= 0 || publish.topicName().indexOf('+') >= 0
          || publish.topicName().indexOf('\u0000') >= 0) {
        throw new IllegalArgumentException("Invalid PUBLISH Topic Name");
      }
      topic = publish.topicName().getBytes(StandardCharsets.UTF_8);
      if (topic.length > 65535) throw new IllegalArgumentException("Topic Name too long");
    } else {
      if (publish.topicAlias() < 1 || publish.topicAlias() > 65535) {
        throw new IllegalArgumentException("Invalid PUBLISH Topic Alias");
      }
      topic = null;
    }
    byte[] payload = publish.payload();
    int bodySize = 3 + ((!wos && qos > 0) ? 2 : 0)
        + (topic == null ? 0 : topic.length) + payload.length;
    ByteBuffer data = ByteBuffer.allocate(bodySize);
    int flags = type | (publish.retained() ? 0x10 : 0)
        | (wos ? 0 : qos << 5) | (publish.duplicate() ? 0x80 : 0);
    data.put((byte) flags);
    if (!wos && qos > 0) data.putShort((short) publish.packetIdentifier());
    if (type == 3) {
      data.putShort((short) topic.length).put(topic);
    } else {
      data.putShort((short) publish.topicAlias());
    }
    data.put(payload).flip();
    return MqttSn2FrameCodec.encode(wos ? MqttSn2PacketType.PUBWOS : MqttSn2PacketType.PUBLISH, data);
  }

  public static Publish decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    boolean wos = frame.type() == MqttSn2PacketType.PUBWOS;
    if (frame.type() != MqttSn2PacketType.PUBLISH && !wos) {
      throw new IOException("Expected PUBLISH or PUBWOS");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 3) {
      throw new IOException("Truncated MQTT-SN 2.0 publish");
    }
    int flags = Byte.toUnsignedInt(body.get());
    int topicType = flags & 3;
    boolean retain = (flags & 0x10) != 0;
    boolean duplicate = (flags & 0x80) != 0;
    int qos = (flags >>> 5) & 3;
    if (wos) {
      if ((flags & 0xEC) != 0 || topicType == 0 || topicType == 2) {
        throw new IOException("Invalid PUBWOS flags or topic type");
      }
    } else if ((flags & 0x0C) != 0 || qos == 3 || (duplicate && qos == 0)) {
      throw new IOException("Invalid PUBLISH flags");
    }

    int identifier = 0;
    if (!wos && qos != 0) {
      if (body.remaining() < 4) {
        throw new IOException("Truncated PUBLISH Packet Identifier");
      }
      identifier = Short.toUnsignedInt(body.getShort());
      if (identifier == 0) {
        throw new IOException("PUBLISH Packet Identifier must be non-zero");
      }
    }
    if (body.remaining() < 2) {
      throw new IOException("Missing PUBLISH topic field");
    }
    int topicValue = Short.toUnsignedInt(body.getShort());
    int alias = 0;
    String name = null;
    if (topicType == 3) {
      if (body.remaining() < topicValue) {
        throw new IOException("Truncated PUBLISH Topic Name");
      }
      byte[] topicBytes = new byte[topicValue];
      body.get(topicBytes);
      try {
        name = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(topicBytes)).toString();
      } catch (CharacterCodingException e) {
        throw new IOException("Malformed PUBLISH Topic Name UTF-8", e);
      }
      if (name.isEmpty() || name.indexOf('#') >= 0 || name.indexOf('+') >= 0
          || name.indexOf('\u0000') >= 0) {
        throw new IOException("Invalid PUBLISH Topic Name");
      }
    } else {
      if (topicType == 2 || topicValue == 0) {
        throw new IOException("Invalid PUBLISH topic alias");
      }
      alias = topicValue;
    }

    byte[] payload = new byte[body.remaining()];
    body.get(payload);
    return new Publish(wos, qos, duplicate, retain, topicType, identifier, name, alias, payload);
  }
}
