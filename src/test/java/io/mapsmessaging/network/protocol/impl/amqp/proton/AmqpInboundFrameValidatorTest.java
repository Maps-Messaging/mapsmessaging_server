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

package io.mapsmessaging.network.protocol.impl.amqp.proton;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AmqpInboundFrameValidatorTest {

  private static final byte[] AMQP_HEADER = {'A', 'M', 'Q', 'P', 0, 1, 0, 0};
  private static final byte[] SASL_HEADER = {'A', 'M', 'Q', 'P', 3, 1, 0, 0};
  private static final byte[] OPEN = {0, 0x53, 0x10, 0x45};
  private static final byte[] BEGIN = {0, 0x53, 0x11, 0x45};

  @Test
  void acceptsSupportedHeaderAndFirstOpenThenBeginAndHeartbeat() {
    AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
    ByteBuffer bytes = ByteBuffer.wrap(concat(AMQP_HEADER, frame(0, 2, 0, OPEN),
        frame(0, 2, 0, BEGIN), frame(0, 2, 0, new byte[0])));

    assertDoesNotThrow(() -> validator.validate(bytes, true));
    assertEquals(0, bytes.position(), "Validation must not consume Proton input");
  }

  @Test
  void acceptsEverySplitBoundaryAcrossHeaderAndFrames() {
    byte[] wire = concat(AMQP_HEADER, frame(0, 2, 0, OPEN), frame(0, 2, 0, BEGIN));
    for (int split = 1; split < wire.length; split++) {
      AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
      int boundary = split;
      assertDoesNotThrow(() -> {
        validator.validate(ByteBuffer.wrap(wire, 0, boundary), true);
        validator.validate(ByteBuffer.wrap(wire, boundary, wire.length - boundary), true);
      }, "Split boundary " + split);
    }
  }

  @ParameterizedTest
  @MethodSource("invalidFrameHeaders")
  void rejectsMalformedSizeDoffFrameTypeAndPreOpenState(byte[] malformed) throws Exception {
    AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
    validator.validate(ByteBuffer.wrap(AMQP_HEADER), true);

    assertThrows(IOException.class, () -> validator.validate(ByteBuffer.wrap(malformed), true));
  }

  @Test
  void supportedVersionIsReturnedWhenProtocolVersionIsUnsupported() {
    AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
    byte[] unsupported = {'A', 'M', 'Q', 'P', 0, 1, 1, 0};

    AmqpInboundFrameValidator.UnsupportedVersion mismatch = assertThrows(
        AmqpInboundFrameValidator.UnsupportedVersion.class,
        () -> validator.validate(ByteBuffer.wrap(unsupported), false));
    assertArrayEquals(AMQP_HEADER, mismatch.supportedHeader());
  }

  @Test
  void unsupportedSaslVersionReturnsSupportedSaslHeader() {
    AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
    byte[] unsupported = {'A', 'M', 'Q', 'P', 3, 1, 1, 0};

    AmqpInboundFrameValidator.UnsupportedVersion mismatch = assertThrows(
        AmqpInboundFrameValidator.UnsupportedVersion.class,
        () -> validator.validate(ByteBuffer.wrap(unsupported), false));
    assertArrayEquals(SASL_HEADER, mismatch.supportedHeader());
  }

  @Test
  void acceptsSaslToNormalAmqpHeaderAfterSaslCompletes() {
    AmqpInboundFrameValidator validator = new AmqpInboundFrameValidator();
    assertDoesNotThrow(() -> {
      validator.validate(ByteBuffer.wrap(concat(SASL_HEADER,
          frame(1, 2, 0, new byte[] {0, 0x53, 0x40, 0x45}))), false);
      validator.validate(ByteBuffer.wrap(concat(AMQP_HEADER, frame(0, 2, 0, OPEN))), true);
    });
  }

  private static Stream<Arguments> invalidFrameHeaders() {
    return Stream.of(
        Arguments.of((Object) frameWithHeader(7, 2, 0, 0, new byte[0])),
        Arguments.of((Object) frameWithHeader(8, 1, 0, 0, new byte[0])),
        Arguments.of((Object) frameWithHeader(8, 3, 0, 0, new byte[0])),
        Arguments.of((Object) frameWithHeader(8, 2, 0x7f, 0, new byte[0])),
        Arguments.of((Object) frameWithHeader(513, 2, 0, 0, new byte[505])),
        Arguments.of((Object) frame(0, 2, 0, BEGIN)),
        Arguments.of((Object) frame(0, 2, 1, OPEN)),
        Arguments.of((Object) frame(0, 2, 0, new byte[0])));
  }

  private static byte[] frame(int type, int doff, int channel, byte[] body) {
    return frameWithHeader(8 + body.length, doff, type, channel, body);
  }

  private static byte[] frameWithHeader(int size, int doff, int type, int channel, byte[] body) {
    ByteBuffer wire = ByteBuffer.allocate(8 + body.length);
    wire.putInt(size);
    wire.put((byte) doff);
    wire.put((byte) type);
    wire.putShort((short) channel);
    wire.put(body);
    return wire.array();
  }

  private static byte[] concat(byte[]... parts) {
    ByteArrayOutputStream wire = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      wire.writeBytes(part);
    }
    return wire.toByteArray();
  }
}
