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

package io.mapsmessaging.network.protocol.impl.mqtt_sn.v1_2.packet;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MqttSn12ExtendedLengthTest {
  private final PacketFactory factory = new PacketFactory();

  @ParameterizedTest
  @ValueSource(ints = {255, 256, 257, 258, 1024, 65535})
  void register_preserves_topic_and_identifiers_at_wire_length_boundaries(int length) throws Exception {
    int width = length < 256 ? 1 : 3;
    String topic = "t".repeat(length - width - 5);
    Register register = assertInstanceOf(Register.class,
        parse(wire(length, width, MQTT_SNPacket.REGISTER, new byte[] {0, 7, 0x12, 0x34}, topic.getBytes(StandardCharsets.UTF_8))));
    assertEquals(7, register.getTopicId());
    assertEquals(0x1234, register.getMessageId());
    assertEquals(topic, register.getTopic());
  }

  @ParameterizedTest
  @ValueSource(ints = {255, 256, 257, 258, 1024, 65535})
  void variable_will_fields_preserve_complete_payload_at_wire_length_boundaries(int length) throws Exception {
    int width = length < 256 ? 1 : 3;
    String topic = "t".repeat(length - width - 2);
    WillTopic will = assertInstanceOf(WillTopic.class,
        parse(wire(length, width, MQTT_SNPacket.WILLTOPIC, new byte[] {0x30}, topic.getBytes(StandardCharsets.UTF_8))));
    WillTopicUpdate update = assertInstanceOf(WillTopicUpdate.class,
        parse(wire(length, width, MQTT_SNPacket.WILLTOPICUPD, new byte[] {0x30}, topic.getBytes(StandardCharsets.UTF_8))));
    assertEquals(topic, will.getTopic());
    assertEquals(topic, update.getTopic());
    assertTrue(will.getRetain());
    assertTrue(update.isRetain());
    byte[] payload = new byte[length - width - 1];
    Arrays.fill(payload, (byte) 0xa5);
    assertArrayEquals(payload, assertInstanceOf(WillMessage.class,
        parse(wire(length, width, MQTT_SNPacket.WILLMSG, new byte[0], payload))).getMessage());
    assertArrayEquals(payload, assertInstanceOf(WillMessageUpdate.class,
        parse(wire(length, width, MQTT_SNPacket.WILLMSGUPD, new byte[0], payload))).getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {255, 256, 257, 258, 1024, 65535})
  void remaining_buffer_parsers_preserve_extended_payloads(int length) throws Exception {
    int width = length < 256 ? 1 : 3;
    byte[] payload = new byte[length - width - 6];
    Arrays.fill(payload, (byte) 0x5a);
    Publish publish = assertInstanceOf(Publish.class,
        parse(wire(length, width, MQTT_SNPacket.PUBLISH, new byte[] {0x20, 0, 7, 0x12, 0x34}, payload)));
    assertEquals(7, publish.getTopicId());
    assertEquals(0x1234, publish.getMessageId());
    assertArrayEquals(payload, publish.getMessage());
    String topic = "s".repeat(length - width - 4);
    assertEquals(topic, assertInstanceOf(Subscribe.class,
        parse(wire(length, width, MQTT_SNPacket.SUBSCRIBE, new byte[] {0x20, 0x12, 0x34}, topic.getBytes(StandardCharsets.UTF_8)))).getTopicName());
    assertEquals(topic, assertInstanceOf(Unsubscribe.class,
        parse(wire(length, width, MQTT_SNPacket.UNSUBSCRIBE, new byte[] {0, 0x12, 0x34}, topic.getBytes(StandardCharsets.UTF_8)))).getTopicName());
  }

  @Test
  void extended_header_connect_preserves_valid_client_id_and_duration() throws Exception {
    String id = "client";
    Connect connect = assertInstanceOf(Connect.class,
        parse(wire(8 + id.length(), 3, MQTT_SNPacket.CONNECT, new byte[] {4, 1, 0, 30}, id.getBytes(StandardCharsets.UTF_8))));
    assertEquals(id, connect.getClientId());
    assertEquals(30, connect.getDuration());
    assertEquals(1, connect.getProtocolId());
  }

  @Test
  void extended_header_distinguishes_optional_fields_from_header_bytes() throws Exception {
    Disconnect disconnect = assertInstanceOf(Disconnect.class,
        parse(wire(4, 3, MQTT_SNPacket.DISCONNECT, new byte[0], new byte[0])));
    assertEquals(0, disconnect.getDuration());
    assertNull(assertInstanceOf(PingRequest.class,
        parse(wire(4, 3, MQTT_SNPacket.PINGREQ, new byte[0], new byte[0]))).getClientId());
    assertEquals("client", assertInstanceOf(PingRequest.class,
        parse(wire(10, 3, MQTT_SNPacket.PINGREQ, new byte[0], "client".getBytes(StandardCharsets.UTF_8)))).getClientId());
    assertEquals(42, assertInstanceOf(Disconnect.class,
        parse(wire(6, 3, MQTT_SNPacket.DISCONNECT, new byte[] {0, 42}, new byte[0]))).getDuration());
  }

  @Test
  void incomplete_or_impossible_headers_are_rejected_with_io_exception() {
    for (byte[] bytes : new byte[][] {new byte[0], {1}, {1, 0}, {1, 0, 3}, {0}}) {
      assertThrows(IOException.class, () -> parse(bytes));
    }
  }

  @Test
  void declared_extended_length_must_match_the_datagram() {
    byte[] valid = wire(260, 3, MQTT_SNPacket.WILLMSG, new byte[0], new byte[256]);
    assertThrows(IOException.class, () -> parse(Arrays.copyOf(valid, valid.length - 1)));
    assertThrows(IOException.class, () -> parse(Arrays.copyOf(valid, valid.length + 1)));
  }

  private MQTT_SNPacket parse(byte[] wire) throws IOException {
    return factory.parseFrame(new Packet(ByteBuffer.wrap(wire)));
  }

  private static byte[] wire(int length, int width, int type, byte[] fixed, byte[] data) {
    assertEquals(length, width + 1 + fixed.length + data.length);
    byte[] wire = new byte[length];
    int offset;
    if (width == 1) {
      wire[0] = (byte) length;
      offset = 1;
    } else {
      wire[0] = 1;
      wire[1] = (byte) (length >>> 8);
      wire[2] = (byte) length;
      offset = 3;
    }
    wire[offset++] = (byte) type;
    System.arraycopy(fixed, 0, wire, offset, fixed.length);
    System.arraycopy(data, 0, wire, offset + fixed.length, data.length);
    return wire;
  }
}
