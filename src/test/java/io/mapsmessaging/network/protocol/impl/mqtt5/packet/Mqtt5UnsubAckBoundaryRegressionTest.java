/*
 *
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MQTTPacket;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.MalformedException;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Mqtt5UnsubAckBoundaryRegressionTest {

  // MQTT-3.11.3-1 and MQTT-3.11.3-2: the single-filter response has a payload.
  @Test
  void single_filter_ack_has_mandatory_reason_payload() {
    Packet packet = new Packet(16, false);
    new UnsubAck5(73).packFrame(packet);
    packet.flip();
    byte[] actual = new byte[packet.available()];
    packet.get(actual);
    assertArrayEquals(new byte[] {(byte) 0xb0, 4, 0, 73, 0, 0}, actual);
  }

  // MQTT 5 sections 3.11.2 and 3.11.3; MQTT-3.11.3-2.
  @Test
  void multi_byte_property_length_does_not_consume_next_frame() throws Exception {
    UnsubAck5 original = new UnsubAck5(65535,
        new StatusCode[] {StatusCode.SUCCESS, StatusCode.NOT_AUTHORISED});
    original.add(new io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.ReasonString(
        "x".repeat(128)));
    Packet encoded = pack(original);
    byte fixed = encoded.get();
    long remaining = MQTTPacket.readVariableInt(encoded);
    UnsubAck5 restored = new UnsubAck5(fixed, remaining, encoded);
    assertEquals(original.getReasonCodes(), restored.getReasonCodes());
    assertEquals(0, encoded.available());
  }

  @Test
  void outgoing_ack_rejects_invalid_identifiers_and_reason_codes() {
    for (int identifier : new int[] {0, -1, 65536}) {
      assertThrows(IllegalArgumentException.class, () -> new UnsubAck5(identifier));
    }
    for (StatusCode reason : new StatusCode[] {null, StatusCode.SUCCESS_QOS_1,
        StatusCode.MALFORMED_PACKET, StatusCode.NO_MATCHING_SUBSCRIBERS}) {
      assertThrows(IllegalArgumentException.class,
          () -> new UnsubAck5(1, new StatusCode[] {reason}));
    }
  }

  @Test
  void every_legal_unsubscribe_reason_round_trips_in_order() throws Exception {
    StatusCode[] reasons = {StatusCode.SUCCESS, StatusCode.NO_SUBSCRIPTION_EXISTED,
        StatusCode.UNSPECIFIED_ERROR, StatusCode.IMPLEMENTATION_SPECIFIC_ERROR,
        StatusCode.NOT_AUTHORISED, StatusCode.TOPIC_FILTER_INVALID,
        StatusCode.PACKET_IDENTIFIER_INUSE};
    Packet packet = pack(new UnsubAck5(65535, reasons));
    byte fixed = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);
    assertEquals(List.of(reasons), new UnsubAck5(fixed, remaining, packet).getReasonCodes());
    assertEquals(0, packet.available());
  }

  @Test
  void incoming_ack_rejects_zero_identifier_and_invalid_reason_codes() {
    for (byte reason : new byte[] {1, 2, 0x10, (byte) 0x81, (byte) 0xff}) {
      Packet packet = new Packet(ByteBuffer.wrap(new byte[] {0, 1, 0, reason}));
      assertThrows(MalformedException.class, () -> new UnsubAck5((byte) 0xb0, 4, packet));
    }
    Packet packet = new Packet(ByteBuffer.wrap(new byte[] {0, 0, 0, 0}));
    assertThrows(MalformedException.class, () -> new UnsubAck5((byte) 0xb0, 4, packet));
  }

  // MQTT 5 sections 1.5.5 and 3.11.1: VBI boundaries include the complete payload.
  @ParameterizedTest
  @ValueSource(ints = {124, 125, 126, 127, 128, 130, 16380, 16381, 16382, 20000})
  void remaining_length_boundaries_round_trip_every_reason(int reasonCount) throws Exception {
    StatusCode[] reasons = new StatusCode[reasonCount];
    java.util.Arrays.fill(reasons, StatusCode.SUCCESS);
    UnsubAck5 original = new UnsubAck5(257, reasons);
    Packet packet = new Packet(reasonCount + 16, false);
    int written = original.packFrame(packet);
    assertEquals(packet.position(), written);
    packet.flip();
    byte fixed = packet.get();
    long remaining = MQTTPacket.readVariableInt(packet);
    assertEquals(reasonCount + 3, remaining);
    assertEquals(remaining, packet.available());
    UnsubAck5 restored = new UnsubAck5(fixed, remaining, packet);
    assertEquals(257, restored.getPacketId());
    assertEquals(original.getReasonCodes(), restored.getReasonCodes());
    assertEquals(0, packet.available());
  }

  private static Packet pack(UnsubAck5 ack) {
    Packet packet = new Packet(512, false);
    int written = ack.packFrame(packet);
    assertEquals(packet.position(), written);
    packet.flip();
    return packet;
  }
}
