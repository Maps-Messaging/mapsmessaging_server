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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.features.RetainHandler;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.SubscriptionInfo;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeUnsubscribeSubAckCoverageTest {

  @Test
  void subscribePackAndLoadRoundTripsSingleSubscription() throws Exception {
    Subscribe5 original = new Subscribe5();
    original.setMessageId(77);
    original.getSubscriptionList().add(
        new SubscriptionInfo("sensors/temperature", QualityOfService.AT_LEAST_ONCE));

    Subscribe5 restored = unpackSubscribe(original);

    assertEquals(77, restored.getMessageId());
    assertEquals(1, restored.getSubscriptionList().size());
    SubscriptionInfo info = restored.getSubscriptionList().get(0);
    assertEquals("sensors/temperature", info.getTopicName());
    assertEquals(QualityOfService.AT_LEAST_ONCE, info.getQualityOfService());
  }

  @Test
  void subscribePackAndLoadPreservesOptionBits() throws Exception {
    Subscribe5 original = new Subscribe5();
    original.setMessageId(9);
    original.getSubscriptionList().add(
        new SubscriptionInfo("a/b", QualityOfService.EXACTLY_ONCE,
            RetainHandler.SEND_IF_NEW, true, true, null));

    SubscriptionInfo info = unpackSubscribe(original).getSubscriptionList().get(0);

    assertEquals(QualityOfService.EXACTLY_ONCE, info.getQualityOfService());
    assertEquals(RetainHandler.SEND_IF_NEW, info.getRetainHandling());
    assertTrue(info.noLocalMessages());
    assertTrue(info.isRetainAsPublished());
  }

  @Test
  void subscribeRejectsInvalidFixedHeaderFlags() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new Subscribe5((byte) 0x00, 3, packet));
  }

  @Test
  void subscribeRejectsEmptyPayload() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new Subscribe5((byte) 0x02, 3, packet));
  }

  @Test
  void subscribeRejectsReservedOptionBits() {
    Packet packet = packet(new byte[]{0, 1, 0, 0, 1, 'a', (byte) 0xC0});

    assertThrows(MalformedException.class, () -> new Subscribe5((byte) 0x02, 7, packet));
  }

  @Test
  void subscribeRejectsInvalidQosOption() {
    Packet packet = packet(new byte[]{0, 1, 0, 0, 1, 'a', 0x03});

    assertThrows(MalformedException.class, () -> new Subscribe5((byte) 0x02, 7, packet));
  }

  @Test
  void unsubscribePackAndLoadRoundTripsMultipleTopics() throws Exception {
    Unsubscribe5 original = new Unsubscribe5(List.of("a/b", "c/d"));
    original.setMessageId(123);

    Packet packed = pack(original);
    byte fixedHeader = packed.get();
    int remaining = packed.get() & 0xff;
    Unsubscribe5 restored = new Unsubscribe5(fixedHeader, remaining, packed);

    assertEquals(123, restored.getMessageId());
    assertEquals(List.of("a/b", "c/d"), restored.getUnsubscribeList());
  }

  @Test
  void unsubscribeRejectsInvalidFixedHeaderFlags() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new Unsubscribe5((byte) 0x00, 3, packet));
  }

  @Test
  void unsubscribeRejectsEmptyPayload() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new Unsubscribe5((byte) 0x02, 3, packet));
  }

  @Test
  void subAckPackAndLoadRoundTripsReasonCodes() throws Exception {
    SubAck5 original = new SubAck5(42, new StatusCode[]{
        StatusCode.SUCCESS,
        StatusCode.SUCCESS_QOS_1,
        StatusCode.NOT_AUTHORISED
    });

    Packet packed = pack(original);
    byte fixedHeader = packed.get();
    int remaining = packed.get() & 0xff;
    SubAck5 restored = new SubAck5(fixedHeader, remaining, packed);

    assertEquals(42, restored.getPacketId());
    assertTrue(restored.toString().contains("SUCCESS"));
    assertTrue(restored.toString().contains("SUCCESS_QOS_1"));
    assertTrue(restored.toString().contains("NOT_AUTHORISED"));
  }

  @Test
  void subAckRejectsInvalidFixedHeaderFlags() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new SubAck5((byte) 0x01, 3, packet));
  }

  @Test
  void subAckRejectsMissingReasonCode() {
    Packet packet = packet(new byte[]{0, 1, 0});

    assertThrows(MalformedException.class, () -> new SubAck5((byte) 0x00, 3, packet));
  }

  @Test
  void subAckRejectsUnknownReasonCode() {
    Packet packet = packet(new byte[]{0, 1, 0, (byte) 0xFF});

    assertThrows(IllegalArgumentException.class, () -> new SubAck5((byte) 0x00, 4, packet));
  }

  private Subscribe5 unpackSubscribe(Subscribe5 original) throws Exception {
    Packet packed = pack(original);
    byte fixedHeader = packed.get();
    int remaining = packed.get() & 0xff;
    return new Subscribe5(fixedHeader, remaining, packed);
  }

  private Packet pack(MQTTPacket5 packet) {
    Packet buffer = new Packet(ByteBuffer.allocate(512));
    packet.packFrame(buffer);
    buffer.flip();
    return buffer;
  }

  private Packet packet(byte[] bytes) {
    return new Packet(ByteBuffer.wrap(bytes));
  }
}
