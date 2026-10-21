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

package io.mapsmessaging.network.protocol.impl.satellite.modem.device.values;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MessageFormatTest {

  @Test
  void dataFormatIsExplicitlyUnsupported() {
    assertThrows(UnsupportedOperationException.class, () -> MessageFormat.DATA.encode(new byte[]{1}));
    assertThrows(UnsupportedOperationException.class, () -> MessageFormat.DATA.decode("01"));
  }

  @Test
  void textFormatEscapesNonPrintableAndBackslashBytes() {
    byte[] input = new byte[]{'A', 'z', ' ', '\\', 0x00, 0x1f, 0x7f};

    String encoded = MessageFormat.TEXT.encode(input);

    assertEquals("\"Az \\5C\\00\\1F\\7F\"", encoded);
    assertArrayEquals(input, MessageFormat.TEXT.decode(encoded));
  }

  @Test
  void textDecodeAcceptsQuotedAndUnquotedPayloads() {
    assertArrayEquals("ABC".getBytes(StandardCharsets.US_ASCII), MessageFormat.TEXT.decode("\"ABC\""));
    assertArrayEquals(new byte[]{'A', 0x0a, 'B'}, MessageFormat.TEXT.decode("A\\0AB"));
  }

  @Test
  void hexFormatRoundTripsBinaryPayload() {
    byte[] input = new byte[]{0x00, 0x01, 0x0f, 0x10, 0x7f, (byte) 0xff};

    String encoded = MessageFormat.HEX.encode(input);

    assertEquals("00010F107FFF", encoded);
    assertArrayEquals(input, MessageFormat.HEX.decode(encoded));
    assertArrayEquals(input, MessageFormat.HEX.decode(encoded.toLowerCase()));
  }

  @Test
  void base64FormatRoundTripsBinaryPayload() {
    byte[] input = new byte[]{0x00, 0x01, 0x02, (byte) 0xfe, (byte) 0xff};

    String encoded = MessageFormat.BASE64.encode(input);

    assertArrayEquals(input, MessageFormat.BASE64.decode(encoded));
  }

  @Test
  void modemCodesMapToFormatsAndRejectUnknownCodes() {
    assertEquals(MessageFormat.DATA, MessageFormat.fromCode(0));
    assertEquals(MessageFormat.TEXT, MessageFormat.fromCode(1));
    assertEquals(MessageFormat.HEX, MessageFormat.fromCode(2));
    assertEquals(MessageFormat.BASE64, MessageFormat.fromCode(3));

    assertEquals(0, MessageFormat.DATA.getCode());
    assertEquals(1, MessageFormat.TEXT.getCode());
    assertEquals(2, MessageFormat.HEX.getCode());
    assertEquals(3, MessageFormat.BASE64.getCode());

    assertThrows(IllegalArgumentException.class, () -> MessageFormat.fromCode(-1));
    assertThrows(IllegalArgumentException.class, () -> MessageFormat.fromCode(4));
  }
}
