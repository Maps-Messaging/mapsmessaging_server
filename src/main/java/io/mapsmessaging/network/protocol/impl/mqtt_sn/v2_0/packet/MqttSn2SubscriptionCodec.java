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
 * MQTT-SN 2.0 CSD01 subscription request wire structures.
 * Application-level topic matching stays in the existing server subscription layer.
 */
public final class MqttSn2SubscriptionCodec {

  public record Request(boolean subscribe, int packetIdentifier, int topicType, String topic,
                        int topicAlias, int maximumQos, boolean noLocal,
                        boolean retainAsPublished, int retainHandling) {
  }

  private MqttSn2SubscriptionCodec() {
  }

  public static Request decode(MqttSn2FrameCodec.Frame frame) throws IOException {
    boolean subscribe = frame.type() == MqttSn2PacketType.SUBSCRIBE;
    if (!subscribe && frame.type() != MqttSn2PacketType.UNSUBSCRIBE) {
      throw new IOException("Expected SUBSCRIBE or UNSUBSCRIBE");
    }
    ByteBuffer body = frame.payload().asReadOnlyBuffer();
    if (body.remaining() < 3) {
      throw new IOException("Truncated subscription packet");
    }
    int flags = Byte.toUnsignedInt(body.get());
    int topicType = flags & 3;
    if (topicType == 3) {
      throw new IOException("Reserved subscription topic type");
    }
    int qos = 0;
    int retainHandling = 0;
    boolean noLocal = false;
    boolean retainAsPublished = false;
    if (subscribe) {
      qos = (flags >>> 5) & 3;
      retainHandling = (flags >>> 2) & 3;
      retainAsPublished = (flags & 0x10) != 0;
      noLocal = (flags & 0x80) != 0;
      if (qos == 3 || retainHandling == 3) {
        throw new IOException("Reserved SUBSCRIBE QoS or retain handling");
      }
    } else if ((flags & 0xFC) != 0) {
      throw new IOException("Reserved UNSUBSCRIBE flags");
    }
    int id = Short.toUnsignedInt(body.getShort());
    if (id == 0) {
      throw new IOException("Zero subscription Packet Identifier");
    }
    String topic = null;
    int alias = 0;
    if (topicType == 0) {
      if (!body.hasRemaining()) {
        throw new IOException("Missing subscription topic");
      }
      byte[] data = new byte[body.remaining()];
      body.get(data);
      try {
        topic = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(data)).toString();
      } catch (CharacterCodingException e) {
        throw new IOException("Invalid subscription UTF-8", e);
      }
      if (topic.isEmpty() || topic.indexOf('\u0000') >= 0) {
        throw new IOException("Invalid subscription topic");
      }
    } else {
      if (body.remaining() != 2) {
        throw new IOException("Subscription alias must contain exactly two octets");
      }
      alias = Short.toUnsignedInt(body.getShort());
      if (alias == 0) {
        throw new IOException("Zero subscription alias");
      }
    }
    return new Request(subscribe, id, topicType, topic, alias, qos, noLocal,
        retainAsPublished, retainHandling);
  }
}
