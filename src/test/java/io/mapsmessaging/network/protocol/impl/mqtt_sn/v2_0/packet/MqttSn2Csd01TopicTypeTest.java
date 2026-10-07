/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 * Licensed under the Apache License, Version 2.0 with the Commons Clause.
 */
package io.mapsmessaging.network.protocol.impl.mqtt_sn.v2_0.packet;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT-SN 2.0 CSD01 topic types are 0=session alias, 1=predefined alias,
 * 3=topic name; 2 is reserved. The 1.2 topic-type map is different.
 */
class MqttSn2Csd01TopicTypeTest {

  @Test
  void encodedPublishWithNameUsesTypeThree() throws Exception {
    MqttSn2PublishCodec.Publish source = new MqttSn2PublishCodec.Publish(
        false, 1, false, false, 3, 9, "devices/temp", 0, new byte[]{3, 4});
    ByteBuffer wire = MqttSn2PublishCodec.encode(source);
    assertEquals(3, Byte.toUnsignedInt(wire.get(2)) & 3);
    assertEquals("devices/temp",
        MqttSn2PublishCodec.decode(MqttSn2FrameCodec.decode(wire)).topicName());
  }

  @Test
  void pubwosRejectsSessionAliasButAcceptsPredefinedAlias() throws Exception {
    MqttSn2PublishCodec.Publish valid = new MqttSn2PublishCodec.Publish(
        true, 0, false, false, 1, 0, null, 37, new byte[]{5});
    assertEquals(37, MqttSn2PublishCodec.decode(
        MqttSn2FrameCodec.decode(MqttSn2PublishCodec.encode(valid))).topicAlias());
    assertThrows(IllegalArgumentException.class, () ->
        MqttSn2PublishCodec.encode(new MqttSn2PublishCodec.Publish(
            true, 0, false, false, 0, 0, null, 37, new byte[]{5})));
  }

  @Test
  void subscriptionsUseCorrectNameAndAliasIdentifiers() throws Exception {
    MqttSn2SubscriptionCodec.Request byName = new MqttSn2SubscriptionCodec.Request(
        true, 42, 3, "devices/+", 0, 1, false, false, 0);
    MqttSn2SubscriptionCodec.Request decoded = MqttSn2SubscriptionCodec.decode(
        MqttSn2FrameCodec.decode(MqttSn2SubscriptionCodec.encode(byName)));
    assertEquals(3, decoded.topicType());
    assertEquals("devices/+", decoded.topic());

    MqttSn2SubscriptionCodec.Request byAlias = new MqttSn2SubscriptionCodec.Request(
        false, 12, 0, null, 77, 0, false, false, 0);
    assertEquals(77, MqttSn2SubscriptionCodec.decode(
        MqttSn2FrameCodec.decode(MqttSn2SubscriptionCodec.encode(byAlias))).topicAlias());
    assertThrows(IOException.class, () -> MqttSn2SubscriptionCodec.decode(
        MqttSn2FrameCodec.decode(ByteBuffer.wrap(new byte[]{7, 10, 2, 0, 12, 0, 77}))));
  }

  @Test
  void responsesUseSessionAliasTypeZero() throws Exception {
    MqttSn2RegAckCodec.RegAck regAck = new MqttSn2RegAckCodec.RegAck(0, 77, 42, 0);
    ByteBuffer regBytes = MqttSn2RegAckCodec.encode(regAck);
    assertEquals(regAck, MqttSn2RegAckCodec.decode(MqttSn2FrameCodec.decode(regBytes)));

    MqttSn2ReplyCodec.SubAck subAck = new MqttSn2ReplyCodec.SubAck(0, 77, 42, 0);
    ByteBuffer subBytes = MqttSn2ReplyCodec.encodeSubAck(subAck);
    assertEquals(subAck, MqttSn2ReplyCodec.decodeSubAck(MqttSn2FrameCodec.decode(subBytes)));
  }
}
