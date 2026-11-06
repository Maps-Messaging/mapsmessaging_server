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

package io.mapsmessaging.network.protocol.impl.mqtt.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MqttParserHardeningTest {

  @ParameterizedTest
  @MethodSource("canonicalVariableIntegers")
  void decodesCanonicalVariableByteIntegers(byte[] encoded, long expected) throws Exception {
    Packet packet = wrapped(encoded);

    assertEquals(expected, MQTTPacket.readVariableInt(packet));
    assertEquals(0, packet.available());
  }

  @ParameterizedTest
  @MethodSource("invalidVariableIntegers")
  void rejectsNonMinimalAndOverlongVariableByteIntegers(byte[] encoded) {
    assertThrows(MalformedException.class, () -> MQTTPacket.readVariableInt(wrapped(encoded)));
  }

  @Test
  void incompleteVariableByteIntegerStillRequestsMoreData() {
    assertThrows(EndOfBufferException.class,
        () -> MQTTPacket.readVariableInt(wrapped((byte) 0x80, (byte) 0x80)));
  }

  @ParameterizedTest
  @MethodSource("malformedUtf8")
  void rejectsMalformedLengthPrefixedUtf8(byte[] encoded) {
    ByteBuffer bytes = ByteBuffer.allocate(encoded.length + 2);
    bytes.putShort((short) encoded.length);
    bytes.put(encoded);
    bytes.flip();

    assertThrows(MalformedException.class,
        () -> MQTTPacket.readUTF8(new Packet(bytes)));
  }

  @ParameterizedTest
  @MethodSource("malformedUtf8")
  void rejectsMalformedRemainingUtf8(byte[] encoded) {
    assertThrows(MalformedException.class,
        () -> MQTTPacket.readRemainingString(wrapped(encoded)));
  }

  @Test
  void missingUtf8LengthIsMalformed() {
    assertThrows(MalformedException.class, () -> MQTTPacket.readUTF8(wrapped((byte) 0x00)));
  }

  @Test
  void truncatedUtf8ContentIsMalformed() {
    assertThrows(MalformedException.class,
        () -> MQTTPacket.readUTF8(wrapped((byte) 0x00, (byte) 0x03, (byte) 'a')));
  }

  @Test
  void acceptsValidUnicodeIncludingMultibyteCharacters() throws Exception {
    String text = "café \uD83D\uDE80";
    byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
    ByteBuffer bytes = ByteBuffer.allocate(encoded.length + 2);
    bytes.putShort((short) encoded.length);
    bytes.put(encoded);
    bytes.flip();

    assertEquals(text, MQTTPacket.readUTF8(new Packet(bytes)));
    assertEquals(text, MQTTPacket.readRemainingString(wrapped(encoded)));
  }

  private static Stream<Arguments> canonicalVariableIntegers() {
    return Stream.of(
        Arguments.of(new byte[] {0}, 0L),
        Arguments.of(new byte[] {0x7f}, 127L),
        Arguments.of(new byte[] {(byte) 0x80, 0x01}, 128L),
        Arguments.of(new byte[] {(byte) 0xff, 0x7f}, 16_383L),
        Arguments.of(new byte[] {(byte) 0x80, (byte) 0x80, 0x01}, 16_384L),
        Arguments.of(new byte[] {(byte) 0xff, (byte) 0xff, 0x7f}, 2_097_151L),
        Arguments.of(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01}, 2_097_152L),
        Arguments.of(new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, 0x7f}, 268_435_455L));
  }

  private static Stream<Arguments> invalidVariableIntegers() {
    return Stream.of(
        Arguments.of((Object) new byte[] {(byte) 0x80, 0x00}),
        Arguments.of((Object) new byte[] {(byte) 0xff, 0x00}),
        Arguments.of((Object) new byte[] {(byte) 0x80, (byte) 0x81, 0x00}),
        Arguments.of((Object) new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00}),
        Arguments.of((Object) new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00}),
        Arguments.of((Object) new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x01}));
  }

  private static Stream<Arguments> malformedUtf8() {
    return Stream.of(
        Arguments.of((Object) new byte[] {(byte) 0xc3, 0x28}),
        Arguments.of((Object) new byte[] {(byte) 0xc0, (byte) 0xaf}),
        Arguments.of((Object) new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0x80}),
        Arguments.of((Object) new byte[] {(byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80}),
        Arguments.of((Object) new byte[] {0x00}));
  }

  private static Packet wrapped(byte... bytes) {
    return new Packet(ByteBuffer.wrap(bytes));
  }
}
