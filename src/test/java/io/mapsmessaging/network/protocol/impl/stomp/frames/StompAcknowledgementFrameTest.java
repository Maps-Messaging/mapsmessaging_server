/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.Packet;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StompAcknowledgementFrameTest {

  @Test
  void stomp12MessageContainsOpaqueAckHeader() {
    Message internal =
        new MessageBuilder()
            .setId(42)
            .setOpaqueData("payload".getBytes(StandardCharsets.UTF_8))
            .build();
    io.mapsmessaging.network.protocol.impl.stomp.frames.Message frame =
        new io.mapsmessaging.network.protocol.impl.stomp.frames.Message(1024, false);

    frame.packMessage("/topic/test", "subscription:one", internal, true, true);

    assertEquals("subscription:one:42", frame.getHeader("ack"));
    assertEquals("42", frame.getHeader("message-id"));
    assertEquals("subscription:one", frame.getHeader("subscription"));
  }

  @Test
  void autoAcknowledgementDoesNotAddAckHeader() {
    Message internal =
        new MessageBuilder().setId(7).setOpaqueData(new byte[]{1}).build();
    io.mapsmessaging.network.protocol.impl.stomp.frames.Message frame =
        new io.mapsmessaging.network.protocol.impl.stomp.frames.Message(1024, false);

    frame.packMessage("/topic/test", "subscription", internal, true, false);

    assertFalse(frame.getHeader().containsKey("ack"));
  }

  @Test
  void parsesStomp12AckIdAfterHeaderUnescaping() throws Exception {
    Packet packet =
        new Packet(
            ByteBuffer.wrap(
                ("ACK\n"
                        + "id:subscription\\cone\\c42\n"
                        + "\n"
                        + "\0")
                    .getBytes(StandardCharsets.UTF_8)));
    Ack ack = assertInstanceOf(
        Ack.class, new FrameFactory(1024, false, false).parseFrame(packet));

    ack.scanFrame(packet);

    assertTrue(ack.isValid());
    assertEquals("subscription:one:42", ack.getAcknowledgementId());
    AcknowledgementToken.Value value =
        AcknowledgementToken.parse(ack.getAcknowledgementId());
    assertEquals("subscription:one", value.subscriptionId());
    assertEquals(42, value.messageId());
  }

  @Test
  void retainsStomp11SubscriptionAndMessageIdForm() throws Exception {
    Packet packet =
        new Packet(
            ByteBuffer.wrap(
                ("ACK\n"
                        + "subscription:subscription-one\n"
                        + "message-id:42\n"
                        + "\n"
                        + "\0")
                    .getBytes(StandardCharsets.UTF_8)));
    Ack ack = assertInstanceOf(
        Ack.class, new FrameFactory(1024, false, false).parseFrame(packet));

    ack.scanFrame(packet);

    assertTrue(ack.isValid());
    assertEquals("subscription-one", ack.getSubscription());
    assertEquals("42", ack.getMessageId());
  }
}
