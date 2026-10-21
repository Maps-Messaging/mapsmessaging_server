/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.io.Packet;
import java.io.ByteArrayOutputStream;
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
    assertEquals(Integer.toString(Priority.NORMAL.getValue()), frame.getHeader("priority"));
  }

  @Test
  void packsCompleteMessageWithNumericPriorityAndOneTerminator() {
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
    Message internal =
        new MessageBuilder()
            .setId(42)
            .setPriority(Priority.ONE_ABOVE_NORMAL)
            .setOpaqueData(payload)
            .build();
    io.mapsmessaging.network.protocol.impl.stomp.frames.Message frame =
        new io.mapsmessaging.network.protocol.impl.stomp.frames.Message(1024, false);
    frame.packMessage("/topic/test", "subscription", internal, true, false);

    Packet coalescingPacket = new Packet(1024, false);
    Packet[] packets = frame.packAdvancedFrame(coalescingPacket);
    packets[0].flip();
    byte[] wire = concatenate(packets);
    String text = new String(wire, StandardCharsets.UTF_8);

    assertTrue(text.startsWith("MESSAGE\n"), text);
    assertTrue(text.contains("content-length:" + payload.length + "\n"), text);
    assertTrue(text.contains("priority:" + Priority.ONE_ABOVE_NORMAL.getValue() + "\n"), text);
    assertTrue(text.endsWith("\n\npayload\0"), text);
    assertEquals(1, count(wire, (byte) 0));
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

  private byte[] concatenate(Packet[] packets) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (Packet packet : packets) {
      ByteBuffer duplicate = packet.getRawBuffer().duplicate();
      byte[] part = new byte[duplicate.remaining()];
      duplicate.get(part);
      output.writeBytes(part);
    }
    return output.toByteArray();
  }

  private int count(byte[] bytes, byte expected) {
    int result = 0;
    for (byte value : bytes) {
      if (value == expected) {
        result++;
      }
    }
    return result;
  }
}
