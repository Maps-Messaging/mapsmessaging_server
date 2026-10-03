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

package io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties;

import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MessagePropertyEncodingFamiliesTest {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void booleanPropertyRoundTrips(boolean value) throws Exception {
    PayloadFormatIndicator original = new PayloadFormatIndicator(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    PayloadFormatIndicator restored = (PayloadFormatIndicator) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getPayloadFormatIndicator());
    assertEquals(1, original.getSize());
    assertTrue(original.toString().endsWith(":" + value));
  }

  @ParameterizedTest
  @ValueSource(bytes = {0, 1, 2, 127, -1})
  void bytePropertyRoundTrips(byte value) throws Exception {
    MaximumQoS original = new MaximumQoS();
    original.setMaximumQoS(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    MaximumQoS restored = (MaximumQoS) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getMaximumQoS());
    assertEquals(1, original.getSize());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 255, 256, 32767, 65535})
  void shortPropertyRoundTrips(int value) throws Exception {
    ReceiveMaximum original = new ReceiveMaximum(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    ReceiveMaximum restored = (ReceiveMaximum) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getReceiveMaximum());
    assertEquals(2, original.getSize());
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, 1L, 65535L, 65536L, 2147483647L, 4294967295L})
  void integerPropertyRoundTrips(long value) throws Exception {
    MessageExpiryInterval original = new MessageExpiryInterval(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    MessageExpiryInterval restored = (MessageExpiryInterval) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getMessageExpiryInterval());
    assertEquals(4, original.getSize());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "text/plain", "application/json", "μqtt"})
  void utf8PropertyRoundTrips(String value) throws Exception {
    ContentType original = new ContentType(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    ContentType restored = (ContentType) original.instance();
    restored.load(packet);

    assertEquals(value, restored.getContentType());
    assertEquals(2 + value.getBytes(StandardCharsets.UTF_8).length, original.getSize());
  }

  @Test
  void binaryPropertyRoundTripsEmptyPayload() throws Exception {
    assertBinaryRoundTrip(new byte[0]);
  }

  @Test
  void binaryPropertyRoundTripsTypicalPayload() throws Exception {
    assertBinaryRoundTrip("auth-data".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void binaryPropertyRoundTripsBinaryExtremes() throws Exception {
    assertBinaryRoundTrip(new byte[]{0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF});
  }

  @Test
  void concreteBooleanSetterIsReflectedOnWire() throws Exception {
    RequestProblemInformation original = new RequestProblemInformation();
    original.setRequestProblemInformation(true);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    RequestProblemInformation restored =
        (RequestProblemInformation) MessagePropertyFactory.getInstance()
            .find(MessagePropertyFactory.REQUEST_PROBLEM_INFORMATION);
    restored.load(packet);

    assertTrue(restored.getRequestProblemInformation());
  }

  @Test
  void correlationDataUsesBinaryPropertyEncoding() throws Exception {
    byte[] expected = {1, 2, 3, 4};
    CorrelationData original = new CorrelationData(expected);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    CorrelationData restored = (CorrelationData) original.instance();
    restored.load(packet);

    assertArrayEquals(expected, restored.getCorrelationData());
    assertEquals(expected.length + 2, original.getSize());
  }

  @Test
  void reasonStringUsesUtf8PropertyEncoding() throws Exception {
    ReasonString original = new ReasonString("not-authorised");
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    ReasonString restored = (ReasonString) original.instance();
    restored.load(packet);

    assertEquals("not-authorised", restored.getReasonString());
  }

  private void assertBinaryRoundTrip(byte[] value) throws Exception {
    AuthenticationData original = new AuthenticationData(value);
    Packet packet = packet();

    original.pack(packet);
    packet.flip();

    AuthenticationData restored = (AuthenticationData) original.instance();
    restored.load(packet);

    assertArrayEquals(value, restored.getAuthenticationData());
    assertEquals(value.length + 2, original.getSize());
  }

  private Packet packet() {
    return new Packet(ByteBuffer.allocate(256));
  }
}
